package main

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"io"
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
	app := &application{config: configuration}
	request := httptest.NewRequest(http.MethodGet, "/healthz", nil)
	response := httptest.NewRecorder()
	app.routes().ServeHTTP(response, request)
	if response.Code != http.StatusOK || !strings.Contains(response.Body.String(), `"ok":true`) {
		t.Fatalf("status=%d body=%s", response.Code, response.Body.String())
	}
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
