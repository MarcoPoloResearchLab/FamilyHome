# FamilyHome service

The Go service keeps the LLM Proxy secret and model selection off the Portal.
It also retrieves calendars and weather and stores shared PNG drawings.
The deployment hostname is `https://familyhome-api.mprlab.com`.
The production manifest places the service on the MPR gateway host.

## Authentication

Every `/v1/` request requires `Authorization: Bearer <device token>`.
`GET /healthz` checks drawing storage without authentication.
A drawing URL contains a random 128-bit identifier and acts as a shareable capability link.
The service does not expose a drawing directory listing.

Create an installation token with `openssl rand -hex 32`.
Put the same token in the private deployment input and the APK build input.
Token rotation requires a service deployment and a replacement APK.
This shared installation token does not establish a child's identity or isolate families.
P002 owns the future family authorization contract.

## Weather

`GET /v1/weather?location=<ZIP or city>` uses the Open-Meteo geocoding and forecast APIs.
It returns the location, child-facing condition, icon category, current temperature, apparent temperature, daily high and low, and precipitation probability.
The operation requires the FamilyHome device token and no weather-provider key.
The Android weather card displays Open-Meteo attribution.

## Configuration

`service/config.yml` owns all server, LLM Proxy, and Ask settings.
The service requires `--config <path>` and reads the file once at startup.
It rejects missing fields, unresolved references, duplicate keys, unknown keys, multiple YAML documents, and invalid limits.
The former independent environment settings no longer select the model or server policy.
Only explicit `${VARIABLE}` references in YAML obtain environment values.

For a local run, change `server.listen_address` to `127.0.0.1:8765` and `server.data_dir` to `./data` in `config.yml`.
Supply the two secrets, then run the service from this directory:

```sh
export FAMILYHOME_DEVICE_TOKEN='<installation token with at least 32 characters>'
export LLM_PROXY_SECRET='<LLM Proxy tenant secret>'
go run . --config config.yml
```

The tracked selection is `openai`, `gpt-5-mini`, `low` reasoning effort, and a 45-second request work budget.
Change `llm_proxy.provider`, `llm_proxy.model`, and `llm_proxy.reasoning_effort` together to select a different model.
Restart the backend after a configuration change. No Android rebuild is necessary for a model change.
The container includes this YAML at `/app/config.yml`.
For the production image, rebuild the image after a YAML change and deploy that image.
The deployment manifest supplies the secrets. It does not repeat model settings.

The official Go client resolves through:

```sh
go get github.com/tyemirov/llm-proxy/pkg/llmproxyclient@latest
```

Startup constructs one validated client. Each question uses `NewMessagesRequest` and `PostMessages`.
The client sends the configured budget through `X-LLM-Proxy-Request-Timeout-Seconds`.
The `ask` configuration also sets transfer allowance, question length, recording duration, upload size, and concurrent request limits.
The initial limits are 2,000 Unicode characters, 60 seconds of recording, 6 MiB of audio, and one active request.

## Ask HTTP contract

`GET /v1/ask/settings` returns the client deadline and the question, recording, and upload limits.
It returns no model selection or LLM Proxy secret.
Android reads settings when Ask opens and before submission.
The initial client deadline is 90 seconds: the work budget plus three transfer allowances.
The allowances cover upload, capability discovery, and proxy transfer. Android cancels the connection at the total deadline.
The HTTP server limits request reads to one transfer allowance, which initially permits 15 seconds.

`POST /v1/ask` accepts one JSON object with `profile_id`, `name`, and `question` string fields.
It rejects unknown or repeated fields and trailing JSON.
The profile ID and name provide untrusted personalization. They do not authorize child-owned resources.
The backend keeps the child's name and question out of system instructions.

`POST /v1/ask/audio` accepts multipart fields `profile_id`, `name`, and one `audio` file.
It accepts M4A, WAV, and MPEG audio with matching container signatures.
The Portal records AAC audio in M4A.
The backend checks the selected model in the official public capability catalog before audio submission.
It returns `unsupported_audio` when that model lacks audio input. It does not select another model automatically.

On 2026-09-08, the public catalog listed text and image input for `openai:gpt-5-mini`, but no audio input.
Voice acceptance thus requires an explicit audio-capable model selection and an actual provider test.

A successful question returns HTTP 200 with `{"answer":"..."}`.
Ask errors contain `code` and a readable `error` string.
Authentication errors use the existing FamilyHome authentication response.

| HTTP status | Ask code | Meaning |
| --- | --- | --- |
| 400, 413, 415 | `invalid_question` | Invalid question fields, length, body, or media type |
| 400, 415 | `invalid_audio` | Invalid, empty, oversized, or unsupported recording |
| 422 | `unsupported_audio` | The selected model lacks audio input |
| 429 | `ask_busy` | The installation already uses its concurrent request allowance |
| 503 | `service_unavailable`, `provider_busy` | Capability discovery failed or the provider is busy |
| 502 | `provider_error`, `empty_answer` | The provider failed or returned no answer |
| 502, 504 | `outcome_unknown` | The connection or deadline ended without a confirmed answer |

The application does not retry provider requests automatically or keep conversation history.
Cancellation closes local work. It does not prove that the provider stopped or incurred no charge.
Questions, answers, recordings, credentials, and raw provider failures do not enter routine application logs.
P002 owns persistent duplicate prevention, retention policy, and future family accounting.

## Android behavior and validation

Ask permits one active recording or question.
The main controls remain visible when the answer area scrolls.
Navigation, activity pause, cancellation, and screensaver entry stop recording and speech and cancel network work.
Temporary recordings are removed after success, failure, and cancellation. A new activity also removes abandoned recordings.
The question draft survives request failures and activity recreation.
Invalid responses produce an error. A speech failure leaves the answer readable.
The Stop speaking control ends playback.

Run the service checks from the repository root:

```sh
make test-service
```

Run the Ask interface scenarios on a dedicated clean emulator:

```sh
ANDROID_SERIAL=emulator-5586 make test-android-ask
```

The Ask target covers normal text and the 1.3 font scale, microphone denial, recording cleanup, and screensaver cancellation.
It uses a local HTTP server and captures screenshots in `android/build/tests/ask`.
Service tests use the actual executable and HTTP listeners with the official client.
The audio fixture contains a generated tone. It proves byte transport, not speech understanding.
Local tests do not establish provider access, voice understanding, or audible answers on the physical Portal.
I009 records that remaining acceptance work.
