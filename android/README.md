# FamilyHome Android app

`com.mprlab.portal` is the native Android 9 home app for FamilyHome. It keeps profiles, timers, drawing documents, brush choices, the household weather location, and a short-lived weather cache on the device. Its built-in activities include the family Settings screen, drawing studio, offline piano and guitar instruments, a native weather card, and the game library. Calendar feeds, weather lookup, drawing-link storage, and Ask requests go through the deployed companion service in `../service`.

The weather card is household-wide rather than child-specific. Enter a ZIP code or city in Settings to show it. Clear the field to remove the entire card from the home screen. FamilyHome does not reserve an empty weather slot.

The Home screen has one Games entry.
The game library shows Kart (SuperTuxKart), Blocks (Block Drop), Tiles (Tessel), and Match (Privacy Friendly Memo Game).
The library marks a game clearly when its separate APK has not been installed.

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

The upgrade test uses a legacy APK fixture signed with the same temporary test key as the current APK.
It supplies two profiles, old game-choice fields, timer settings, a weather location, a drawing, drawing-tool preferences, and an app-private sentinel.
The test installs the current APK with `adb install -r`.
It verifies that the data survives and that obsolete game-choice fields are removed.
It also verifies the Home entry, all four games, and the applicable activities.

## Games

Game APKs remain independent of FamilyHome. Install each reviewed APK with ordinary ADB. FamilyHome shows the complete catalog and launches these package/activity pairs:

| Game | Package | Activity |
| --- | --- | --- |
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

The physical Portal audit found no stacked headers in Kart.
Kart uses fullscreen gameplay and game controls.
The audit included the game menus, active games, and the Kart pause menu.

Run the toolbar integration test on a dedicated emulator:

```sh
ANDROID_SERIAL=emulator-5582 make -C .. test-android-toolbar
```

## Ask

Ask sends typed questions and voice recordings through the FamilyHome backend.
The backend selects the model through `service/config.yml` and keeps the LLM Proxy secret off the device.
See the [service contract](../service/README.md) for configuration, HTTP fields, errors, and model capability limits.

The question field contains a microphone icon followed by a paper airplane icon at its right edge.
The empty input shows the instructions. Answers and status messages appear below the input without a colored panel.
The field and its controls stay visible when the answer scrolls.
The microphone starts a recording and changes to a red square.
The red square stops recording and requests a transcript. Ask adds the transcript to the input for review and changes.
During recording, the paper airplane stops recording, waits for the transcript, and sends the combined typed and spoken text.
The recording limit also stops recording for transcription without question submission.
For a typed question, the paper airplane sends the text.
Both icons have accessibility labels and 64 dp touch regions.
Only one question or recording can be active.
Cancel stops local work. The provider can continue processing a dispatched question.

Back, Home, activity pause, and screensaver entry cancel local work and stop speech.
Ask removes temporary recordings after completion, failure, cancellation, and activity restart.
The question draft survives request failures and activity recreation.
Answers play automatically. An unavailable speech engine leaves the answer readable.
Ask has no start or stop buttons for speech playback.
If transcription fails, Ask preserves the typed text and does not submit a question.

The current model selection supports typed and spoken questions through the same official client.
`service/config.yml` owns the selection. Ask does not change models automatically.
The current speech language is US English.

Run the Ask interface tests on a dedicated clean emulator:

```sh
ANDROID_SERIAL=emulator-5586 make -C .. test-android-ask
```

The target covers normal text, the 1.3 font scale, HTTP responses, duplicate submissions, deadlines, recordings, microphone denial, and screensaver cleanup.
Screenshots are in `build/tests/ask`.
Actual model understanding and physical Portal audio remain separate acceptance steps under I009.

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

## Photo Booth

Photo Booth takes pictures on the Portal without a service connection.
Each child has a separate photo album on this device.
The camera preview reflects the scene like a mirror. Saved pictures keep normal text direction.

1. Select a child on Home.
2. Open Photo Booth.
3. Permit camera access when Android requests it.
4. Select the single-picture icon or the four-picture strip icon.
5. Select the plain, Stars, or Confetti frame preview.
6. Open Filters to select a photo filter.
7. Select Back to return to the picture controls.
8. Step into the picture. Photo Booth follows one visible person automatically.
9. If several persons appear, tap your face inside its yellow rectangle.
10. Select Take picture.
11. Wait for the three-second countdown before each picture.
12. Select Save, Retake, or Discard on the review screen.
13. Open Album to see saved pictures.

Funhouse choices are Big nose, Stretch, and Mirror twins.
Face accessories are Bunny ears, Googly eyes, and Party hat.
Original removes the photo filter.
Keep your face near the image center for Big nose.
Face accessories require a visible face directed toward the camera.
The interface shows a face instruction when face detection finds no face.
A picture without a detected face has no face accessory.
The selected photo filter applies to every picture in a photo strip.
Saved pixels contain the photo filter. Existing photos and the metadata schema stay the same.

Person tracking starts automatically with a maximum zoom of 2× when one face appears.
The crop expands when necessary to include the head, hair, and shoulders.
Several faces produce a full camera preview with rectangles for touch selection.
After a selection, Photo Booth follows that person while other persons remain visible.
After tracking loss, Photo Booth detects persons again without a setup command.
Zoom offers optional maximum settings of 1×, 1.5×, 2×, and 3×.
Higher zoom uses fewer source pixels in saved pictures.
See [camera controls](PHOTO_BOOTH_CAMERA.md) for the physical camera findings and tracking limits.

Local face detection uses the bundled YuNet model through OpenCV.
The filtered preview uses images with at most 640 pixels on the longest side.
Full-size pictures use the same effect before JPEG encoding.
The renderer includes the source, effect output, and face-detection image in its memory calculation.

See [camera controls](PHOTO_BOOTH_CAMERA.md) for the physical Portal investigation.

To remove a picture, open its album entry and select Delete.
Confirm the deletion in the dialog.
Each album permits 100 entries or 256 MiB, whichever occurs first.
All photo albums together permit 512 MiB.
The capacity calculation includes images, thumbnails, and metadata.
The application keeps saved pictures until explicit deletion.
A photo strip saves one combined image. The application removes its temporary individual pictures.

The screensaver closes the camera and cancels an incomplete sequence.
A completed review image remains available after wake.
The first wake input activates no photo control.

The camera adapter uses Camera2 YUV output and application JPEG encoding.
The physical Portal qualification found invalid native JPEG output.
A closed camera cover produces an obscured picture. Android reports no camera error for that condition.
The current YUV path works on the Portal and emulator.

Run the application integration test on a dedicated emulator:

```sh
ANDROID_SERIAL=emulator-5584 make -C .. test-android-photobooth
```

The test covers pictures, strips, frames, review, albums, deletion, capacity, screensaver interruption, and persistence after an APK update.
Physical camera evidence and final device acceptance remain separate from emulator results.
