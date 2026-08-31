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
	app := &application{config: configuration, client: client, http: fakeProxy.Client()}
	request := httptest.NewRequest(http.MethodPost, "/v1/ask", strings.NewReader(`{"profile_id":"alice","name":"Alice","question":"Why is the sky blue?"}`))
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
