# FamilyHome

FamilyHome is a reversible, child-friendly home experience for first-generation Meta Portal devices. It replaces the Android home screen with large, profile-aware activities while preserving Meta's original launcher and system packages.

The current build was validated on a 1280 × 800 Portal running Android 9 (API 28).

## What it provides

- named child profiles with direct switching;
- separate calendars, reading timers, drawings, brush choices, and game availability for each child;
- a full-screen drawing studio with illustrated controls, colors, brushes, zoom, local saving, and sharing;
- typed and recorded Ask requests routed through the official provider-neutral LLM Proxy client;
- a read-only iCalendar/ICS next-event card;
- optional Adventure Game and Kart Adventure launchers; and
- a balanced child-facing layout with the activity dock anchored to the bottom of the screen.

## Repository layout

| Path | Purpose |
| --- | --- |
| `android/` | Native Android 9 home application, package `com.mprlab.portal` |
| `service/` | LAN companion service for Ask, calendar fetching, and drawing sharing |
| `games/freedoom-portal/` | Source of the small Portal-specific adapter for Freedoom for Android |

SuperTuxKart is used unmodified and is installed from its official Android release. It is not vendored in this repository.

## Build and test

The service requires Go and the Android app requires an Android SDK containing platform 35 and build-tools 36.1.0.

```sh
make test

cd android
ANDROID_SDK_ROOT=/path/to/android-sdk ./build.sh
```

Signing variables and installation instructions are documented in [`android/README.md`](android/README.md). Service configuration is documented in [`service/README.md`](service/README.md).

## Runtime boundary

The Android application contains no LLM credential. Ask requests go to the LAN companion service, which uses `github.com/tyemirov/llm-proxy/pkg/llmproxyclient`. Provider, model, and `LLM_PROXY_SECRET` configuration remain on the service host.

Update the example LAN address in `service/config.yml` and `android/app/src/main/java/com/mprlab/portal/PortalConfig.java` for the target network before building.

## Device safety and rollback

Installation uses ordinary ADB sideloading and Android's preferred-HOME mechanism. The original Meta launcher remains installed. Restore it with:

```sh
adb shell cmd package set-home-activity \
  com.facebook.alohaapps.launcher/com.facebook.aloha.app.home.touch.HomeActivity
```

Factory reset and system-package removal are outside the FamilyHome installation process.

## Third-party games

Freedoom for Android and SuperTuxKart remain subject to their upstream licenses. FamilyHome includes no commercial Doom data, game APKs, signing keys, or generated binaries.
