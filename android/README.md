# FamilyHome Android app

`com.mprlab.portal` is the native Android 9 home app for FamilyHome. It keeps profiles, timers, drawing documents, brush choices, the household weather location, and per-child game availability on the device. Its built-in activities include the family Settings screen, drawing studio, and an offline two-octave piano. Calendar feeds, drawing-link storage, and Ask requests go through the deployed companion service in `../service`. The game choices are Adventure Game (the Portal edition of Freedoom) and Kart Adventure (the official SuperTuxKart Android app).

The build injects the deployed service address and installation-specific device token into a generated class. Neither value is tracked in Git.

## Build

Set `ANDROID_SDK_ROOT` to an Android SDK containing build-tools 36.1.0 and platform 35, then run:

```sh
export FAMILYHOME_SERVICE_BASE_URL=https://familyhome-api.mprlab.com
export FAMILYHOME_DEVICE_TOKEN='<same token as .mprlab/deploy/.env>'
./build.sh
```

The script creates an aligned APK under `build/local`. Set `PORTAL_KEYSTORE`, `PORTAL_KEYSTORE_PASSWORD`, and `PORTAL_KEY_PASSWORD` to produce an installable signed APK. The token is an app credential and can be extracted from the APK, so it is scoped to FamilyHome and can be rotated independently.

## Install and select as Home

```sh
adb install -r build/local/Children-Portal-v0.10.0.apk
adb shell cmd package set-home-activity com.mprlab.portal/.MainActivity
```

Restore the Meta launcher with:

```sh
adb shell cmd package set-home-activity com.facebook.alohaapps.launcher/com.facebook.aloha.app.home.touch.HomeActivity
```

## Regression tests

The fast APK contract test builds a debuggable APK with non-production configuration and verifies its package identity, SDK contract, activities, provider, and generated runtime boundary:

```sh
ANDROID_SDK_ROOT=/path/to/android-sdk ./tests/apk-contract.sh
```

The upgrade test uses a frozen legacy APK fixture signed with the same temporary test key as the current APK. It seeds two profiles, timer and game settings, a weather location, a drawing, drawing-tool preferences, and an app-private sentinel; installs the current APK with `adb install -r`; then verifies that the data, Settings entry point, and child-facing activities survive the replacement.

Run it only against a dedicated emulator. The script refuses physical devices and emulators that already contain `com.mprlab.portal` unless the corresponding explicit override is set.

```sh
ANDROID_SDK_ROOT=/path/to/android-sdk \
ANDROID_SERIAL=emulator-5554 \
./tests/upgrade-persistence.sh
```

The upgrade test sets the emulator to the Portal's 1280 × 800 geometry for its UI smoke checks and restores the previous display override afterward.
