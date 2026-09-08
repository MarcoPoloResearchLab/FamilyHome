package main

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"io"
	"log"
	"math"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"sync/atomic"
	"time"

	"github.com/tyemirov/llm-proxy/pkg/llmproxyclient"
)

const (
	maxRequestBytes          = 12 << 20
	openMeteoGeocodingURL    = "https://geocoding-api.open-meteo.com/v1/search"
	openMeteoForecastURL     = "https://api.open-meteo.com/v1/forecast"
	weatherResponseByteLimit = 1 << 20
)

type application struct {
	askRequests         atomic.Int32
	config              config
	client              llmproxyclient.Client
	http                *http.Client
	weatherGeocodingURL string
	weatherForecastURL  string
}

type askInput struct {
	ProfileID string `json:"profile_id"`
	Name      string `json:"name"`
	Question  string `json:"question"`
}

type calendarEvent struct {
	Title string `json:"title"`
	Start string `json:"start"`
}

type weatherResponse struct {
	Location                 string `json:"location"`
	Condition                string `json:"condition"`
	Icon                     string `json:"icon"`
	TemperatureF             int    `json:"temperature_f"`
	FeelsLikeF               int    `json:"feels_like_f"`
	HighF                    int    `json:"high_f"`
	LowF                     int    `json:"low_f"`
	PrecipitationProbability int    `json:"precipitation_probability"`
}

func main() {
	configPath := flag.String("config", "", "required backend YAML path")
	flag.Parse()
	if *configPath == "" || flag.NArg() != 0 {
		log.Fatal("--config <path> is required")
	}
	configuration, configError := loadConfig(*configPath)
	if configError != nil {
		log.Fatal(configError)
	}
	app, appError := newApplication(configuration)
	if appError != nil {
		log.Fatal(appError)
	}
	if directoryError := os.MkdirAll(filepath.Join(configuration.Server.DataDir, "drawings"), 0o750); directoryError != nil {
		log.Fatalf("create data directory: %v", directoryError)
	}
	server := &http.Server{
		Addr:              configuration.Server.ListenAddress,
		Handler:           app.routes(),
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       time.Duration(configuration.Ask.TransferAllowanceSeconds) * time.Second,
		WriteTimeout:      configuration.responseDuration(),
		IdleTimeout:       90 * time.Second,
	}
	log.Printf("Children's Portal service listening on %s", configuration.Server.ListenAddress)
	log.Fatal(server.ListenAndServe())
}

func (app *application) routes() http.Handler {
	api := http.NewServeMux()
	api.HandleFunc("GET /v1/ask/settings", app.askSettings)
	api.HandleFunc("POST /v1/ask", app.ask)
	api.HandleFunc("POST /v1/ask/transcriptions", app.askTranscription)
	api.HandleFunc("GET /v1/calendar/next", app.nextCalendarEvent)
	api.HandleFunc("GET /v1/weather", app.weather)
	api.HandleFunc("POST /v1/drawings", app.saveDrawing)

	root := http.NewServeMux()
	root.HandleFunc("GET /healthz", app.health)
	root.HandleFunc("GET /drawings/{name}", app.getDrawing)
	root.Handle("/v1/", app.authenticate(api))
	return securityHeaders(root)
}

func (app *application) authenticate(next http.Handler) http.Handler {
	expected := sha256.Sum256([]byte(app.config.Server.DeviceToken))
	return http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		header := request.Header.Get("Authorization")
		if !strings.HasPrefix(header, "Bearer ") {
			app.unauthorized(writer)
			return
		}
		provided := strings.TrimPrefix(header, "Bearer ")
		actual := sha256.Sum256([]byte(provided))
		if subtle.ConstantTimeCompare(actual[:], expected[:]) != 1 {
			app.unauthorized(writer)
			return
		}
		next.ServeHTTP(writer, request)
	})
}

func (app *application) unauthorized(writer http.ResponseWriter) {
	writer.Header().Set("WWW-Authenticate", `Bearer realm="FamilyHome"`)
	writeError(writer, http.StatusUnauthorized, "This Portal is not authorized.")
}

func securityHeaders(next http.Handler) http.Handler {
	return http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.Header().Set("X-Content-Type-Options", "nosniff")
		writer.Header().Set("Cache-Control", "no-store")
		next.ServeHTTP(writer, request)
	})
}

func (app *application) health(writer http.ResponseWriter, _ *http.Request) {
	if err := app.checkDrawingStorage(); err != nil {
		log.Printf("health check failed: %v", err)
		writeJSON(writer, http.StatusServiceUnavailable, map[string]any{"ok": false})
		return
	}
	writeJSON(writer, http.StatusOK, map[string]any{"ok": true})
}

func (app *application) checkDrawingStorage() error {
	directory, err := os.Open(filepath.Join(app.config.Server.DataDir, "drawings"))
	if err != nil {
		return fmt.Errorf("open drawing storage: %w", err)
	}
	_, readError := directory.ReadDir(1)
	closeError := directory.Close()
	if readError != nil && !errors.Is(readError, io.EOF) {
		return fmt.Errorf("read drawing storage: %w", readError)
	}
	if closeError != nil {
		return fmt.Errorf("close drawing storage: %w", closeError)
	}
	return nil
}

func (app *application) nextCalendarEvent(writer http.ResponseWriter, request *http.Request) {
	calendarURL := strings.TrimSpace(request.URL.Query().Get("url"))
	parsedURL, parseError := url.Parse(calendarURL)
	if parseError != nil || (parsedURL.Scheme != "http" && parsedURL.Scheme != "https") || parsedURL.Host == "" {
		writeError(writer, http.StatusBadRequest, "A valid calendar link is required.")
		return
	}
	calendarRequest, _ := http.NewRequestWithContext(request.Context(), http.MethodGet, parsedURL.String(), nil)
	response, fetchError := app.http.Do(calendarRequest)
	if fetchError != nil {
		writeError(writer, http.StatusBadGateway, "The calendar could not be reached.")
		return
	}
	defer response.Body.Close()
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		writeError(writer, http.StatusBadGateway, "The calendar did not return an event list.")
		return
	}
	body, readError := io.ReadAll(io.LimitReader(response.Body, 2<<20))
	if readError != nil {
		writeError(writer, http.StatusBadGateway, "The calendar could not be read.")
		return
	}
	event, found := parseNextEvent(string(body), time.Now())
	if !found {
		writeJSON(writer, http.StatusOK, map[string]any{"event": nil})
		return
	}
	writeJSON(writer, http.StatusOK, map[string]any{"event": event})
}

func (app *application) weather(writer http.ResponseWriter, request *http.Request) {
	locationQuery := strings.TrimSpace(request.URL.Query().Get("location"))
	if locationQuery == "" || len(locationQuery) > 120 {
		writeError(writer, http.StatusBadRequest, "A ZIP code or city is required.")
		return
	}
	geocodingBaseURL := app.weatherGeocodingURL
	forecastBaseURL := app.weatherForecastURL
	if geocodingBaseURL == "" {
		geocodingBaseURL = openMeteoGeocodingURL
	}
	if forecastBaseURL == "" {
		forecastBaseURL = openMeteoForecastURL
	}

	geocodingValues := url.Values{
		"name":     {locationQuery},
		"count":    {"1"},
		"language": {"en"},
		"format":   {"json"},
	}
	var geocoding struct {
		Results []struct {
			Name      string  `json:"name"`
			Admin1    string  `json:"admin1"`
			Country   string  `json:"country"`
			Latitude  float64 `json:"latitude"`
			Longitude float64 `json:"longitude"`
		} `json:"results"`
	}
	if fetchError := app.fetchWeatherJSON(request.Context(), geocodingBaseURL+"?"+geocodingValues.Encode(), &geocoding); fetchError != nil {
		log.Printf("weather geocoding failed location=%q: %v", locationQuery, fetchError)
		writeError(writer, http.StatusBadGateway, "Weather is temporarily unavailable.")
		return
	}
	if len(geocoding.Results) == 0 {
		writeError(writer, http.StatusNotFound, "That weather location could not be found.")
		return
	}
	place := geocoding.Results[0]

	forecastValues := url.Values{
		"latitude":         {strconv.FormatFloat(place.Latitude, 'f', -1, 64)},
		"longitude":        {strconv.FormatFloat(place.Longitude, 'f', -1, 64)},
		"current":          {"temperature_2m,apparent_temperature,weather_code"},
		"daily":            {"temperature_2m_max,temperature_2m_min,precipitation_probability_max"},
		"temperature_unit": {"fahrenheit"},
		"timezone":         {"auto"},
		"forecast_days":    {"1"},
	}
	var forecast struct {
		Current struct {
			Temperature *float64 `json:"temperature_2m"`
			FeelsLike   *float64 `json:"apparent_temperature"`
			WeatherCode *int     `json:"weather_code"`
		} `json:"current"`
		Daily struct {
			High                     []float64 `json:"temperature_2m_max"`
			Low                      []float64 `json:"temperature_2m_min"`
			PrecipitationProbability []int     `json:"precipitation_probability_max"`
		} `json:"daily"`
	}
	if fetchError := app.fetchWeatherJSON(request.Context(), forecastBaseURL+"?"+forecastValues.Encode(), &forecast); fetchError != nil {
		log.Printf("weather forecast failed location=%q: %v", locationQuery, fetchError)
		writeError(writer, http.StatusBadGateway, "Weather is temporarily unavailable.")
		return
	}
	if forecast.Current.Temperature == nil || forecast.Current.FeelsLike == nil || forecast.Current.WeatherCode == nil || len(forecast.Daily.High) == 0 || len(forecast.Daily.Low) == 0 {
		writeError(writer, http.StatusBadGateway, "The weather forecast was incomplete.")
		return
	}
	condition, icon := describeWeather(*forecast.Current.WeatherCode)
	precipitation := 0
	if len(forecast.Daily.PrecipitationProbability) > 0 {
		precipitation = forecast.Daily.PrecipitationProbability[0]
	}
	writeJSON(writer, http.StatusOK, weatherResponse{
		Location:                 weatherLocationLabel(place.Name, place.Admin1, place.Country),
		Condition:                condition,
		Icon:                     icon,
		TemperatureF:             int(math.Round(*forecast.Current.Temperature)),
		FeelsLikeF:               int(math.Round(*forecast.Current.FeelsLike)),
		HighF:                    int(math.Round(forecast.Daily.High[0])),
		LowF:                     int(math.Round(forecast.Daily.Low[0])),
		PrecipitationProbability: precipitation,
	})
}

func (app *application) fetchWeatherJSON(ctx context.Context, requestURL string, destination any) error {
	weatherRequest, requestError := http.NewRequestWithContext(ctx, http.MethodGet, requestURL, nil)
	if requestError != nil {
		return requestError
	}
	weatherRequest.Header.Set("User-Agent", "FamilyHome/1.0 (+https://github.com/MarcoPoloResearchLab/FamilyHome)")
	response, fetchError := app.http.Do(weatherRequest)
	if fetchError != nil {
		return fetchError
	}
	defer response.Body.Close()
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return fmt.Errorf("provider returned status %d", response.StatusCode)
	}
	decoder := json.NewDecoder(io.LimitReader(response.Body, weatherResponseByteLimit))
	if decodeError := decoder.Decode(destination); decodeError != nil {
		return decodeError
	}
	return nil
}

func weatherLocationLabel(name, admin1, country string) string {
	parts := make([]string, 0, 2)
	if strings.TrimSpace(name) != "" {
		parts = append(parts, strings.TrimSpace(name))
	}
	region := strings.TrimSpace(admin1)
	if region == "" {
		region = strings.TrimSpace(country)
	}
	if region != "" && (len(parts) == 0 || !strings.EqualFold(parts[0], region)) {
		parts = append(parts, region)
	}
	return strings.Join(parts, ", ")
}

func describeWeather(code int) (string, string) {
	switch {
	case code == 0:
		return "Sunny", "clear"
	case code == 1 || code == 2:
		return "Partly cloudy", "partly_cloudy"
	case code == 3:
		return "Cloudy", "cloudy"
	case code == 45 || code == 48:
		return "Foggy", "fog"
	case (code >= 51 && code <= 67) || (code >= 80 && code <= 82):
		return "Rainy", "rain"
	case (code >= 71 && code <= 77) || code == 85 || code == 86:
		return "Snowy", "snow"
	case code >= 95:
		return "Stormy", "storm"
	default:
		return "Changing skies", "cloudy"
	}
}

func (app *application) saveDrawing(writer http.ResponseWriter, request *http.Request) {
	request.Body = http.MaxBytesReader(writer, request.Body, maxRequestBytes)
	if request.Header.Get("Content-Type") != "image/png" {
		writeError(writer, http.StatusUnsupportedMediaType, "The drawing must be a PNG image.")
		return
	}
	body, readError := io.ReadAll(request.Body)
	if readError != nil || len(body) < 8 || string(body[:8]) != "\x89PNG\r\n\x1a\n" {
		writeError(writer, http.StatusBadRequest, "The drawing image is invalid.")
		return
	}
	profileID := safeName(request.Header.Get("X-Portal-Profile"))
	if profileID == "" {
		profileID = "child"
	}
	title := strings.TrimSpace(request.Header.Get("X-Portal-Title"))
	stamp := time.Now().UTC().Format("20060102T150405Z")
	randomBytes := make([]byte, 16)
	if _, randomError := rand.Read(randomBytes); randomError != nil {
		writeError(writer, http.StatusInternalServerError, "The drawing could not be saved.")
		return
	}
	fileName := fmt.Sprintf("%s-%s-%x.png", profileID, stamp, randomBytes)
	filePath := filepath.Join(app.config.Server.DataDir, "drawings", fileName)
	if writeErrorValue := os.WriteFile(filePath, body, 0o640); writeErrorValue != nil {
		writeError(writer, http.StatusInternalServerError, "The drawing could not be saved.")
		return
	}
	if title == "" {
		title = "Drawing " + time.Now().Format("Jan 2, 3:04 PM")
	}
	writeJSON(writer, http.StatusCreated, map[string]string{"title": title, "url": "/drawings/" + fileName, "file_name": fileName})
}

var drawingFileName = regexp.MustCompile(`^[a-zA-Z0-9_-]+\.png$`)

func (app *application) getDrawing(writer http.ResponseWriter, request *http.Request) {
	name := request.PathValue("name")
	if !drawingFileName.MatchString(name) {
		http.NotFound(writer, request)
		return
	}
	path := filepath.Join(app.config.Server.DataDir, "drawings", name)
	if _, statError := os.Stat(path); statError != nil {
		http.NotFound(writer, request)
		return
	}
	writer.Header().Set("Content-Type", "image/png")
	http.ServeFile(writer, request, path)
}

var unsafeName = regexp.MustCompile(`[^a-zA-Z0-9_-]+`)

func safeName(value string) string {
	return strings.Trim(unsafeName.ReplaceAllString(value, "-"), "-")
}

func parseNextEvent(calendar string, now time.Time) (calendarEvent, bool) {
	calendar = strings.ReplaceAll(calendar, "\r\n ", "")
	calendar = strings.ReplaceAll(calendar, "\r\n\t", "")
	blocks := strings.Split(calendar, "BEGIN:VEVENT")
	events := make([]calendarEvent, 0)
	for _, block := range blocks[1:] {
		end := strings.Index(block, "END:VEVENT")
		if end < 0 {
			continue
		}
		block = block[:end]
		var summary, startRaw string
		for _, line := range strings.Split(strings.ReplaceAll(block, "\r\n", "\n"), "\n") {
			if strings.HasPrefix(line, "SUMMARY:") {
				summary = strings.TrimSpace(strings.TrimPrefix(line, "SUMMARY:"))
			}
			if strings.HasPrefix(line, "DTSTART") {
				if colon := strings.Index(line, ":"); colon >= 0 {
					startRaw = strings.TrimSpace(line[colon+1:])
				}
			}
		}
		start, parseOK := parseICSTime(startRaw)
		if summary != "" && parseOK && !start.Before(now.Add(-time.Minute)) {
			events = append(events, calendarEvent{Title: unescapeICS(summary), Start: start.Format(time.RFC3339)})
		}
	}
	sort.Slice(events, func(i, j int) bool { return events[i].Start < events[j].Start })
	if len(events) == 0 {
		return calendarEvent{}, false
	}
	return events[0], true
}

func parseICSTime(value string) (time.Time, bool) {
	for _, layout := range []string{"20060102T150405Z", "20060102T150405", "20060102"} {
		if parsed, parseError := time.Parse(layout, value); parseError == nil {
			return parsed, true
		}
	}
	return time.Time{}, false
}

func unescapeICS(value string) string {
	value = strings.ReplaceAll(value, `\n`, " ")
	value = strings.ReplaceAll(value, `\,`, ",")
	value = strings.ReplaceAll(value, `\;`, ";")
	return strings.ReplaceAll(value, `\\`, `\`)
}

func writeError(writer http.ResponseWriter, status int, message string) {
	writeJSONStatus(writer, status, map[string]string{"error": message})
}

func writeJSON(writer http.ResponseWriter, status int, value any) {
	writeJSONStatus(writer, status, value)
}

func writeJSONStatus(writer http.ResponseWriter, status int, value any) {
	writer.Header().Set("Content-Type", "application/json; charset=utf-8")
	writer.WriteHeader(status)
	_ = json.NewEncoder(writer).Encode(value)
}
