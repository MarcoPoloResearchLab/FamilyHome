package main

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"io"
	"log"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"net/textproto"
	"net/url"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/tyemirov/llm-proxy/pkg/llmproxyclient"
)

func TestAskUsesOfficialClientBoundary(t *testing.T) {
	var captured map[string]any
	fakeProxy := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		if request.URL.Path != "/v2" || request.URL.Query().Get("key") != "test-secret" || request.URL.Query().Get("provider") != "openai" {
			t.Fatalf("unexpected official-client request: %s", request.URL.String())
		}
		_ = json.NewDecoder(request.Body).Decode(&captured)
		_, _ = io.WriteString(writer, "A short answer for Alice.")
	}))
	defer fakeProxy.Close()
	clientConfig, err := llmproxyclient.NewConfig(llmproxyclient.ConfigInput{BaseURL: fakeProxy.URL, Secret: "test-secret", Provider: "openai"})
	if err != nil {
		t.Fatal(err)
	}
	client, err := llmproxyclient.NewClient(clientConfig, fakeProxy.Client())
	if err != nil {
		t.Fatal(err)
	}
	configuration := config{}
	configuration.LLMProxy.Model = "test-model"
	configuration.LLMProxy.ReasoningEffort = "low"
	configuration.LLMProxy.RequestTimeoutSeconds = 10
	configuration.Server.DeviceToken = testDeviceToken
	app := &application{config: configuration, client: client, http: fakeProxy.Client()}
	request := httptest.NewRequest(http.MethodPost, "/v1/ask", strings.NewReader(`{"profile_id":"alice","name":"Alice","question":"Why is the sky blue?"}`))
	authorize(request)
	response := httptest.NewRecorder()
	app.routes().ServeHTTP(response, request)
	if response.Code != http.StatusOK || !strings.Contains(response.Body.String(), "A short answer") {
		t.Fatalf("status=%d body=%s", response.Code, response.Body.String())
	}
	if captured["model"] != "test-model" || captured["reasoning_effort"] != "low" {
		t.Fatalf("payload=%v", captured)
	}
	messages := captured["messages"].([]any)
	if len(messages) != 2 || messages[0].(map[string]any)["role"] != "system" {
		t.Fatalf("messages=%v", messages)
	}
}

func TestAudioAskUsesOfficialClientAttachment(t *testing.T) {
	var captured map[string]any
	fakeProxy := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		if request.URL.Path != "/v2" || request.URL.Query().Get("key") != "test-secret" || request.URL.Query().Get("provider") != "openai" {
			t.Fatalf("unexpected official-client request: %s", request.URL.String())
		}
		_ = json.NewDecoder(request.Body).Decode(&captured)
		_, _ = io.WriteString(writer, "I heard Alice's question.")
	}))
	defer fakeProxy.Close()
	clientConfig, err := llmproxyclient.NewConfig(llmproxyclient.ConfigInput{BaseURL: fakeProxy.URL, Secret: "test-secret", Provider: "openai"})
	if err != nil {
		t.Fatal(err)
	}
	client, err := llmproxyclient.NewClient(clientConfig, fakeProxy.Client())
	if err != nil {
		t.Fatal(err)
	}
	configuration := config{}
	configuration.LLMProxy.Model = "test-model"
	configuration.LLMProxy.ReasoningEffort = "low"
	configuration.LLMProxy.RequestTimeoutSeconds = 10
	configuration.Server.DeviceToken = testDeviceToken
	app := &application{config: configuration, client: client, http: fakeProxy.Client()}

	audio := []byte("small-m4a-fixture")
	var body bytes.Buffer
	form := multipart.NewWriter(&body)
	_ = form.WriteField("profile_id", "alice")
	_ = form.WriteField("name", "Alice")
	header := make(textproto.MIMEHeader)
	header.Set("Content-Disposition", `form-data; name="audio"; filename="question.m4a"`)
	header.Set("Content-Type", "audio/m4a")
	part, err := form.CreatePart(header)
	if err != nil {
		t.Fatal(err)
	}
	_, _ = part.Write(audio)
	_ = form.Close()

	request := httptest.NewRequest(http.MethodPost, "/v1/ask/audio", &body)
	request.Header.Set("Content-Type", form.FormDataContentType())
	authorize(request)
	response := httptest.NewRecorder()
	app.routes().ServeHTTP(response, request)
	if response.Code != http.StatusOK || !strings.Contains(response.Body.String(), "I heard Alice") {
		t.Fatalf("status=%d body=%s", response.Code, response.Body.String())
	}
	messages := captured["messages"].([]any)
	attachments := messages[1].(map[string]any)["attachments"].([]any)
	if len(attachments) != 1 {
		t.Fatalf("attachments=%v", attachments)
	}
	attachment := attachments[0].(map[string]any)
	if attachment["type"] != "audio" || attachment["mime_type"] != "audio/m4a" || attachment["data"] != base64.StdEncoding.EncodeToString(audio) {
		t.Fatalf("attachment=%v", attachment)
	}
}

func TestNextCalendarEvent(t *testing.T) {
	now := time.Now().UTC()
	ics := "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nDTSTART:" + now.Add(2*time.Hour).Format("20060102T150405Z") + "\r\nSUMMARY:Piano lesson\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"
	event, found := parseNextEvent(ics, now)
	if !found || event.Title != "Piano lesson" {
		t.Fatalf("event=%v found=%v", event, found)
	}
}

func TestCalendarRejectsNonHTTPURL(t *testing.T) {
	configuration := config{}
	app := &application{config: configuration, http: http.DefaultClient}
	request := httptest.NewRequest(http.MethodGet, "/v1/calendar/next?url="+url.QueryEscape("file:///etc/passwd"), nil)
	response := httptest.NewRecorder()
	app.nextCalendarEvent(response, request)
	if response.Code != http.StatusBadRequest {
		t.Fatalf("status=%d", response.Code)
	}
}

func TestWeatherResolvesLocationAndNormalizesForecast(t *testing.T) {
	provider := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		switch request.URL.Path {
		case "/geocoding":
			if request.URL.Query().Get("name") != "90210" || request.URL.Query().Get("count") != "1" {
				t.Fatalf("unexpected geocoding query: %s", request.URL.RawQuery)
			}
			_, _ = io.WriteString(writer, `{"results":[{"name":"Beverly Hills","admin1":"California","country":"United States","latitude":34.0901,"longitude":-118.4065}]}`)
		case "/forecast":
			query := request.URL.Query()
			if query.Get("temperature_unit") != "fahrenheit" || query.Get("timezone") != "auto" || query.Get("forecast_days") != "1" {
				t.Fatalf("unexpected forecast query: %s", request.URL.RawQuery)
			}
			if !strings.Contains(query.Get("current"), "weather_code") || !strings.Contains(query.Get("daily"), "temperature_2m_max") {
				t.Fatalf("missing forecast fields: %s", request.URL.RawQuery)
			}
			_, _ = io.WriteString(writer, `{"current":{"temperature_2m":72.6,"apparent_temperature":71.2,"weather_code":2},"daily":{"temperature_2m_max":[78.8],"temperature_2m_min":[59.4],"precipitation_probability_max":[12]}}`)
		default:
			http.NotFound(writer, request)
		}
	}))
	defer provider.Close()

	configuration := config{}
	configuration.Server.DeviceToken = testDeviceToken
	app := &application{
		config: configuration, http: provider.Client(),
		weatherGeocodingURL: provider.URL + "/geocoding",
		weatherForecastURL:  provider.URL + "/forecast",
	}
	request := httptest.NewRequest(http.MethodGet, "/v1/weather?location=90210", nil)
	authorize(request)
	response := httptest.NewRecorder()
	app.routes().ServeHTTP(response, request)
	if response.Code != http.StatusOK {
		t.Fatalf("status=%d body=%s", response.Code, response.Body.String())
	}
	var weather weatherResponse
	if decodeError := json.NewDecoder(response.Body).Decode(&weather); decodeError != nil {
		t.Fatal(decodeError)
	}
	if weather.Location != "Beverly Hills, California" || weather.Condition != "Partly cloudy" || weather.Icon != "partly_cloudy" {
		t.Fatalf("weather=%+v", weather)
	}
	if weather.TemperatureF != 73 || weather.FeelsLikeF != 71 || weather.HighF != 79 || weather.LowF != 59 || weather.PrecipitationProbability != 12 {
		t.Fatalf("weather=%+v", weather)
	}
}

func TestWeatherRequiresConfiguredLocation(t *testing.T) {
	configuration := config{}
	configuration.Server.DeviceToken = testDeviceToken
	app := &application{config: configuration, http: http.DefaultClient}
	request := httptest.NewRequest(http.MethodGet, "/v1/weather", nil)
	authorize(request)
	response := httptest.NewRecorder()
	app.routes().ServeHTTP(response, request)
	if response.Code != http.StatusBadRequest {
		t.Fatalf("status=%d body=%s", response.Code, response.Body.String())
	}
}

func TestWeatherConditionCategories(t *testing.T) {
	tests := map[int]string{0: "clear", 2: "partly_cloudy", 3: "cloudy", 45: "fog", 61: "rain", 75: "snow", 96: "storm"}
	for code, expected := range tests {
		_, icon := describeWeather(code)
		if icon != expected {
			t.Fatalf("code=%d icon=%q expected=%q", code, icon, expected)
		}
	}
}

const testDeviceToken = "familyhome-test-device-token-32-characters"

func authorize(request *http.Request) {
	request.Header.Set("Authorization", "Bearer "+testDeviceToken)
}

func TestAPIRequiresDeviceBearerToken(t *testing.T) {
	configuration := config{}
	configuration.Server.DeviceToken = testDeviceToken
	app := &application{config: configuration, http: http.DefaultClient}

	for _, authorization := range []string{"", "Bearer wrong-device-token"} {
		request := httptest.NewRequest(http.MethodGet, "/v1/calendar/next?url=not-a-url", nil)
		request.Header.Set("Authorization", authorization)
		response := httptest.NewRecorder()
		app.routes().ServeHTTP(response, request)
		if response.Code != http.StatusUnauthorized {
			t.Fatalf("authorization=%q status=%d body=%s", authorization, response.Code, response.Body.String())
		}
		if response.Header().Get("WWW-Authenticate") == "" {
			t.Fatal("missing bearer authentication challenge")
		}
	}

	request := httptest.NewRequest(http.MethodGet, "/v1/calendar/next?url=not-a-url", nil)
	authorize(request)
	response := httptest.NewRecorder()
	app.routes().ServeHTTP(response, request)
	if response.Code != http.StatusBadRequest {
		t.Fatalf("authorized status=%d body=%s", response.Code, response.Body.String())
	}
}

func TestHealthDoesNotRequireAuthentication(t *testing.T) {
	configuration := config{}
	configuration.Server.DeviceToken = testDeviceToken
	configuration.Server.DataDir = t.TempDir()
	drawings := filepath.Join(configuration.Server.DataDir, "drawings")
	if err := os.Mkdir(drawings, 0o750); err != nil {
		t.Fatal(err)
	}
	app := &application{config: configuration}
	server := httptest.NewServer(app.routes())
	defer server.Close()
	var events bytes.Buffer
	previousOutput := log.Writer()
	log.SetOutput(&events)
	defer log.SetOutput(previousOutput)
	probe := func(wantStatus int, wantBody string) {
		t.Helper()
		response, err := server.Client().Get(server.URL + "/healthz")
		if err != nil {
			t.Fatal(err)
		}
		defer response.Body.Close()
		body, err := io.ReadAll(response.Body)
		if err != nil {
			t.Fatal(err)
		}
		if response.StatusCode != wantStatus || strings.TrimSpace(string(body)) != wantBody || response.Header.Get("Cache-Control") != "no-store" {
			t.Fatalf("status=%d headers=%v body=%s", response.StatusCode, response.Header, body)
		}
	}
	probe(http.StatusOK, `{"ok":true}`)
	if events.Len() != 0 {
		t.Fatalf("successful probe produced events: %s", events.String())
	}
	entries, err := os.ReadDir(drawings)
	if err != nil || len(entries) != 0 {
		t.Fatalf("probe changed drawing storage: %v %v", entries, err)
	}
	if err := os.Rename(drawings, drawings+"-unavailable"); err != nil {
		t.Fatal(err)
	}
	probe(http.StatusServiceUnavailable, `{"ok":false}`)
	if !strings.Contains(events.String(), "health check failed") {
		t.Fatalf("missing failed probe diagnostics: %s", events.String())
	}
	if err := os.Rename(drawings+"-unavailable", drawings); err != nil {
		t.Fatal(err)
	}
	probe(http.StatusOK, `{"ok":true}`)
}

func TestDrawingShareUsesUnguessableUnauthenticatedLink(t *testing.T) {
	dataDirectory := t.TempDir()
	configuration := config{}
	configuration.Server.DeviceToken = testDeviceToken
	configuration.Server.DataDir = dataDirectory
	if err := os.MkdirAll(filepath.Join(dataDirectory, "drawings"), 0o750); err != nil {
		t.Fatal(err)
	}
	app := &application{config: configuration}
	png := append([]byte("\x89PNG\r\n\x1a\n"), []byte("test-image")...)
	request := httptest.NewRequest(http.MethodPost, "/v1/drawings", bytes.NewReader(png))
	request.Header.Set("Content-Type", "image/png")
	request.Header.Set("X-Portal-Profile", "alice")
	authorize(request)
	response := httptest.NewRecorder()
	app.routes().ServeHTTP(response, request)
	if response.Code != http.StatusCreated {
		t.Fatalf("status=%d body=%s", response.Code, response.Body.String())
	}
	var saved map[string]string
	if err := json.Unmarshal(response.Body.Bytes(), &saved); err != nil {
		t.Fatal(err)
	}
	if !strings.HasPrefix(saved["url"], "/drawings/alice-") || len(saved["file_name"]) < 55 {
		t.Fatalf("saved=%v", saved)
	}

	getRequest := httptest.NewRequest(http.MethodGet, saved["url"], nil)
	getResponse := httptest.NewRecorder()
	app.routes().ServeHTTP(getResponse, getRequest)
	if getResponse.Code != http.StatusOK || !bytes.Equal(getResponse.Body.Bytes(), png) {
		t.Fatalf("status=%d body=%q", getResponse.Code, getResponse.Body.Bytes())
	}

	listRequest := httptest.NewRequest(http.MethodGet, "/drawings/", nil)
	listResponse := httptest.NewRecorder()
	app.routes().ServeHTTP(listResponse, listRequest)
	if listResponse.Code != http.StatusNotFound {
		t.Fatalf("directory listing status=%d", listResponse.Code)
	}
}

func TestLoadConfigRequiresCompleteEnvironment(t *testing.T) {
	for name, value := range map[string]string{
		"FAMILYHOME_LISTEN_ADDRESS":         "127.0.0.1:8765",
		"FAMILYHOME_DATA_DIR":               t.TempDir(),
		"FAMILYHOME_DEVICE_TOKEN":           testDeviceToken,
		"LLM_PROXY_BASE_URL":                "https://llm-proxy-api.mprlab.com",
		"LLM_PROXY_SECRET":                  "proxy-secret",
		"LLM_PROXY_PROVIDER":                "openai",
		"LLM_PROXY_MODEL":                   "test-model",
		"LLM_PROXY_REASONING_EFFORT":        "low",
		"LLM_PROXY_REQUEST_TIMEOUT_SECONDS": "45",
	} {
		t.Setenv(name, value)
	}
	configuration, err := loadConfig()
	if err != nil {
		t.Fatal(err)
	}
	if configuration.Server.DeviceToken != testDeviceToken || configuration.LLMProxy.RequestTimeoutSeconds != 45 {
		t.Fatalf("configuration=%+v", configuration)
	}
	t.Setenv("FAMILYHOME_DEVICE_TOKEN", "short")
	if _, err = loadConfig(); err == nil || !strings.Contains(err.Error(), "at least 32") {
		t.Fatalf("short token error=%v", err)
	}
}
