package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strings"
	"time"

	"github.com/tyemirov/llm-proxy/pkg/llmproxyclient"
	"gopkg.in/yaml.v3"
)

const maxRequestBytes = 12 << 20

type config struct {
	Server struct {
		ListenAddress string `yaml:"listen_address"`
		PublicBaseURL string `yaml:"public_base_url"`
		DataDir       string `yaml:"data_dir"`
	} `yaml:"server"`
	LLMProxy struct {
		BaseURL               string `yaml:"base_url"`
		Secret                string `yaml:"secret"`
		Provider              string `yaml:"provider"`
		Model                 string `yaml:"model"`
		ReasoningEffort       string `yaml:"reasoning_effort"`
		RequestTimeoutSeconds int    `yaml:"request_timeout_seconds"`
	} `yaml:"llm_proxy"`
}

type application struct {
	config config
	client llmproxyclient.Client
	http   *http.Client
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

func main() {
	configPath := "config.yml"
	if len(os.Args) > 1 {
		configPath = os.Args[1]
	}
	configuration, configError := loadConfig(configPath)
	if configError != nil {
		log.Fatal(configError)
	}
	httpClient := &http.Client{Timeout: time.Duration(configuration.LLMProxy.RequestTimeoutSeconds+10) * time.Second}
	clientConfig, configError := llmproxyclient.NewConfig(llmproxyclient.ConfigInput{
		BaseURL:  configuration.LLMProxy.BaseURL,
		Secret:   configuration.LLMProxy.Secret,
		Provider: configuration.LLMProxy.Provider,
	})
	if configError != nil {
		log.Fatalf("llm proxy config: %v", configError)
	}
	client, clientError := llmproxyclient.NewClient(clientConfig, httpClient)
	if clientError != nil {
		log.Fatalf("llm proxy client: %v", clientError)
	}
	app := &application{config: configuration, client: client, http: httpClient}
	if directoryError := os.MkdirAll(filepath.Join(configuration.Server.DataDir, "drawings"), 0o750); directoryError != nil {
		log.Fatalf("create data directory: %v", directoryError)
	}
	server := &http.Server{
		Addr:              configuration.Server.ListenAddress,
		Handler:           app.routes(),
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       60 * time.Second,
		WriteTimeout:      60 * time.Second,
		IdleTimeout:       90 * time.Second,
	}
	log.Printf("Children's Portal service listening on %s", configuration.Server.ListenAddress)
	log.Fatal(server.ListenAndServe())
}

func loadConfig(path string) (config, error) {
	raw, readError := os.ReadFile(path)
	if readError != nil {
		return config{}, fmt.Errorf("read config: %w", readError)
	}
	expanded := os.ExpandEnv(string(raw))
	var value config
	if decodeError := yaml.Unmarshal([]byte(expanded), &value); decodeError != nil {
		return config{}, fmt.Errorf("decode config: %w", decodeError)
	}
	if strings.TrimSpace(value.Server.ListenAddress) == "" || strings.TrimSpace(value.Server.PublicBaseURL) == "" || strings.TrimSpace(value.Server.DataDir) == "" {
		return config{}, errors.New("server listen_address, public_base_url, and data_dir are required")
	}
	if _, parseError := url.ParseRequestURI(value.Server.PublicBaseURL); parseError != nil {
		return config{}, fmt.Errorf("server public_base_url: %w", parseError)
	}
	if strings.TrimSpace(value.LLMProxy.Model) == "" || value.LLMProxy.RequestTimeoutSeconds <= 0 {
		return config{}, errors.New("llm_proxy model and positive request_timeout_seconds are required")
	}
	return value, nil
}

func (app *application) routes() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", app.health)
	mux.HandleFunc("POST /v1/ask", app.ask)
	mux.HandleFunc("POST /v1/ask/audio", app.askAudio)
	mux.HandleFunc("GET /v1/calendar/next", app.nextCalendarEvent)
	mux.HandleFunc("POST /v1/drawings", app.saveDrawing)
	mux.Handle("GET /drawings/", http.StripPrefix("/drawings/", http.FileServer(http.Dir(filepath.Join(app.config.Server.DataDir, "drawings")))))
	return securityHeaders(mux)
}

func securityHeaders(next http.Handler) http.Handler {
	return http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.Header().Set("X-Content-Type-Options", "nosniff")
		writer.Header().Set("Cache-Control", "no-store")
		next.ServeHTTP(writer, request)
	})
}

func (app *application) health(writer http.ResponseWriter, _ *http.Request) {
	writeJSON(writer, http.StatusOK, map[string]any{"ok": true})
}

func (app *application) ask(writer http.ResponseWriter, request *http.Request) {
	request.Body = http.MaxBytesReader(writer, request.Body, 32<<10)
	var input askInput
	if decodeError := json.NewDecoder(request.Body).Decode(&input); decodeError != nil {
		writeError(writer, http.StatusBadRequest, "Please enter a question.")
		return
	}
	app.completeAsk(writer, request, input, nil)
}

func (app *application) askAudio(writer http.ResponseWriter, request *http.Request) {
	request.Body = http.MaxBytesReader(writer, request.Body, maxRequestBytes)
	if parseError := request.ParseMultipartForm(maxRequestBytes); parseError != nil {
		writeError(writer, http.StatusBadRequest, "The recording could not be read.")
		return
	}
	file, header, fileError := request.FormFile("audio")
	if fileError != nil {
		writeError(writer, http.StatusBadRequest, "A voice recording is required.")
		return
	}
	defer file.Close()
	audioBytes, readError := io.ReadAll(io.LimitReader(file, maxRequestBytes))
	if readError != nil || len(audioBytes) == 0 {
		writeError(writer, http.StatusBadRequest, "The voice recording was empty.")
		return
	}
	mimeType := header.Header.Get("Content-Type")
	if mimeType == "" {
		mimeType = "audio/m4a"
	}
	attachment, attachmentError := llmproxyclient.NewAudioAttachment(llmproxyclient.AudioAttachmentInput{MIMEType: mimeType, Data: audioBytes})
	if attachmentError != nil {
		writeError(writer, http.StatusBadRequest, "This recording format is not supported.")
		return
	}
	input := askInput{ProfileID: request.FormValue("profile_id"), Name: request.FormValue("name"), Question: "Listen to the child's question and answer it."}
	app.completeAsk(writer, request, input, []llmproxyclient.MessageAttachment{attachment})
}

func (app *application) completeAsk(writer http.ResponseWriter, request *http.Request, input askInput, attachments []llmproxyclient.MessageAttachment) {
	question := strings.TrimSpace(input.Question)
	if question == "" || len(question) > 2000 {
		writeError(writer, http.StatusBadRequest, "Please ask a shorter question.")
		return
	}
	name := strings.TrimSpace(input.Name)
	if name == "" {
		name = "the child"
	}
	systemPrompt := "You are the Children's Portal assistant. Give a warm, accurate, age-appropriate answer in plain language. Avoid frightening or sexual content. Never ask for personal contact details, location, passwords, or secrets. Keep the answer under 140 words unless the child explicitly asks for a story. The child's first name is " + name + "."
	reasoningEffort := app.config.LLMProxy.ReasoningEffort
	timeoutSeconds := app.config.LLMProxy.RequestTimeoutSeconds
	proxyRequest, requestError := llmproxyclient.NewMessagesRequest(llmproxyclient.MessagesRequestInput{
		Messages: []llmproxyclient.MessageInput{
			{Role: "system", Content: systemPrompt},
			{Role: "user", Content: question, Attachments: attachments},
		},
		Model:                 app.config.LLMProxy.Model,
		ReasoningEffort:       &reasoningEffort,
		RequestTimeoutSeconds: &timeoutSeconds,
	})
	if requestError != nil {
		log.Printf("create Ask request: %v", requestError)
		writeError(writer, http.StatusBadRequest, "That question could not be prepared.")
		return
	}
	ctx, cancel := context.WithTimeout(request.Context(), time.Duration(timeoutSeconds+5)*time.Second)
	defer cancel()
	answer, postError := app.client.PostMessages(ctx, proxyRequest)
	if postError != nil {
		log.Printf("Ask failed profile=%q: %v", input.ProfileID, postError)
		writeError(writer, http.StatusBadGateway, "Ask is temporarily unavailable. Please try again.")
		return
	}
	writeJSON(writer, http.StatusOK, map[string]string{"answer": strings.TrimSpace(answer)})
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
	fileName := profileID + "-" + stamp + ".png"
	filePath := filepath.Join(app.config.Server.DataDir, "drawings", fileName)
	if writeErrorValue := os.WriteFile(filePath, body, 0o640); writeErrorValue != nil {
		writeError(writer, http.StatusInternalServerError, "The drawing could not be saved.")
		return
	}
	if title == "" {
		title = "Drawing " + time.Now().Format("Jan 2, 3:04 PM")
	}
	publicURL := strings.TrimRight(app.config.Server.PublicBaseURL, "/") + "/drawings/" + fileName
	writeJSON(writer, http.StatusCreated, map[string]string{"title": title, "url": publicURL, "file_name": fileName})
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
