# Blocks toolbar adaptation

Blocks puts Back, Home, score, level, lines, and Settings in one toolbar.
Settings and the multiplayer screens use the same navigation controls.
Home opens FamilyHome and keeps the game task.

The upstream source revision is `019db6c2fc69937160901572192982b5cb843f4b`.
The build verifies the source archive hash before extraction.
It applies `upstream.patch` and copies `overlay/` into the source tree.
See `UPSTREAM-LICENSE` for the upstream license.

## Build

Set `ANDROID_SDK_ROOT` to the installed Android SDK.
Use Java 21.
Set `FLUTTER_ROOT` to Flutter 3.44.0 at revision `559ffa3f75e7402d65a8def9c28389a9b2e6fe42`.

Run `make build-blocks` from the FamilyHome root.
Set `PORTAL_KEYSTORE`, `PORTAL_KEYSTORE_PASSWORD`, and `PORTAL_KEY_PASSWORD` to sign the APK.

The build creates `Blocks-Portal.apk` and `Blocks-Portal-source.tar.gz` under `android/build/blocks-portal/`.
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
