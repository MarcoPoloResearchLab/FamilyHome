# FamilyHome service

This service keeps LLM Proxy credentials and model routing off the Portal. It also fetches iCalendar feeds, resolves household weather locations, normalizes current forecasts, and stores shareable PNG drawings. The production service runs on the MPR gateway host and is available through `https://familyhome-api.mprlab.com`.

`GET /v1/weather?location=<ZIP or city>` uses Open-Meteo’s geocoding and forecast APIs and returns only the location label, child-facing condition, icon category, current and apparent temperatures, daily high and low, and precipitation probability. It requires the normal FamilyHome device token and no weather-provider key. The Android app displays Open-Meteo attribution in the weather card.

## Authentication

Every `/v1/` request requires `Authorization: Bearer <device token>`. `/healthz` is available for deployment checks. A drawing URL contains a random 128-bit identifier and acts as a shareable capability link; the service does not expose a drawing directory listing.

Create one installation token with `openssl rand -hex 32`. Put the same value in the deployment private input and the Portal APK build environment. Rotating the token requires a service deployment and a replacement APK.

## Local run

Set the complete environment contract, then run `go run .` from this directory:

```sh
export FAMILYHOME_LISTEN_ADDRESS=127.0.0.1:8765
export FAMILYHOME_DATA_DIR=./data
export FAMILYHOME_DEVICE_TOKEN='<64-character installation token>'
export LLM_PROXY_BASE_URL=https://llm-proxy-api.mprlab.com
export LLM_PROXY_SECRET='<LLM Proxy key>'
export LLM_PROXY_PROVIDER=openai
export LLM_PROXY_MODEL=gpt-5-mini
export LLM_PROXY_REASONING_EFFORT=low
export LLM_PROXY_REQUEST_TIMEOUT_SECONDS=45
go run .
```

Ask uses the official `github.com/tyemirov/llm-proxy/pkg/llmproxyclient` package. The Android client receives the FamilyHome device token but never receives the LLM Proxy secret.
