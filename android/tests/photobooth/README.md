# Photo Booth test

Run `make test-android-photobooth` on a dedicated emulator with a front camera.
Set `PHOTO_BOOTH_PHASE=panel` to verify the visual option rows and filter navigation.
The panel test checks visible icons, touch targets, selection state, Back, and the fixed shutter control.
Set `PHOTO_BOOTH_PHASE=headroom` to verify space above a close head and variable crops within a photo strip.

The test uses the real Home activity, camera, photo store, and Android interface.
It checks live filter choices and mirror symmetry in saved pictures and strips.
Focused image tests also use the bundled YuNet face detector with a fixed face image.
These tests check Bunny ears, Googly eyes, and Party hat pixels.

Controlled camera images also enter the real Photo Booth preview interface.
The test uses native face detection and dispatches touches through the mirrored view.
The only visible person must start person tracking without a setup command.
Several persons must produce a full preview with rectangles for touch selection.
The test checks taps inside and outside those rectangles through the mirrored Android view.
It also checks tracking loss, countdown cancellation, and automatic detection after the person returns.

Zoom changes must preserve the selected person at 1.5×, 2×, and 3×.
A covered face must preserve person tracking through optical flow for 1.5 seconds.
Removal of the person must stop person tracking, including scenes with a textured background.
A brief gap without measurements must preserve selection and prevent capture until a measurement returns.
All four zoom choices must produce the expected preview dimensions and crop pixels.
Focused tests check an ambiguous match, stale measurements, image-edge bounds, and independent crops in a four-picture strip.

Set `PHOTO_BOOTH_PHASE=framing` to run the focused automatic tracking and zoom checks.

The `head_turn` phase uses a private Portal screenshot as a controlled camera input.
Set `PHOTO_BOOTH_PHASE=head_turn` and `PHOTO_BOOTH_HEAD_TURN_FRAME` to that screenshot before the same Make target.
This phase checks automatic person tracking in the observed head-turn scene.
Keep this private image under an ignored build directory.

The real camera flow then verifies captures and saved images at 2× zoom.

The `assets/face.gif` fixture is a NASA portrait of Eileen Collins.
The image source is the [NASA StarChild page](https://starchild.gsfc.nasa.gov/docs/StarChild/whos_who_level2/collins.html).
The fixture is test data. It is not part of the application APK.

Run `make test-android-photo-effects` to verify the same native image effects on a physical Portal.
That target installs a separate qualification application and removes it after the test.
It does not change FamilyHome photos or settings.
