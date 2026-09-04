# Development and service operations

## Repository layout

| Path | Purpose |
| --- | --- |
| `android/` | Native Android application, package `com.mprlab.portal` |
| `service/` | Companion service for Ask, calendars, weather, and drawing share links |
| `games/` | Source adaptations for separately installed games |
| `screenshots/` | App captures with sample data and capture records |
| `docs/MULTI-FAMILY.md` | Overview of the proposal in P002 |

## Build and checks

The service requires Go 1.26.5.
The Android build requires a JDK, `zip`, Android platform-tools, platform 35, and build-tools 36.1.0.

From the repository root, run:

```sh
export ANDROID_SDK_ROOT=/path/to/android-sdk
make ci
```

`make ci` runs the Go tests, service build, and Android APK contract test.
The APK contract test supplies development configuration.

For upgrade acceptance, select a dedicated emulator:

```sh
ANDROID_SERIAL=emulator-5554 make test-android-upgrade
```

This test installs application fixtures and changes emulator data.
The [Android guide](../android/README.md) contains additional device test commands.
The [setup guide](INSTALL.md) contains signing and installation steps.

## Current service boundary

The production service address is `https://familyhome-api.mprlab.com`.
The Android app sends connected requests to the FamilyHome service.
The service uses `github.com/tyemirov/llm-proxy/pkg/llmproxyclient` for Ask.
Provider, model, and `LLM_PROXY_SECRET` configuration stay on the service host.
Weather lookup and forecast conversion also occur in the service.

The backend requires `FAMILYHOME_DEVICE_TOKEN` for each `/v1/` request.
Each APK for the installation contains the same token.
Drawing share URLs contain random capability identifiers and permit recipient access without the device token.

P002 defines the replacement contract for parent authentication, individual devices, family ownership, and usage limits.
The [service guide](../service/README.md) documents the current API and local development process.

## MPR deployment

The application owns `.mprlab/deploy/resources.yml`.
The manifest defines the container, retained drawing data, service routing, and the `/healthz` check.

1. Create the ignored `.mprlab/deploy/.env` file with the operator's private inputs:

   ```dotenv
   FAMILYHOME_DEVICE_TOKEN=<64-character installation token>
   LLM_PROXY_SECRET=<LLM Proxy key>
   ```

2. Confirm that `familyhome-api.mprlab.com` resolves to the MPR gateway.
3. After deployment approval, run the production lifecycle from the repository root:

   ```sh
   make release && make publish && make deploy
   ```

Source validation, backend deployment, APK distribution, and Portal acceptance are separate results.
