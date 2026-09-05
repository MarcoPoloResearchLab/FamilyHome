# Install the developer preview

FamilyHome currently requires a source build and ADB installation.
The current APK contains the service address and one shared installation credential.
P002 defines the future installation process with individual device credentials.

## Requirements

- A first-generation Meta Portal with Android 9 (API 28).
- Working ADB access to that Portal.
- A computer with a JDK, `zip`, and an Android SDK.
- Android platform-tools, platform 35, and build-tools 36.1.0.
- A signing key for the APK.
- A service address and installation credential from the service operator.

The supported screen layout is landscape at 1280 × 800.
Support for other Portal models requires separate hardware acceptance.
This procedure starts with an operational Portal and working ADB access.

## Build the APK

1. Open a terminal in the repository root.
2. Set the SDK location:

   ```sh
   export ANDROID_SDK_ROOT=/path/to/android-sdk
   ```

3. Set the build inputs:

   ```sh
   export FAMILYHOME_SERVICE_BASE_URL='https://your-familyhome-service.example'
   export FAMILYHOME_DEVICE_TOKEN='<installation credential from the service operator>'
   export PORTAL_KEYSTORE='/path/to/signing-key.jks'
   export PORTAL_KEYSTORE_PASSWORD='<keystore password>'
   export PORTAL_KEY_PASSWORD='<key password>'
   ```

4. Build the signed APK:

   ```sh
   cd android
   ./build.sh
   ```

The script prints the output path under `android/build/local`.
The current filename is `Children-Portal-v0.13.0.apk`.
The package name is `com.mprlab.portal`.
The [Android guide](../android/README.md) contains the build contract.

## Install on the selected Portal

1. List the attached devices:

   ```sh
   adb devices
   ```

2. Select the Portal serial number:

   ```sh
   export ANDROID_SERIAL='<Portal serial number>'
   ```

3. From the `android` directory, install the APK:

   ```sh
   adb install -r build/local/Children-Portal-v0.13.0.apk
   ```

4. Open FamilyHome:

   ```sh
   adb shell am start -n com.mprlab.portal/.MainActivity
   ```

5. Open Settings to configure child profiles.
6. Select FamilyHome as the home app:

   ```sh
   adb shell cmd package set-home-activity com.mprlab.portal/.MainActivity
   ```

An update requires the same signing key as the installed APK.
The `-r` option requests an update that retains application data.

## Configure optional features

1. In Settings, enter an ICS feed URL for each child who needs a calendar.
2. Enter a ZIP code or city to show the household weather card.
3. Install the selected game APKs from the [game guide](../android/GAMES.md).

Ask, calendar access, weather, and drawing share links require a working companion service.
The [service guide](../service/README.md) describes its configuration.
The current shared credential identifies the installation, not an individual family.

## Restore the Meta home screen

With `ANDROID_SERIAL` still set to the intended Portal, run:

```sh
adb shell cmd package set-home-activity \
  com.facebook.alohaapps.launcher/com.facebook.aloha.app.home.touch.HomeActivity
```

This changes the home-app selection. It retains FamilyHome and its local data.
