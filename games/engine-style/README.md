# FamilyHome engine interfaces

I005 adds the FamilyHome appearance to Kart.
The appearance contract is `android/STYLING.md`.

## Build

The build requires Java 17, `uv`, Android platform 36, and Android build tools 36.1.0.
Set `ANDROID_SDK_ROOT` to the Android SDK directory.

```sh
make build-kart
```

`sources.json` fixes the upstream APK versions and their SHA-256 digests.
The build verifies each digest before extraction.
It generates interface resources and compiles the Java adapters.
The native engine libraries come from the upstream APKs.
The build verifies that each native library stays the same.
It does not compile the native engines from source.

The output directory is `android/build/kart-portal/`.
It contains an unsigned APK, an interface source archive, and `interface-build.json`.
The build record contains the source digests and the unsigned APK digest.
The interface source archive contains the FamilyHome adaptation and shared font.
It does not contain the upstream engine source.
The build has fixed inputs. Byte-identical APK output is unverified.

If `PORTAL_KEYSTORE` is set, the build also creates and verifies a signed APK.
The signer reads `PORTAL_KEYSTORE_PASSWORD` and `PORTAL_KEY_PASSWORD` from the process environment.
Use the existing signing key for an installed game update.

## Interface ownership

`build.py` generates the Kart skin and includes Fredoka Bold.
The Kart adapter sets the skin and text size before engine initialization.
An interface digest controls asset extraction after an update.
The engine keeps its `home` directory during extraction.

## Validation

Use a dedicated 1280 x 800 emulator with a child profile and the installed game adaptations.
The interface test uses the FamilyHome Games entry point.
It captures screenshots and checks visible labels and background colors.

```sh
ANDROID_SERIAL=emulator-5588 make test-engine-style
```

The test requires macOS Vision for text recognition.
Screenshots go to `android/build/engine-style/evidence/`.
Run the test at font scales of 1.0 and 1.3.
Review the screenshots for text bounds and control dimensions.
Verify instructions, scores, results, and saved games separately.

I005 records the current validation results and remaining work.
