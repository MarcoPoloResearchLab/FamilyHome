# Tiles toolbar adaptation

Tiles puts Back, Home, and the game menu in one toolbar.
The main menu tabs share the same row. Settings, Help, Statistics, and Paint use that row.
Home opens FamilyHome and keeps the game task.

The upstream source revision is `7da4fa622e1f2510894f85d98e3d1653b81184f4`.
The build verifies the source archive hash before extraction.
It applies `upstream.patch` and copies `overlay/` into the source tree.
See `UPSTREAM-LICENSE` for the upstream license.

## Build

Set `ANDROID_SDK_ROOT` to the installed Android SDK.
Use Java 21.
Use Node.js and npm with the upstream package lock.

Run `make build-tiles` from the FamilyHome root.
Set `PORTAL_KEYSTORE`, `PORTAL_KEYSTORE_PASSWORD`, and `PORTAL_KEY_PASSWORD` to sign the APK.

The build creates `Tiles-Portal.apk` and `Tiles-Portal-source.tar.gz` under `android/build/tiles-portal/`.
Keep the APK and its source archive together when you distribute the adaptation.

## Validation and installation

Install FamilyHome, Blocks, and Tiles on a dedicated emulator with a 1280 by 800 screen and density 160.
Run `ANDROID_SERIAL=emulator-5582 make test-games-toolbar`.
The tests use the installed applications through Android UI automation.

The upstream Portal game used a different signing certificate.
Android requires removal of a game if its signing certificate differs from the adaptation.
Removal deletes the game data.
Get user approval for that removal before installation.
Verify the toolbar and navigation on the physical Portal after installation.

The user approved the initial replacement on the Portal.
The signed adaptation is installed and passed physical toolbar and navigation checks.
Updates with the same signing certificate can preserve game data.
