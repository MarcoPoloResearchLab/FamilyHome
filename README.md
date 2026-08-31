# FamilyHome

FamilyHome is a reversible, child-friendly home experience for first-generation Meta Portal devices. It replaces the Android home screen with large, profile-aware activities while preserving Meta's original launcher and system packages.

The current build was validated on a 1280 × 800 Portal running Android 9 (API 28).

## What it provides

- named child profiles with direct switching;
- separate calendars, reading timers, drawings, brush choices, and game availability for each child;
- a full-screen drawing studio with illustrated controls, colors, brushes, zoom, local saving, and sharing;
- a full-screen, two-octave piano with multi-touch chords, note labels, and offline synthesized sound;
- typed and recorded Ask requests routed through the official provider-neutral LLM Proxy client;
- a read-only iCalendar/ICS next-event card;
- optional Adventure Game and Kart Adventure launchers; and
- a balanced child-facing layout with the activity dock anchored to the bottom of the screen.

## Repository layout

| Path | Purpose |
| --- | --- |
| `android/` | Native Android 9 home application, package `com.mprlab.portal` |
| `service/` | Deployed companion service for Ask, calendar fetching, and drawing sharing |
| `games/freedoom-portal/` | Source of the small Portal-specific adapter for Freedoom for Android |

SuperTuxKart is used unmodified and is installed from its official Android release. It is not vendored in this repository.

## Build and test

The service requires Go 1.26.5. The Android app requires a JDK, `zip`, and an Android SDK containing platform-tools, platform 35, and build-tools 36.1.0.

```sh
export ANDROID_SDK_ROOT=/path/to/android-sdk
make ci
```

`make ci` runs the Go tests and build plus the host-side Android APK contract test with safe development configuration. The destructive upgrade/persistence test requires a dedicated running emulator:

```sh
ANDROID_SERIAL=emulator-5554 make test-android-upgrade
```

Signing variables and installation instructions are documented in [`android/README.md`](android/README.md). Service configuration is documented in [`service/README.md`](service/README.md).

## Runtime and authentication boundary

The Android application contains no LLM credential. Ask requests go to `https://familyhome-api.mprlab.com`, which uses `github.com/tyemirov/llm-proxy/pkg/llmproxyclient`. Provider, model, and `LLM_PROXY_SECRET` configuration remain on the service host.

Each Portal build carries one FamilyHome device bearer token. The backend requires it for every `/v1/` request. Drawing share URLs use random capability identifiers and remain accessible to recipients without the device token.

## MPR deployment

The application owns its deployment resources in `.mprlab/deploy/resources.yml`. The manifest builds a non-root container, retains drawing data, exposes the service through the MPR Caddy runtime, and verifies `https://familyhome-api.mprlab.com/healthz`.

Create the ignored `.mprlab/deploy/.env` file with mode `0600`:

```dotenv
FAMILYHOME_DEVICE_TOKEN=<64-character installation token>
LLM_PROXY_SECRET=<LLM Proxy key>
```

The production lifecycle is:

```sh
make release && make publish && make deploy
```

The operator runs this command after the `familyhome-api.mprlab.com` DNS record points to the MPR gateway and the private input is present.

## Device safety and rollback

Installation uses ordinary ADB sideloading and Android's preferred-HOME mechanism. The original Meta launcher remains installed. Restore it with:

```sh
adb shell cmd package set-home-activity \
  com.facebook.alohaapps.launcher/com.facebook.aloha.app.home.touch.HomeActivity
```

Factory reset and system-package removal are outside the FamilyHome installation process.

## Third-party games

Freedoom for Android and SuperTuxKart remain subject to their upstream licenses. FamilyHome includes no commercial Doom data, game APKs, signing keys, or generated binaries.
