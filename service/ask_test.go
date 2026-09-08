package main

import (
	"bytes"
	"context"
	"encoding/json"
	"io"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"net/textproto"
	"os"
	"path/filepath"
	"strings"
	"sync/atomic"
	"testing"
	"time"
)

func askService(t *testing.T, handler http.HandlerFunc) (*httptest.Server, *application) {
	t.Helper()
	proxy := httptest.NewServer(handler)
	t.Cleanup(proxy.Close)
	t.Setenv("FAMILYHOME_DEVICE_TOKEN", testDeviceToken)
	t.Setenv("LLM_PROXY_SECRET", "test-secret")
	path := filepath.Join(t.TempDir(), "config.yml")
	writeTestFile(t, path, startupYAML("127.0.0.1:0", t.TempDir(), proxy.URL, "test-model"))
	configuration, err := loadConfig(path)
	if err != nil {
		t.Fatal(err)
	}
	app, err := newApplication(configuration)
	if err != nil {
		t.Fatal(err)
	}
	server := httptest.NewServer(app.routes())
	t.Cleanup(server.Close)
	return server, app
}

func askRequest(t *testing.T, server *httptest.Server, body, contentType string) (int, string) {
	t.Helper()
	request, _ := http.NewRequest(http.MethodPost, server.URL+"/v1/ask", strings.NewReader(body))
	request.Header.Set("Content-Type", contentType)
	authorize(request)
	response, err := server.Client().Do(request)
	if err != nil {
		t.Fatal(err)
	}
	defer response.Body.Close()
	data, _ := io.ReadAll(response.Body)
	return response.StatusCode, string(data)
}

func TestAskHTTPValidationAndSettings(t *testing.T) {
	var calls atomic.Int32
	server, _ := askService(t, func(w http.ResponseWriter, r *http.Request) { calls.Add(1); io.WriteString(w, "Answer") })
	for _, input := range []string{
		`{"profile_id":"alice","name":"Alice","question":"Why?","model":"injected"}`,
		`{"profile_id":"alice","name":"Alice","question":"Why?"} {}`,
		`{"profile_id":"alice","name":"Alice","question":"Why?","question":"Again?"}`,
		`{"name":"Alice","question":"Why?"}`,
		`{"profile_id":"alice","name":"Alice","question":""}`,
	} {
		status, body := askRequest(t, server, input, "application/json")
		if status != 400 || !strings.Contains(body, `"code":"invalid_question"`) {
			t.Errorf("invalid request status=%d body=%s", status, body)
		}
	}
	status, _ := askRequest(t, server, `{"profile_id":"alice","name":"Alice","question":"Why?"}`, "text/plain")
	if status != 415 {
		t.Errorf("unsupported media status=%d", status)
	}
	if calls.Load() != 0 {
		t.Errorf("invalid requests reached provider: %d", calls.Load())
	}
	request, _ := http.NewRequest(http.MethodGet, server.URL+"/v1/ask/settings", nil)
	authorize(request)
	response, err := server.Client().Do(request)
	if err != nil {
		t.Fatal(err)
	}
	defer response.Body.Close()
	var settings map[string]any
	_ = json.NewDecoder(response.Body).Decode(&settings)
	if response.StatusCode != 200 || settings["request_timeout_seconds"] != float64(17) || settings["recording_duration_seconds"] != float64(60) {
		t.Errorf("settings status=%d body=%v", response.StatusCode, settings)
	}
	for _, key := range []string{"model", "provider", "secret"} {
		if _, exists := settings[key]; exists {
			t.Error("settings disclosed routing policy")
		}
	}
}

func TestAskHTTPPromptBoundaryAndEmptyAnswer(t *testing.T) {
	var system string
	server, _ := askService(t, func(w http.ResponseWriter, r *http.Request) {
		var payload struct {
			Messages []struct{ Role, Content string }
		}
		_ = json.NewDecoder(r.Body).Decode(&payload)
		system = payload.Messages[0].Content
		io.WriteString(w, "   ")
	})
	status, body := askRequest(t, server, `{"profile_id":"alice","name":"UNTRUSTED-NAME","question":"Why?"}`, "application/json")
	if status != 502 || !strings.Contains(body, `"code":"empty_answer"`) {
		t.Errorf("empty answer status=%d body=%s", status, body)
	}
	if strings.Contains(system, "UNTRUSTED-NAME") {
		t.Error("child name was included in system instructions")
	}
}

func TestAskHTTPCancellationAndConcurrency(t *testing.T) {
	started := make(chan struct{})
	cancelled := make(chan struct{})
	server, _ := askService(t, func(w http.ResponseWriter, r *http.Request) {
		_, _ = io.Copy(io.Discard, r.Body)
		close(started)
		<-r.Context().Done()
		close(cancelled)
	})
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	request, _ := http.NewRequestWithContext(ctx, http.MethodPost, server.URL+"/v1/ask", strings.NewReader(`{"profile_id":"alice","name":"Alice","question":"Why?"}`))
	request.Header.Set("Content-Type", "application/json")
	authorize(request)
	done := make(chan struct{})
	go func() {
		defer close(done)
		response, _ := server.Client().Do(request)
		if response != nil {
			response.Body.Close()
		}
	}()
	select {
	case <-started:
	case <-time.After(2 * time.Second):
		t.Fatal("request did not reach proxy")
	}
	status, body := askRequest(t, server, `{"profile_id":"alice","name":"Alice","question":"Again?"}`, "application/json")
	if status != 429 || !strings.Contains(body, `"code":"ask_busy"`) {
		t.Errorf("concurrency status=%d body=%s", status, body)
	}
	cancel()
	select {
	case <-cancelled:
	case <-time.After(2 * time.Second):
		t.Fatal("cancellation did not reach proxy")
	}
	<-done
}

func TestAskHTTPProviderFailures(t *testing.T) {
	for _, scenario := range []struct {
		name     string
		status   int
		payload  string
		expected int
		code     string
	}{
		{"busy", 429, `{"error":{"code":"provider_rate_limited","message":"PRIVATE"}}`, 503, "provider_busy"},
		{"timeout", 504, `{"error":{"code":"request_timeout","message":"PRIVATE"}}`, 504, "outcome_unknown"},
		{"failure", 500, `PRIVATE provider payload`, 502, "provider_error"},
	} {
		t.Run(scenario.name, func(t *testing.T) {
			var calls atomic.Int32
			server, _ := askService(t, func(w http.ResponseWriter, r *http.Request) {
				calls.Add(1)
				w.WriteHeader(scenario.status)
				io.WriteString(w, scenario.payload)
			})
			status, body := askRequest(t, server, `{"profile_id":"alice","name":"Alice","question":"Why?"}`, "application/json")
			if status != scenario.expected || !strings.Contains(body, scenario.code) || strings.Contains(body, "PRIVATE") || calls.Load() != 1 {
				t.Fatalf("status=%d body=%s calls=%d", status, body, calls.Load())
			}
		})
	}
}

func TestAskHTTPWorkDeadline(t *testing.T) {
	cancelled := make(chan struct{})
	server, app := askService(t, func(w http.ResponseWriter, r *http.Request) {
		io.Copy(io.Discard, r.Body)
		<-r.Context().Done()
		close(cancelled)
	})
	app.config.LLMProxy.RequestTimeoutSeconds = 1
	app.config.Ask.TransferAllowanceSeconds = 1
	status, body := askRequest(t, server, `{"profile_id":"alice","name":"Alice","question":"Why?"}`, "application/json")
	if status != 504 || !strings.Contains(body, "outcome_unknown") {
		t.Fatalf("status=%d body=%s", status, body)
	}
	select {
	case <-cancelled:
	case <-time.After(time.Second):
		t.Fatal("provider context did not stop at the deadline")
	}
}

func TestAskHTTPAudioCapabilityAndValidation(t *testing.T) {
	for _, scenario := range []struct {
		name, mime       string
		supported, valid bool
		status           int
	}{
		{"unsupported model", "audio/m4a", false, true, 422},
		{"wrong container", "audio/m4a", true, false, 415},
		{"wrong MIME", "image/png", true, true, 415},
		{"supported", "audio/m4a", true, true, 200},
	} {
		t.Run(scenario.name, func(t *testing.T) {
			var completions atomic.Int32
			catalog, err := os.ReadFile("testdata/audio-capabilities.json")
			if err != nil {
				t.Fatal(err)
			}
			if !scenario.supported {
				catalog = []byte(`{"offerings":[{"identifier":"openai:test-model","provider":"openai","model":"test-model","capabilities":["text"],"wire_contract":"openai_responses","execution_lifecycle":"pollable_resource","media_limits":[]}]}`)
			}
			server, _ := askService(t, func(w http.ResponseWriter, r *http.Request) {
				if r.Method == http.MethodGet {
					w.Write(catalog)
					return
				}
				completions.Add(1)
				io.WriteString(w, "Recorded question answered.")
			})
			data, err := os.ReadFile("testdata/question.m4a")
			if err != nil {
				t.Fatal(err)
			}
			if !scenario.valid {
				data = []byte("not audio")
			}
			var body bytes.Buffer
			form := multipart.NewWriter(&body)
			form.WriteField("profile_id", "alice")
			form.WriteField("name", "Alice")
			header := make(textproto.MIMEHeader)
			header.Set("Content-Disposition", `form-data; name="audio"; filename="question.m4a"`)
			header.Set("Content-Type", scenario.mime)
			part, err := form.CreatePart(header)
			if err != nil {
				t.Fatal(err)
			}
			part.Write(data)
			form.Close()
			request, _ := http.NewRequest(http.MethodPost, server.URL+"/v1/ask/audio", &body)
			request.Header.Set("Content-Type", form.FormDataContentType())
			authorize(request)
			response, err := server.Client().Do(request)
			if err != nil {
				t.Fatal(err)
			}
			defer response.Body.Close()
			if response.StatusCode != scenario.status {
				t.Fatalf("status=%d want=%d", response.StatusCode, scenario.status)
			}
			if scenario.status != 200 && completions.Load() != 0 {
				t.Fatal("invalid or unsupported audio reached provider completion")
			}
		})
	}
}
