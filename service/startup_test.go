package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/http/httptest"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

func startupYAML(address, directory, proxy, model string) string {
	return fmt.Sprintf(`server:
  listen_address: %q
  data_dir: %q
  device_token: "${FAMILYHOME_DEVICE_TOKEN}"
llm_proxy:
  base_url: %q
  secret: "${LLM_PROXY_SECRET}"
  provider: openai
  model: %q
  reasoning_effort: low
  request_timeout_seconds: 2
ask:
  transfer_allowance_seconds: 5
  question_character_limit: 2000
  recording_duration_seconds: 60
  audio_byte_limit: 6291456
  concurrent_request_limit: 1
`, address, directory, proxy, model)
}

func writeTestFile(t *testing.T, name, content string) {
	t.Helper()
	file, err := os.Create(name)
	if err != nil {
		t.Fatal(err)
	}
	if _, err = io.WriteString(file, content); err != nil {
		t.Fatal(err)
	}
	if err = file.Close(); err != nil {
		t.Fatal(err)
	}
}

func TestServiceCLIConfiguration(t *testing.T) {
	binary := filepath.Join(t.TempDir(), "familyhome-service")
	if output, err := exec.Command("go", "build", "-o", binary, ".").CombinedOutput(); err != nil {
		t.Fatalf("build: %v %s", err, output)
	}
	proxy := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var payload struct {
			Model     string `json:"model"`
			Reasoning string `json:"reasoning_effort"`
		}
		if err := json.NewDecoder(r.Body).Decode(&payload); err != nil {
			t.Error(err)
		}
		if r.URL.Path != "/v2" || r.URL.Query().Get("key") != "fixture-proxy-secret" || r.URL.Query().Get("provider") != "openai" || payload.Reasoning != "low" || r.Header.Get("X-LLM-Proxy-Request-Timeout-Seconds") != "2" {
			t.Error("incorrect official request contract")
		}
		_, _ = io.WriteString(w, payload.Model)
	}))
	defer proxy.Close()
	environment := []string{"FAMILYHOME_DEVICE_TOKEN=" + testDeviceToken, "LLM_PROXY_SECRET=fixture-proxy-secret", "LLM_PROXY_MODEL=must-not-override-yaml"}
	for _, model := range []string{"model-one", "model-two"} {
		t.Run(model, func(t *testing.T) {
			listener, err := net.Listen("tcp", "127.0.0.1:0")
			if err != nil {
				t.Fatal(err)
			}
			address := listener.Addr().String()
			_ = listener.Close()
			path := filepath.Join(t.TempDir(), "config.yml")
			writeTestFile(t, path, startupYAML(address, t.TempDir(), proxy.URL, model))
			command := exec.Command(binary, "--config", path)
			command.Env = environment
			var output bytes.Buffer
			command.Stdout = &output
			command.Stderr = &output
			if err = command.Start(); err != nil {
				t.Fatal(err)
			}
			defer func() { _ = command.Process.Kill(); _ = command.Wait() }()
			client := &http.Client{Timeout: time.Second}
			ready := false
			for deadline := time.Now().Add(5 * time.Second); time.Now().Before(deadline); {
				response, err := client.Get("http://" + address + "/healthz")
				if err == nil {
					response.Body.Close()
					if response.StatusCode == 200 {
						ready = true
						break
					}
				}
				time.Sleep(20 * time.Millisecond)
			}
			if !ready {
				t.Fatal("service did not start from the canonical YAML")
			}
			request, _ := http.NewRequest(http.MethodPost, "http://"+address+"/v1/ask", strings.NewReader(`{"profile_id":"alice","name":"Alice","question":"Why?"}`))
			request.Header.Set("Content-Type", "application/json")
			authorize(request)
			response, err := client.Do(request)
			if err != nil {
				t.Fatal(err)
			}
			defer response.Body.Close()
			body, _ := io.ReadAll(response.Body)
			if response.StatusCode != 200 || !strings.Contains(string(body), model) {
				t.Fatalf("response %d: %s", response.StatusCode, body)
			}
		})
	}
	valid := startupYAML("127.0.0.1:0", t.TempDir(), proxy.URL, "model-one")
	cases := map[string]string{
		"unknown field":      valid + "unexpected: true\n",
		"duplicate field":    strings.Replace(valid, "  model: ", "  model: first\n  model: ", 1),
		"multiple documents": valid + "---\nserver: {}\n",
		"missing secret":     strings.Replace(valid, "${LLM_PROXY_SECRET}", "${UNSET_TEST_SECRET}", 1),
		"short device token": strings.Replace(valid, "${FAMILYHOME_DEVICE_TOKEN}", "short", 1),
		"invalid budget":     strings.Replace(valid, "request_timeout_seconds: 2", "request_timeout_seconds: 0", 1),
		"missing model":      strings.Replace(valid, "  model: \"model-one\"\n", "", 1),
		"placeholder model":  strings.Replace(valid, "model-one", "<selected-model-id>", 1),
		"malformed YAML":     "server: [\n",
	}
	for name, content := range cases {
		t.Run(name, func(t *testing.T) {
			path := filepath.Join(t.TempDir(), "config.yml")
			writeTestFile(t, path, content)
			command := exec.Command(binary, "--config", path)
			command.Env = environment
			output, err := command.CombinedOutput()
			if err == nil || strings.Contains(string(output), "listening") {
				t.Fatal("invalid configuration started a listener")
			}
			if strings.Contains(string(output), "fixture-proxy-secret") || strings.Contains(string(output), testDeviceToken) {
				t.Fatal("startup disclosed a secret")
			}
		})
	}
	for _, args := range [][]string{nil, {"--config", filepath.Join(t.TempDir(), "missing.yml")}} {
		command := exec.Command(binary, args...)
		command.Env = environment
		if err := command.Run(); err == nil {
			t.Fatal("missing config must fail")
		}
	}
}
