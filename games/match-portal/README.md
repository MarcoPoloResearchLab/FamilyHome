# Match toolbar adaptation

Match uses one toolbar for Back, Home, the game menu, and game status.
Back asks before it closes an active game.
Home opens FamilyHome and keeps the game task.
The Help, Settings, About, Statistics, and Highscore screens also show Back and Home.

The source is SECUSO Privacy Friendly Memo Game 1.1.3.
The build uses these upstream revisions:

- Memo Game: `69e89f31ce5ae13a4cb11ca793dc7276af8000d0`.
- Backup API: `254885602ee4501a572f56306fbf2b3712cfc834`.

`build.sh` verifies each source archive hash before extraction.
It applies `upstream.patch` and copies the source files from `overlay/`.
The package remains `org.secuso.privacyfriendlymemory`.
The source and adaptation use the license in `UPSTREAM-LICENSE`.

## Build

Set `ANDROID_SDK_ROOT` to the installed Android SDK.
Use Java 17.
Run this command from the FamilyHome root:

```sh
make build-match
```

The build creates these files under `android/build/match-portal/`:

- `Match-Portal-debug.apk`: The signed APK for emulator validation.
- `Match-Portal-unsigned.apk`: The unsigned release APK.
- `Match-Portal-source.tar.gz`: The complete source archive with upstream licenses.

Set `PORTAL_KEYSTORE`, `PORTAL_KEYSTORE_PASSWORD`, and `PORTAL_KEY_PASSWORD` for a signed `Match-Portal.apk`.
Keep the APK and its source archive together when you distribute the adaptation.

## Integration test

Use a dedicated emulator with the 1280 by 800 Portal screen dimensions and density 160.
Install FamilyHome and the Match APK on that emulator.
Run this command from the FamilyHome root:

```sh
ANDROID_SERIAL=emulator-5582 make test-match-toolbar
```

The test verifies toolbar dimensions, the game menu, Help, Back confirmation, and Home.
It saves `combined-toolbar.png` under `android/build/match-portal/`.

## Portal installation

The upstream F-Droid Match APK used a different signing certificate.
Android rejects an update from the adaptation signer.
Remove the installed Match APK and its application data before you install the adaptation.
Get user approval for that removal before installation.
Keep FamilyHome update validation separate from removal of the installed Match APK.
Verify the single toolbar and navigation on the physical Portal after installation.

The user approved the initial replacement on the Portal.
The signed adaptation is installed and passed physical toolbar and navigation checks.
Updates with the same signing certificate can preserve game data.
