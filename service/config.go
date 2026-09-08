package main

import (
	"errors"
	"fmt"
	"io"
	"net"
	"net/http"
	"os"
	"regexp"
	"strings"
	"time"

	"github.com/tyemirov/llm-proxy/pkg/llmproxyclient"
	"go.yaml.in/yaml/v3"
)

type config struct {
	Server struct {
		ListenAddress string `yaml:"listen_address"`
		DataDir       string `yaml:"data_dir"`
		DeviceToken   string `yaml:"device_token"`
	} `yaml:"server"`
	LLMProxy struct {
		BaseURL               string `yaml:"base_url"`
		Secret                string `yaml:"secret"`
		Provider              string `yaml:"provider"`
		Model                 string `yaml:"model"`
		ReasoningEffort       string `yaml:"reasoning_effort"`
		RequestTimeoutSeconds int    `yaml:"request_timeout_seconds"`
	} `yaml:"llm_proxy"`
	Ask struct {
		TransferAllowanceSeconds int   `yaml:"transfer_allowance_seconds"`
		QuestionCharacterLimit   int   `yaml:"question_character_limit"`
		RecordingDurationSeconds int   `yaml:"recording_duration_seconds"`
		AudioByteLimit           int64 `yaml:"audio_byte_limit"`
		ConcurrentRequestLimit   int32 `yaml:"concurrent_request_limit"`
	} `yaml:"ask"`
}

var environmentReference = regexp.MustCompile(`\$\{([A-Z][A-Z0-9_]*)\}`)

func loadConfig(path string) (config, error) {
	file, err := os.Open(path)
	if err != nil {
		return config{}, fmt.Errorf("open backend configuration: %w", err)
	}
	defer file.Close()
	decoder := yaml.NewDecoder(io.LimitReader(file, 1<<20))
	decoder.KnownFields(true)
	var value config
	if err = decoder.Decode(&value); err != nil {
		return config{}, errors.New("decode backend configuration: invalid YAML schema")
	}
	if decoder.Decode(&struct{}{}) != io.EOF {
		return config{}, errors.New("decode backend configuration: require one YAML document")
	}
	fields := []struct {
		name  string
		value *string
	}{
		{"server.listen_address", &value.Server.ListenAddress}, {"server.data_dir", &value.Server.DataDir},
		{"server.device_token", &value.Server.DeviceToken}, {"llm_proxy.base_url", &value.LLMProxy.BaseURL},
		{"llm_proxy.secret", &value.LLMProxy.Secret}, {"llm_proxy.provider", &value.LLMProxy.Provider},
		{"llm_proxy.model", &value.LLMProxy.Model}, {"llm_proxy.reasoning_effort", &value.LLMProxy.ReasoningEffort},
	}
	for _, field := range fields {
		missing := false
		resolved := environmentReference.ReplaceAllStringFunc(*field.value, func(reference string) string {
			content, present := os.LookupEnv(reference[2 : len(reference)-1])
			if !present || strings.TrimSpace(content) == "" {
				missing = true
			}
			return content
		})
		resolved = strings.TrimSpace(resolved)
		if missing || resolved == "" || strings.Contains(resolved, "${") || strings.HasPrefix(resolved, "<") {
			return config{}, fmt.Errorf("configure %s: explicit resolved value required", field.name)
		}
		*field.value = resolved
	}
	if _, _, err := net.SplitHostPort(value.Server.ListenAddress); err != nil {
		return config{}, errors.New("configure server.listen_address: host and port required")
	}
	if len(value.Server.DeviceToken) < 32 {
		return config{}, errors.New("configure server.device_token: at least 32 characters required")
	}
	// These bounds protect duration arithmetic, Android millisecond integers, and memory allocation.
	if value.LLMProxy.RequestTimeoutSeconds < 1 || value.LLMProxy.RequestTimeoutSeconds > 86400 || value.Ask.TransferAllowanceSeconds < 1 || value.Ask.TransferAllowanceSeconds > 3600 {
		return config{}, errors.New("configure Ask deadlines: work budget 1..86400 and transfer allowance 1..3600 seconds required")
	}
	if value.Ask.QuestionCharacterLimit < 1 || value.Ask.QuestionCharacterLimit > 8000 || value.Ask.RecordingDurationSeconds < 1 || value.Ask.RecordingDurationSeconds > 3600 || value.Ask.AudioByteLimit < 1024 || value.Ask.AudioByteLimit > maxRequestBytes || value.Ask.ConcurrentRequestLimit < 1 || value.Ask.ConcurrentRequestLimit > 100 {
		return config{}, errors.New("configure Ask limits: invalid question, recording, upload, or concurrency bound")
	}
	if _, err := value.messagesRequest([]llmproxyclient.MessageInput{{Role: "user", Content: "Validate request policy."}}); err != nil {
		return config{}, errors.New("configure llm_proxy: invalid request policy")
	}
	return value, nil
}

func (value config) messagesRequest(messages []llmproxyclient.MessageInput) (llmproxyclient.MessagesRequest, error) {
	return llmproxyclient.NewMessagesRequest(llmproxyclient.MessagesRequestInput{Messages: messages, Model: value.LLMProxy.Model,
		ReasoningEffort: &value.LLMProxy.ReasoningEffort, RequestTimeoutSeconds: &value.LLMProxy.RequestTimeoutSeconds})
}

func (value config) requestDuration() time.Duration {
	return time.Duration(value.LLMProxy.RequestTimeoutSeconds+value.Ask.TransferAllowanceSeconds) * time.Second
}

func (value config) responseDuration() time.Duration {
	// Reserve one allowance each for upload, capability discovery, and proxy transport.
	return time.Duration(value.LLMProxy.RequestTimeoutSeconds+3*value.Ask.TransferAllowanceSeconds) * time.Second
}

func newApplication(configuration config) (*application, error) {
	clientConfig, err := llmproxyclient.NewConfig(llmproxyclient.ConfigInput{BaseURL: configuration.LLMProxy.BaseURL, Secret: configuration.LLMProxy.Secret, Provider: configuration.LLMProxy.Provider})
	if err != nil {
		return nil, errors.New("configure llm_proxy: invalid connection policy")
	}
	client, err := llmproxyclient.NewClient(clientConfig, &http.Client{Timeout: configuration.requestDuration()})
	if err != nil {
		return nil, errors.New("construct llm_proxy client: invalid configuration")
	}
	return &application{config: configuration, client: client, http: &http.Client{Timeout: 60 * time.Second},
		weatherGeocodingURL: openMeteoGeocodingURL, weatherForecastURL: openMeteoForecastURL}, nil
}
