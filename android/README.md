# FamilyHome Android app

`com.mprlab.portal` is the native Android 9 home app for FamilyHome. It keeps profiles, timers, drawing documents, brush choices, the household weather location, and a short-lived weather cache on the device. Its built-in activities include the family Settings screen, drawing studio, offline piano and guitar instruments, a native weather card, and the game library. Calendar feeds, weather lookup, drawing-link storage, and Ask requests go through the deployed companion service in `../service`.

The weather card is household-wide rather than child-specific. Enter a ZIP code or city in Settings to show it. Clear the field to remove the entire card from the home screen. FamilyHome does not reserve an empty weather slot.

The Home screen has one Games entry. The game library always shows Freedoom, Kart (SuperTuxKart), Blocks (Block Drop), Tiles (Tessel), and Match (Privacy Friendly Memo Game). The library marks a game clearly when its separate APK has not been installed.

The exact third-party builds accepted on the physical Portal, including hashes, signing certificates, and permissions, are recorded in [`GAMES.md`](GAMES.md).

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
adb install -r build/local/Children-Portal-v0.13.0.apk
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

The upgrade test uses a frozen legacy APK fixture signed with the same temporary test key as the current APK. It seeds two profiles, old game-choice fields, timer settings, a weather location, a drawing, drawing-tool preferences, and an app-private sentinel. The test installs the current APK with `adb install -r`. It then verifies that the data survives and that obsolete game-choice fields are removed. It also verifies the Home entry, all five games, and the applicable activities.

## Games

Game APKs remain independent of FamilyHome. Install each reviewed APK with ordinary ADB. FamilyHome shows the complete catalog and launches these package/activity pairs:

| Game | Package | Activity |
| --- | --- | --- |
| Freedoom | `net.nullsum.freedoom` | `net.nullsum.freedoom.PortalGameActivity` |
| Kart | `org.supertuxkart.stk` | `org.supertuxkart.stk.SuperTuxKartActivity` |
| Blocks | `com.blockdrop.game` | `com.blockdrop.game.MainActivity` |
| Tiles | `net.vantulder.tessel` | `net.vantulder.tessel.MainActivity` |
| Match | `org.secuso.privacyfriendlymemory` | `org.secuso.privacyfriendlymemory.ui.SplashActivity` |

Removing one game is reversible and does not affect FamilyHome or the other games:

```sh
adb uninstall <game-package>
```

Run it only against a dedicated emulator. The script refuses physical devices and emulators that already contain `com.mprlab.portal` unless the corresponding explicit override is set.

```sh
ANDROID_SDK_ROOT=/path/to/android-sdk \
ANDROID_SERIAL=emulator-5554 \
./tests/upgrade-persistence.sh
```

The upgrade test sets the emulator to the Portal's 1280 × 800 geometry for its UI smoke checks and restores the previous display override afterward.

## Combined toolbar

Games, Settings, Ask, Drawing, Music, Piano, and Guitar show Back and Home in the application toolbar.
The toolbar remains visible when the content scrolls.
Drawing saves the current document before Back or Home opens another screen.
The main Home screen uses its existing profile, clock, and Settings row.

The Match adaptation puts Back, Home, the game menu, and game status in one toolbar.
Blocks puts its score and settings controls in the navigation row.
Tiles puts its menu tabs and navigation controls in one top row.
See the build procedures for [Match](../games/match-portal/README.md), [Blocks](../games/blocks-portal/README.md), and [Tiles](../games/tiles-portal/README.md).

The physical Portal audit found no stacked headers in Freedoom or Kart.
Both games use fullscreen gameplay and game controls.
The audit included the game menus, active games, and the Kart pause menu.

Run the toolbar integration test on a dedicated emulator:

```sh
ANDROID_SERIAL=emulator-5582 make -C .. test-android-toolbar
```

## Music

Music contains Piano and Guitar. Both instruments work without network access.
The guitar has six strings with standard tuning and five fret positions.
The colored section shows the vibrating string length. A shorter section produces a higher note.
The guitar generates its audio on the device.

1. Open Music.
2. Select Piano or Guitar.
3. In Guitar, touch a string at a fret to play its note.
4. Select C, G, Am, or F to show the chord positions.
5. Move a finger across the strumming area to play the selected strings.
6. Select Open strings to remove the chord positions.

An `×` marks a string that the selected chord does not play.
The guitar accepts simultaneous touches. Back and Home stop guitar audio.
The volume buttons control media volume.

Run the guitar integration test on a dedicated emulator:

```sh
ANDROID_SERIAL=emulator-5580 make -C .. test-android-guitar
```

The test covers instrument navigation, notes, chords, fast strums, simultaneous touches, audio cleanup, and Portal screen dimensions.
