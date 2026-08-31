# FamilyHome companion service

This LAN service keeps LLM Proxy credentials and model routing off the Portal. It also fetches iCalendar feeds and stores shareable PNG drawings.

Run from this directory:

```sh
export LLM_PROXY_SECRET='...'
go run . config.yml
```

The included configuration uses `http://192.168.1.121:8765` as an example LAN address. Update `server.public_base_url` and the app's service address for the service host before building the Android app.

`config.yml` is the single runtime configuration hierarchy. Ask uses the official `github.com/tyemirov/llm-proxy/pkg/llmproxyclient` package; the Android client never receives the LLM Proxy secret.

The API base is `https://llm-proxy-api.mprlab.com`. The similarly named `https://llm-proxy.mprlab.com` host is the public website, not the API service.
