# FamilyHome Android app

`com.mprlab.portal` is the native Android 9 home app for FamilyHome. It keeps profiles, timers, drawing documents, brush choices, and per-child game availability on the device. Calendar feeds, drawing-link storage, and Ask requests go through the deployed companion service in `../service`. The game choices are Adventure Game (the Portal edition of Freedoom) and Kart Adventure (the official SuperTuxKart Android app).

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
adb install -r build/local/Children-Portal-v0.9.1.apk
adb shell cmd package set-home-activity com.mprlab.portal/.MainActivity
```

Restore the Meta launcher with:

```sh
adb shell cmd package set-home-activity com.facebook.alohaapps.launcher/com.facebook.aloha.app.home.touch.HomeActivity
```
