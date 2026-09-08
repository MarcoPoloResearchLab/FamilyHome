# ISSUES ARCHIVE

Completed non-recurring entries moved from the active tracker on 2026-09-07.
Historical statements describe the recorded result at that time.
I008 retains unresolved physical checks from B002, F002, I003, and I004.
I005, F001, F008, and F010 retain their separate acceptance work.

## BugFixes

- [x] [B008] (P2) Return to Games from the Blocks toolbar Back control.
  Goal:
  The gameplay toolbar Back control must return to the FamilyHome Games screen.
  The control currently invokes the gameplay gesture handler, which can open Settings or hold a piece.
  Requirements:
  - Give the gameplay toolbar an explicit return action.
  - Preserve the game task when the user returns to Games.
  - Keep Settings Back connected to gameplay and Home connected to FamilyHome Home.
  - Preserve the configured gameplay gestures.
  - Verify public navigation at normal and enlarged text dimensions.
  Validation:
  The initial navigation test failed because gameplay Back opened Settings.
  The three Blocks toolbar tests passed at font scales of 1.0 and 1.3 after the correction.
  Screenshot review confirmed visible controls and text at both dimensions.
  `make build-blocks` and `make ci` passed.
  The signed update was installed on the physical Portal without removal of application data.
  Physical checks confirmed Back to Games, return to the same game task, Settings Back, and Home.
  The installed APK hash matches the built APK hash.

- [x] [B007] (P2) Preserve player colors in Kart selection controls.
  Goal:
  Each player must have a different color for the current selection.
  Requirements:
  - Give the five player selection borders different colors.
  - Use the same color for each player name control and its selection border.
  - Keep black outlines and transparent centers on the selection borders.
  Validation:
  The initial APK test failed because all five borders had only black outlines.
  `make build-kart` and `make test-kart-skin` passed after the correction.
  The generated assets have five different colors and matching name controls.
  The test verifies that each stretched border contains the player color.
  A session with multiple physical controllers remains unverified under I005.

- [x] [B006] (P2) Keep selected text visible in Kart.
  Goal:
  Text must stay visible when the player selects characters in a text field.
  Requirements:
  - Use a blue selection color with partial transparency.
  - Preserve text contrast when the game engine draws the selection over the characters.
  Validation:
  The initial APK tests failed at both text dimensions because the selection was opaque.
  The APK tests passed after the correction.
  `make test-kart-text-selection` passed through Games at font scales of 1.0 and 1.3.
  Screenshot review confirmed visible selected characters at both dimensions.
  I005 retains physical Portal acceptance.

- [x] [B005] Correct application window selection in the Android upgrade test.
  Goal:
  The upgrade test must read the FamilyHome window after each activity change.
  The `android-upgrade` check failed before PR #5 merged.
  Requirements:
  - Use one accessibility connection for the complete navigation test.
  - Establish the accessibility connection before the three-second window timeout starts.
  - Select the FamilyHome window with input focus from the interactive windows.
  - Reject a capture when another application has input focus.
  - Wait for activity input focus before each navigation action.
  - Activate controls through their accessibility labels.
  - Preserve the failed test step and the last UI capture.
  - Repeat the Settings and Home sequence five times.
  - Disable animations during the test and restore the previous settings afterward.
  - Require both CI checks on `main`, including for administrators.
  Validation:
  - Run `make test-android-upgrade` and `make ci`.
  - Verify both GitHub checks on the final PR commit.
  Current result:
  The new integration test failed because the old helper accepted the Android launcher window.
  The first correction still failed the API 28 check with an unavailable window.
  The test now uses one instrumentation process for all navigation actions.
  Navigation and saved-data checks remain in the public upgrade test.
  Both GitHub checks are required on `main` with strict branch protection.
  The language review covered B005.

- [x] [B004] (P1) Restore automatic weather updates on Home.
  Goal:
  The weather card must show current service data while Home stays open.
  Requirements:
  - Request weather data after the 15-minute cache period.
  - After a request failure, show an error and repeat the request after 30 seconds.
  - Remove the old temperature, condition, and clothing advice after the cache period.
  - Reject request results after Home closes or the location changes.
  Validation:
  - Verify cache expiry and automatic recovery through the Android Home screen.
  - Run `make test-android-weather` and `make ci`.
  Current result:
  The Portal showed a saved report from the previous day after startup requests failed.
  The live service returned current weather data.
  The integration test failed before the production change.
  The integration test passed after the production change.
  `make ci` passed.
  The signed update is installed on the Portal.
  The installed APK hash matches the update artifact.
  The Portal shows 68 degrees and rain for Manhattan Beach.
  The language review covered B004.

- [x] [B003] Correct Android upgrade test setup and selection.
  Goal:
  The upgrade test must identify the current Home controls and game cards.
  The GitHub check failed because the Android fullscreen introduction covered Home.
  The next check exposed an absent UI snapshot while Home updated its clock.
  Requirements:
  - Confirm the fullscreen introduction before the test starts.
  - Restore the previous Android setting after the test.
  - Select cards through `content-desc`.
  - Capture the active accessibility window without a UI idle requirement.
  - Fail the test when the capture fails.
  - Keep navigation and saved-data checks after APK replacement.
  Validation:
  - Run `make test-android-upgrade` and `make ci`.
  - Verify the GitHub check after the correction.
  Current result:
  The GitHub UI output identified the Android fullscreen introduction.
  The test uses an Android integration helper to capture the active window.
  The helper reports capture failures directly.
  The upgrade test passed locally with the capture helper.
  `make ci` passed.
  The language review covered B003.

- [x] [B002] Restore the screensaver timeout for text input and dialogs.
  Goal:
  User input resets the timeout. An open dialog does not prevent the screensaver.
  Current failures:
  Repeated keyboard text input does not reset the timeout.
  An open selection dialog stops the timeout until the dialog closes.
  Requirements:
  - Reset the timeout after user text input.
  - Track user input in FamilyHome dialogs.
  - Show the screensaver over an idle dialog.
  - Preserve unsaved dialog input when the screensaver starts.
  - Return to the same dialog after a wake touch.
  - Consume the wake touch before the dialog can use it.
  Validation:
  - Verify both failures through the Android integration tests before the correction.
  - Verify text input, dialog input, timeout, wake behavior, and unsaved input after the correction.
  - Run `make test-android-screensaver`, `make test-android-toolbar`, and `make ci`.
  Current result:
  Both integration tests failed before the production changes.
  Text input now resets the timeout through the Android text interface.
  FamilyHome dialogs now track touch, key, and text input.
  The screensaver covers the complete screen above the current dialog.
  Wake input returns to the same dialog without a selection change.
  The typing and dialog tests passed.
  The dialog test verified input preservation and consumption of the wake touch.
  Existing screensaver behavior and process restart tests passed.
  `make ci` and `make test-android-toolbar` passed.
  The language review covered B002 and the added dialog term.
  Physical Portal installation remains pending.

- [x] [B001] (P1) Restore piano sound.
  Goal:
  Music shows each note but produces no sound.
  Requirements:
  - Write note samples before the audio initialization check.
  - Show audio errors.
  Validation:
  - Verify audio output through keyboard input.
  - Verify repeated notes and both octaves.
  - Run `make ci`.
  The Android integration test failed before the correction.
  The test passed for all 24 keys and repeated notes after the correction.
  `make ci` passed.
  The signed update is installed on the Portal.
  The installed APK hash matches the signed update.
  Android reports active piano audio through the speaker output.
  The media volume changed from zero to 3 of 18.
  The user confirmed audible piano sound on the Portal.

## Improvements

- [x] [I007] Separate current weather from daily preparation advice.
  Goal:
  Show current conditions and the daily forecast as separate reports on Home.
  Requirements:
  - Use current conditions for current clothing recommendations.
  - Show the daily high, low, and precipitation chance under Today.
  - Recommend supplies for heat, precipitation, and cooler hours when applicable.
  - Show heat and precipitation recommendations together when both apply.
  - Preserve forecast uncertainty, attribution, automatic refresh, and unavailable states.
  - Verify normal and enlarged text through Home on an Android emulator.
  - Record physical Portal verification separately.
  Validation:
  The initial Home test failed because the Now report was absent.
  Eleven weather scenarios passed at font scales of 1.0 and 1.3.
  The checks cover separate reports, combined heat and precipitation advice, temperature changes, cache expiry, and automatic recovery.
  Expired daily advice disappears with unavailable weather data.
  The appearance suite and `make ci` passed.
  Physical Portal screenshots confirmed both reports with live service data at both font scales.
  The enlarged report scrolls to its recommendations and attribution.
  The installed APK matched the new build by SHA-256, and the existing profile remained available.

- [x] [I006] Apply the approved flat Home and Games design.
  Goal:
  Use large adjacent color surfaces with bold labels and character illustrations.
  Requirements:
  - Divide Games into four equal quadrants.
  - Divide the timer into four equal quadrants without a heading.
  - Join the five Home activity controls with shared black borders.
  - Use the complete screen without an outer outline, shadow, or margins.
  - Preserve navigation, timer controls, weather, and saved data.
  Validation:
  The initial public appearance test rejected the gaps between the Home activity controls.
  Public appearance checks passed at font scales of 1.0 and 1.3 on an Android 9 emulator.
  The checks cover shared borders, equal quadrants, game illustrations, pressed states, keyboard focus, and timer access.
  Running and paused timers remain accessible after the countdown dialog closes.
  Toolbar, weather refresh, Photo Booth, and APK upgrade checks passed.
  Screenshot review confirmed the large illustrations and bold text.
  The shared font keeps its weight when a text role changes.
  Toolbars permit the height required by enlarged titles.
  A subsequent appearance test rejected the outer frame and margins.
  Home and Games now reach all screen edges and keep their cream toolbar backgrounds.
  Normal and enlarged text checks passed without the outer frame.
  Physical Portal screenshots confirmed both layouts after an update that preserved application data.
  The installed APK matched the new build by SHA-256.

- [x] [I004] Apply shared text roles to FamilyHome and its game adapters.
  Goal:
  Make the approved appearance the required contract for all FamilyHome interfaces.
  Requirements:
  - Define shared text roles and minimum control dimensions.
  - Apply large bold text to native screens, dialogs, Match, Blocks, and Tiles.
  - Keep the Photo Booth main action visible below its scrollable options.
  - Require the appearance contract before future interface changes.
  - Verify normal and enlarged text through public application entry points.
  Validation:
  The initial native test rejected the previous font weight.
  Native appearance and game navigation passed at font scales of 1.0 and 1.3 on a dedicated 1280 x 800 emulator.
  Match board checks passed at all three tested screen dimensions.
  Photo Booth, toolbar, weather, screensaver, and upgrade checks passed.
  Screenshot review found a Blocks shadow defect. The added pixel check failed before the correction and passed after it.
  The Blocks settings tests passed. The final `make ci` passed.
  Root `AGENTS.md` requires `android/STYLING.md` before each interface change.
  I005 tracks the Kart engine interface.
  Physical Portal acceptance remains pending.

- [x] [I003] Apply the approved FamilyHome appearance.
  Goal:
  Use black outlines, bright colors, large activity controls, and character illustrations.
  Requirements:
  - Define shared colors, type, outlines, shadows, and control states.
  - Apply the appearance to Home, Drawing, and native application controls.
  - Preserve profiles, timers, saved drawings, and activity navigation.
  - Keep all five Home activity controls visible in one row.
  - Verify normal and enlarged text on the emulator.
  Validation:
  The initial application test failed because Home activity controls were only 92 dp high.
  The application passed at font scales of 1.0 and 1.3 on a dedicated 1280 x 800 emulator.
  Five activity controls, black outlines, text bounds, brush selection, a saved drawing stroke, and the timer action passed.
  Seven native screens passed the toolbar and navigation test.
  Seven weather outfits, cache expiry, and automatic refresh passed.
  The upgrade test preserved profiles, timers, saved drawings, and private application files.
  Photo Booth capture, save, album, photo strips, and persistence passed with an emulated front camera.
  Screensaver dialog and typing checks passed.
  `make ci`, the Governor check, and `git diff --check` passed.
  The language checker reported no mechanical errors in the changed documents.
  Native screenshots are in `android/build/tests/style`.
  Physical Portal installation and acceptance remain pending.
  See `android/STYLING.md` for the appearance and test procedure.

- [x] [I002] Combine navigation and application controls in one toolbar.
  Goal:
  Back, Home, and application controls share one row.
  Requirements:
  - Keep Back and Home visible beside the application controls.
  - Apply one top row to every app and game launched from FamilyHome.
  - Include game menus, active games, and child screens in the audit.
  - Keep each control within the visible toolbar.
  - Preserve navigation and saved drawings.
  Implementation:
  Home keeps its profile, clock, and Settings row.
  Games, Settings, Ask, Drawing, Music, Piano, and Guitar use one toolbar.
  Guitar chord controls share that row.
  Match combines navigation, its game menu, and game status.
  Blocks combines navigation, score, level, lines, and Settings.
  Tiles combines navigation and menu tabs. Its active game menu uses the same row.
  Kart already uses fullscreen controls without stacked headers.
  The audit covers their menus and active games, plus the Kart pause menu.
  Validation:
  Integration tests failed before the toolbar changes.
  Seven native screens passed toolbar, touch-target, Back, and Home checks.
  Piano and Guitar returned to Music through Back and to Home through Home.
  The upgrade test preserved profiles, drawings, timer settings, and private files.
  `make ci` passed for the final source.
  Blocks and Tiles passed navigation, menu, settings, and gameplay toolbar checks.
  Signed game artifacts and matching source archives are ready.
  See `docs/combined-toolbar-audit.md` for scope and validation details.
  The user approved installation and replacement of Match, Blocks, and Tiles.
  All four signed updates are installed on the physical Portal.
  Each installed APK hash matches its final artifact.
  Physical review found a 60 px top inset in Blocks after the system bar disappeared.
  The correction removes that inset from the Flutter layout.
  The corrected Blocks toolbar starts at the top of the screen.
  Native screens, game menus, gameplay, Back, and Home passed physical review.
  The FamilyHome child profile and home settings remain present.
  Match, Blocks, and Tiles data was reset as approved.
  The Portal is on Home.

- [x] [I001] (P1) Standardize HTTP health at `/healthz`.
  Goal:
  Make `/healthz` the canonical health endpoint for the FamilyHome HTTP
  origin. Use the endpoint for readiness without application requests.

  Requirements:
  - Keep unauthenticated `GET /healthz` as the only HTTP health operation.
  - Return `200` only when the service can serve its current application contract.
  - Return a non-success status when a required runtime dependency prevents service.
  - Send `Cache-Control: no-store` on every health response.
  - Keep the response free from credentials and internal state.
  - Do not call a paid provider or mutate application state during a probe.
  - Do not record a probe as application usage or an audit event.
  - Do not emit routine information-level request events for successful probes.
  - Keep failed probe evidence in container and deployment diagnostics.
  - Use `/healthz` for runtime capability health and public health checks.
  - Keep the selected manifest contract unchanged.

  Deliverables:
  - Update the endpoint, orchestration, manifest, documentation, and black-box tests as necessary.

  Validation:
  - Verify unauthenticated `GET /healthz` returns `200` and `Cache-Control: no-store`.
  - Verify a required dependency failure returns a non-success status without a provider call.
  - Verify successful probes create no routine request events.
  - Verify failed probes retain diagnostic evidence.
  - Run `make ci`.

  Completion review on 2026-09-07:
  The current router, health handler, service guide, and deployment manifest use `/healthz`.
  `TestHealthDoesNotRequireAuthentication` verifies the real HTTP entry point, storage failure, recovery, response headers, and diagnostic behavior.
  The handler reads local storage without a provider call or application mutation.
  `make test-service` passed during this review.
  This review confirmed source completion. It made no deployment or live-service acceptance claim.

## Features

- [x] [F003] (P1) Qualify the Photo Booth camera.
  Goal:
  FamilyHome shows camera preview and creates JPEG pictures through one Camera2 session.
  P004 defines the accepted product scope.
  Requirements:
  - Select supported camera preview and YUV dimensions from camera characteristics.
  - Encode each selected YUV frame as an upright JPEG picture.
  - Keep one YUV camera path for the Portal and emulator.
  - Release camera resources on exit, activity pause, and screensaver entry.
  - Reject callbacks from a camera session after cancellation.
  Validation:
  - Verify the real camera pipeline through the qualification application.
  - Verify the Photo Booth lifecycle through its Android interface.
  Current result:
  The Home test failed with `Missing control: Photo Booth. Take a picture` before application implementation.
  On 2026-09-06, physical JPEG capture returned 18,000,008 bytes without a JPEG header.
  The current adapter uses a continuous YUV stream and application JPEG encoding.
  The Portal passed 20 capture, release, and reopen cycles, plus pause during open and activity resume.
  Camera 0 supplies 1280 by 720 pixels for camera preview and YUV output.
  The saved numbered page has normal text direction. The camera preview reflects the scene horizontally.
  The Portal uses a fixed display rotation. The emulator separately passed the 180-degree rotation test.
  The capture reservation uses four bytes per source pixel plus 65,536 bytes for each picture.
  Four Portal pictures reserve 15,007,744 temporary bytes before capture.
  The source image limit remains 1600 pixels per side. The renderer checks its 64 MiB decoded-image limit before allocation.
  Physical camera evidence is in `android/build/tests/photobooth/physical-camera/`.
  References:
  - [Android YUV image format](https://developer.android.com/reference/android/graphics/ImageFormat).

- [x] [F004] (P1) {F003} Add Photo Booth capture and review.
  Requirements:
  - Add the embedded offline activity to Home through the common toolbar.
  - Use a three-second countdown before each picture.
  - Show Save, Retake, and Discard on the review screen.
  - Keep normal text direction in review and saved pictures.
  - Preserve completed review images across screensaver entry and wake.
  - Cancel incomplete capture on exit, pause, or screensaver entry.
  Validation:
  - Verify the capture flow, cancellation, and camera release through the Android interface.
  Current result:
  The Android integration test passed capture, Retake, Discard, countdown cancellation, and review preservation across screensaver wake.
  The physical Portal passed permission, preview, capture, review, Save, and Album through screen controls.

- [x] [F005] (P1) {F004} Add child photo albums.
  Requirements:
  - Save pictures with the profile ID selected at capture start.
  - Use one current metadata schema and complete each Save once.
  - Keep saved pictures after process restart and APK update.
  - Show the active child's photo album with image review and confirmed deletion.
  - Keep at most 100 entries or 256 MiB per child and 512 MiB across all photo albums.
  - Include images, thumbnails, and metadata in capacity calculations.
  - Stop capture when temporary data and the final result cannot fit.
  - Keep saved pictures until explicit deletion without automatic removal of older entries.
  - Keep pictures in application storage without export or upload.
  Validation:
  - Verify two profiles, duplicate Save input, deletion, restart, APK update, and storage failures through public entry points.
  Current result:
  The integration test passed separate albums, repeated Save input, confirmed deletion, process restart, and an actual APK update.
  Real filesystem conditions verified the entry limit, byte limit, and explicit storage errors.
  Existing profiles and drawings also passed `make test-android-upgrade`.

- [x] [F006] (P1) {F005} Add photo strips and decorative frames.
  Requirements:
  - Add four-picture vertical photo strips with a countdown before each picture.
  - Add None, Stars, and Confetti frame choices.
  - Save only the combined strip and remove temporary individual pictures.
  - Use JPEG quality 90 with at most 1600 pixels on the longest side of a single picture.
  - Use at most 1200 by 3600 pixels for a photo strip without enlargement of source pictures.
  - Keep decoded renderer images within 64 MiB.
  - Select the temporary storage bound from qualified camera dimensions.
  Validation:
  - Verify four distinct pictures, frame rendering, crop, orientation, memory use, and cancellation through the Android interface.
  Current result:
  The integration test passed a four-picture sequence, Stars and Confetti frames, saved image dimensions, and temporary-data removal.
  The renderer decodes one source picture at a time and checks the 64 MiB image bound before allocation.
  Emulator screenshots show the complete strip and its frame.
  The physical Portal produced a 1200 by 2712 pixel Stars strip with four distinct pictures.
  A process memory sample recorded 54,120 KiB total PSS during the sequence.
  This sample is not a peak measurement. The renderer image bound remains a separate allocation check.

- [x] [F007] (P1) {F006} Verify Photo Booth on the physical Portal.
  Requirements:
  - Complete the P004 physical camera procedure and full application checks.
  - Verify the layout at 1280 by 800.
  - Verify camera cover behavior and screensaver transitions.
  - Record physical acceptance separately from emulator results.
  Validation:
  - Run the Photo Booth, toolbar, screensaver, and upgrade tests.
  - Run `make ci` after the final implementation change.
  Current result:
  On 2026-09-06, Photo Booth passed physical preview, countdown, capture, review, Save, and Album at 1280 by 800 pixels.
  The Portal passed a four-picture Stars strip and kept the earlier picture after the final APK update.
  A closed camera cover produced an obscured image without an Android camera error.
  Capture and Discard completed with the cover closed. Camera preview returned after the cover opened.
  A timed screensaver preserved the completed strip. The first wake touch activated no Save control.
  The original five-minute Clock timeout was restored after the checks.
  Home and screensaver entry released the camera. The system camera service confirmed that FamilyHome had no active camera client.
  The installed APK and local artifact have SHA-256 `8c192911f9e7d26a3806e250c78e5fe589c8387171c20ba93473df38cfe6acf3`.
  Photo Booth, toolbar, screensaver, upgrade, camera recovery, and final `make ci` checks passed.
  Physical screenshots and the memory sample remain in the ignored camera evidence directory recorded by F003.

- [x] [F002] Add a screensaver with a timeout in Settings.
  Goal:
  The user can select a screensaver, including a black screen, and its timeout.
  Requirements:
  - Add the screensaver controls to Settings.
  - Save the selection on this Portal.
  - Start the screensaver after the selected timeout.
  Implementation:
  Settings offers `Clock`, `Black screen`, and `Disabled`.
  The timeout choices are 30 seconds and 1, 2, 5, 10, 15, or 30 minutes.
  The default is `Black screen` after five minutes.
  `Preview screensaver` shows the selected display immediately.
  A touch or key returns to the same screen without activation of its controls.
  The screensaver shows an analog clock above the digital time and full date.
  Settings offers a `Time format` control with `12-hour` and `24-hour` choices.
  FamilyHome saves the initial Android format as its own selection.
  Later Android format changes do not change the saved FamilyHome selection.
  Home, calendar event times, and the screensaver use this selection.
  The analog clock has a light purple face and rounded hour and minute indicators.
  A purple dot shows the seconds. A short date appears along the face edge.
  The time and date move continuously across the screen. They change direction at each screen edge.
  If Android animations are disabled, the time and date stay at the center of the screen.
  The black screen uses minimum brightness. The display remains powered.
  The controls apply to FamilyHome screens. Separate game applications control their own display behavior.
  Validation:
  The Android integration test first failed with `Missing control: Screensaver mode`.
  The contrast test failed before correction of the selector text.
  The keyboard test failed before the screensaver closed the keyboard.
  The date test first failed with `Screensaver date absent`.
  The analog face test first failed with `Scalloped analog clock face absent`.
  The date width test exposed clipped text before the layout correction.
  The time format test first failed with `Missing control: Time format`.
  The emulator tests verified initialization and both formats on Home and the screensaver.
  The saved selection survived process restarts and Android format changes.
  The emulator test verified the analog face, indicators, and full date width.
  The emulator test verified continuous movement and direction changes at all four screen edges.
  A separate emulator test verified the stationary date and time with Android animations disabled.
  `make test-android-screensaver` passed, including a process restart and keyboard removal.
  The test verified timeout changes, touch input, brightness, previews, disabled operation, and activity changes.
  `make ci` and `make test-android-toolbar` passed.
  The layout review passed at 1280 by 800.
  The language review covered F002 and the two added display terms.
  Physical Portal installation and acceptance remain pending.

## Planning

- [x] [P005] (P1) Plan completion of Ask with the Go LLM Proxy client.
  Goal:
  Children can submit typed or spoken questions and receive readable answers with optional spoken playback.
  The FamilyHome Go backend uses the official LLM Proxy client and selects its model through a configuration file.
  The user confirmed the Go client and configuration-based model selection on 2026-09-08.

  This issue records the technical implementation plan. The user authorized implementation on 2026-09-08.
  The accepted plan is completed. I009 owns the remaining implementation acceptance.
  The sections below preserve the approved proposal and its source review.

  Requirements:
  - Use `github.com/tyemirov/llm-proxy/pkg/llmproxyclient` in the FamilyHome Go backend.
  - Select the provider, model, reasoning effort, and request work budget through backend configuration.
  - Keep the LLM Proxy secret and provider credentials off the Portal.
  - Keep typed questions, voice questions, visible answers, and Android text-to-speech in the acceptance scope.
  - Preserve child profiles, saved drawings, photo albums, and other local activities.
  - Keep one current configuration and request contract during each coordinated change.
  - Separate local integration results, actual provider results, backend deployment, and physical Portal acceptance.

  Current source evidence:
  The source review date is 2026-09-08.
  `AskActivity` sends text to `POST /v1/ask` and recorded audio to `POST /v1/ask/audio`.
  Both operations return an `answer` field. Android displays that answer and requests spoken playback.
  `service/main.go` constructs one official client at startup and uses `NewMessagesRequest` and `PostMessages`.
  `service/go.mod` currently resolves the LLM Proxy module to v1.2.1.
  Existing service tests verify text requests and encoded audio attachments against a local HTTP server.

  These tests do not prove that a real model understands the recording or that the Portal produces audible answers.
  The backend reads all settings from environment variables. It has no canonical `config.yml`.
  The deployment manifest selects `openai`, `gpt-5-mini`, `low` reasoning effort, and a 45-second request work budget.
  Those declarations do not prove current provider availability or audio support.
  Android uses a 60-second read timeout. The Go HTTP server also uses a 60-second write timeout.
  The current busy indicator does not prevent repeated submissions.

  The activity does not cancel requests or remove temporary recordings after each terminal state.
  The initial work must address these gaps through the existing Ask flow.

  Ownership and dependencies:
  FamilyHome owns question validation, child-facing instructions, interaction state, recording cleanup, and answer presentation.
  LLM Proxy owns its request protocol, tenant authentication, provider execution, and upstream credentials.
  Android calls FamilyHome. It does not call LLM Proxy directly or select arbitrary providers and models.

  P002 owns future parent authentication, individual device enrollment, family authorization, retention policy, and family usage limits.
  This plan can complete without P002 because its initial scope uses the existing authorized Portal installation.
  The shared installation credential does not establish child identity or family isolation.
  A hosted rollout to additional families requires the applicable P002 implementation and acceptance, not only closure of its planning issue.
  P001 calendar work and P003 image generation do not prevent this Ask work.
  I008 retains the broad appearance audit. This issue defines functional Ask acceptance at both text dimensions.

  Proposed configuration contract:
  Establish `service/config.yml` as the single canonical backend configuration file during implementation.
  Move the existing server settings and LLM Proxy settings into that file together.
  Use `--config <path>` as the required startup input for its location.
  Resolve the file once at startup and reject a missing or unreadable file.
  Changing the selected model requires a backend restart. It requires no Android rebuild.
  Automatic configuration reload, model selection on the Portal, and per-question routing remain outside this initial plan.

  This example defines the proposed shape. The model and reasoning values are placeholders, not selected production values.
  The 45-second budget preserves the current deployment value until a different value is selected.
  ```yaml
  server:
    listen_address: "0.0.0.0:8765"
    data_dir: "/data"
    device_token: "${FAMILYHOME_DEVICE_TOKEN}"
  llm_proxy:
    base_url: "https://llm-proxy-api.mprlab.com"
    secret: "${LLM_PROXY_SECRET}"
    provider: "openai"
    model: "<selected-model-id>"
    reasoning_effort: "<supported-reasoning-effort>"
    request_timeout_seconds: 45
  ```

  - Parse YAML with one typed schema and reject unknown fields, duplicate keys, and multiple documents.
  - Expand only explicit environment references in the YAML values.
  - Require every listed field and reject unresolved placeholders or environment references after expansion.
  - Keep secret values in the existing private deployment inputs.
  - Keep model, provider, reasoning, address, and timeout values out of application constants and independent environment overrides.
  - Require an explicit provider and model for this initial scope.
  - Validate the selected reasoning value through the released client contract.
  - Require a positive integer for `request_timeout_seconds`.
  - Fail startup before the HTTP listener opens when configuration or official-client construction fails.
  - Remove the former direct environment lookups after the coordinated configuration change.
  - Copy the canonical YAML into the service image and pass its path through the container command.
  - Update the deployment manifest, local commands, service guide, and startup tests together.
  The manifest supplies secrets and the configuration path. It does not duplicate the model policy in environment values.
  Local integration tests supply a complete temporary YAML file through the same startup input.

  Proposed Go client implementation:
  - Resolve the released client with `go get github.com/tyemirov/llm-proxy/pkg/llmproxyclient@latest` during implementation.
  - Review the resolved client contract before changes to the adapter or dependency metadata.
  - Construct `NewConfig` and `NewClient` once during backend startup.
  - Inject the client into the existing application dependency graph.
  - Construct requests with `NewMessagesRequest` and execute them with `PostMessages`.
  - Pass configured `Model`, `ReasoningEffort`, and `RequestTimeoutSeconds` into each applicable request.
  - Use `NewAudioAttachment` for voice input when the released client supports that operation.
  - Keep request construction within one app-owned Ask adapter.
  - Keep the same selected answer model for text and voice requests.
  - Report unsupported audio input explicitly without an automatic model change or an alternative provider request.
  - Verify actual audio support before acceptance of voice questions.
  - Propagate cancellation through the Go context and official client.
  - Derive transport and server deadlines from the configured budget plus an explicit allowance for request and response transfer.
  - Supply the corresponding Android deadline through the application contract before increasing the backend budget beyond its current limit.
  - Reject empty answers and preserve structured upstream error categories without raw provider payloads.

  Proposed question contract:
  Keep the existing typed and audio Ask operations for the current installation scope.
  P002 owns their later replacement with family-owned question resources in one coordinated client and backend change.
  This work adds no generic public endpoint for arbitrary LLM requests.
  - Authenticate every Ask request through the current FamilyHome request boundary.
  - Validate content type, required fields, question length, audio size, and supported audio format before provider execution.
  - Reject unknown input fields and client-supplied provider, model, reasoning, or budget values.
  - Convert validated inputs into closed text-question or audio-question domain values.
  - Keep child names and question text separate from server-owned system instructions.
  - Treat the supplied profile ID and name as untrusted personalization data, not authorization.
  - Preserve the documented success response and define stable errors for invalid input, unavailable service, unsupported audio, and timeout.
  - Keep questions, recordings, answers, credentials, and raw provider errors out of routine logs.
  - Preserve explicit errors without automatic provider retries.
  - Report an interrupted dispatched request as an unknown outcome when completion cannot be established.
  - Never claim that cancellation proves the provider stopped work or incurred no charge.
  Persistent duplicate prevention, request receipts, and family accounting use the later P002 contract.
  This work prevents repeated UI submissions but does not claim exactly-once provider execution across restarts or lost responses.

  Proposed Android implementation:
  - Separate the FamilyHome backend client, microphone adapter, and speech adapter from `AskActivity` rendering.
  - Use explicit `idle`, `recording`, `submitting`, `answer`, `error`, and `cancelled` interaction states.
  - Permit at most one recording or submitted request at a time.
  - Disable both submit actions while a request is active.
  - Capture the selected profile and a request identity before work starts.
  - Reject callbacks after cancellation, activity exit, profile change, or replacement by a newer request.
  - Preserve the question draft after validation or connection errors.
  - Keep each first-version question independent, without persistent conversation history.
  - Bound recording duration and file size before upload.
  - Show explicit microphone denial, unavailable microphone, empty recording, and upload failure states.
  - Stop recording and spoken playback on Back, Home, activity pause, and screensaver entry.
  - Cancel active network work on those transitions.
  - Remove temporary recordings after completion, failure, or cancellation.
  - Remove abandoned recording files after process restart.
  - Validate the response before display or spoken playback.
  - Keep the answer readable when speech initialization or playback fails.
  - Provide a visible control to stop spoken playback.
  - Apply `android/STYLING.md` at normal text dimensions and the 1.3 font scale.

  Implementation stages after approval:
  | Stage | Scope | Exit evidence |
  | --- | --- | --- |
  | 1 | Canonical YAML, strict startup, and released Go client | Two configured models produce the corresponding outbound requests without code changes |
  | 2 | Typed and audio boundaries, deadlines, cancellation, and errors | Real FamilyHome HTTP requests reach a local LLM Proxy test server through the official client |
  | 3 | Android interaction, recording cleanup, and speech lifecycle | Public UI scenarios pass without duplicate submissions, stale answers, or audio after exit |
  | 4 | Configuration packaging and regression coverage | The service image contains the selected YAML and existing FamilyHome behavior passes its checks |
  | 5 | Authorized provider and physical acceptance | Typed and recorded questions produce correct visible and audible answers on the Portal |
  Each behavior change starts with a failing integration test before production-code changes.

  Deliverables:
  - Record the selected values for the open decisions below before the corresponding implementation stage.
  - Define the YAML schema, startup command, official-client adapter, and deployment input transition.
  - Define the text and audio validation rules and their documented HTTP errors.
  - Define request cancellation, recording cleanup, and speech lifecycle behavior through public integration scenarios.
  - Add a repository target such as `make test-android-ask` for complete Ask interface coverage during implementation.
  - Update the service and Android guides with configuration, model selection, capability limits, and qualification commands.
  - Create bounded implementation issues for the approved stages without treating this plan as completed application work.

  Open Decisions:
  - Select the first provider and model and verify text, audio, reasoning, and account support through the released contracts.
  - Confirm spoken input and playback defaults, supported languages, and the behavior when the selected model lacks audio support.
  - Select the request work budget, transfer allowance, and Android deadline configuration mechanism.
  - Select the question limit, recording duration, upload limit, and initial installation concurrency limit.
  - Define child age assumptions, answer length, parent controls, and the handling of unsuitable requests before child-facing acceptance.
  - Define provider data controls and the treatment of temporary questions and answers for the initial installation.
  - Define live-provider qualification cases and their request budget.
  These decisions do not reopen the confirmed Go client or configuration-based model selection.

  Validation:
  This planning change uses source and document review. It makes no provider request or application change.
  - Verify that the new issue ID is absent from both the active tracker and archive before allocation.
  - Verify the Go API names and source statements against the application and released client during implementation.
  - Run the language checker, Governor check, issue-reference checks, and `git diff --check` for this plan.
  - Keep existing unrelated guide drift separate from new findings.

  Acceptance scenarios for later implementation:
  - Start the actual service with valid YAML and verify its authenticated HTTP entry points.
  - Reject missing files, malformed YAML, duplicate keys, unknown keys, missing secrets, and invalid budgets before listener startup.
  - Change the configured model, restart the backend, and verify the new model at the official HTTP boundary.
  - Verify provider, model, reasoning, authentication, and `X-LLM-Proxy-Request-Timeout-Seconds` without disclosure of secrets.
  - Verify invalid input and unauthorized requests produce no provider call.
  - Verify a real audio fixture reaches the official client with its declared format and unchanged bytes.
  - Verify provider errors, empty answers, cancellation, and deadlines through a controlled HTTP server.
  - Submit rapidly through the Android interface and verify one active request.
  - Verify late responses do not appear or speak after navigation, profile changes, or cancellation.
  - Verify recording cleanup after success, failure, cancellation, screensaver entry, and process restart.
  - Verify microphone denial, speech failure, readable answers, and the stop-playback control.
  - Run `make test-service`, the new Ask interface target, and the affected appearance, toolbar, screensaver, and upgrade targets.
  - Run `make ci` after the final implementation change.
  - Qualify typed input, actual voice understanding, and audible answers with the selected provider on the physical Portal.
  - Record source revision, configuration identity without secrets, APK identity, device, and results at both font scales.
  - Keep hosted multi-family acceptance under the applicable P002 implementation work.

  References:
  - `service/main.go`, `service/main_test.go`, `service/go.mod`: current server, client construction, and boundary tests.
  - `service/README.md`, `.mprlab/deploy/resources.yml`, `Dockerfile`: current configuration and deployment contract.
  - `android/app/src/main/java/com/mprlab/portal/AskActivity.java`: current input, recording, HTTP, and speech behavior.
  - `android/app/src/main/java/com/mprlab/portal/PortalActivity.java`: shared navigation and screensaver lifecycle.
  - `android/STYLING.md`, `Makefile`: appearance and validation contracts.


- [x] [P004] Plan the Photo Booth application.
  Goal:
  This issue defines an implementation proposal for Photo Booth in FamilyHome.
  The user accepted the proposed defaults and requested implementation on 2026-09-05.
  F003 through F007 contain the implementation work.

  Requirements:
  - Plan the Photo Booth application.
  - Use the current FamilyHome contracts.
  - Keep proposed product choices separate from confirmed requirements.

  Confirmed scope:
  The user selected this scope on 2026-09-05.
  - Add Photo Booth as an activity within FamilyHome.
  - Keep Photo Booth available without a service connection.
  - Include single pictures, four-picture photo strips, decorative frames, and child photo albums.
  The subsequent implementation request also accepted the detailed product defaults below.

  Current evidence:
  The source review date is 2026-09-05.
  FamilyHome uses native Java and a direct Android SDK build.
  The manifest specifies Android API 28 as its minimum and target.
  The manifest has no camera permission or Photo Booth activity.
  Home currently shows Draw, Ask, Music, and Games.
  `ProfileStore` supplies local child identities. Profile selection does not authenticate a child.
  `PortalActivity` shows the screensaver as an overlay without an activity pause.
  Physical Portal camera access remains unverified.

  First-version flow:
  The first version works on the Portal without a service connection.
  A Photo Booth control on Home opens one native FamilyHome activity.
  Back, Home, and application controls use the common toolbar.
  The camera preview occupies the main screen area.
  The child selects one picture or a four-picture photo strip.
  A three-second countdown precedes each picture.
  The review screen offers Save, Retake, and Discard.
  A small selection of decorative frames provides the first effects.
  Save adds the result to the active child's photo album.
  The photo album provides picture review and explicit deletion.
  The activity, offline operation, picture types, decorative frames, and child photo albums are confirmed scope.
  The countdown, review controls, and detailed image policy are accepted defaults.

  Proposed image policy:
  The user accepted these design values for implementation. Physical measurements must still verify the technical limits.
  | Choice | Proposed value | Reason |
  | --- | --- | --- |
  | Countdown | Three seconds before each picture | Time for the child to prepare |
  | Decorative frames | None, Stars, and Confetti | A small initial selection |
  | Reflection | Mirror camera preview. Normal text direction in review and saved images | Camera preview like a mirror and readable saved text |
  | Single picture | At most 1600 pixels on the longest side, without enlargement | Bounded image size |
  | Photo strip | Four pictures in one vertical image, at most 1200 by 3600 pixels | One saved result for each sequence |
  | JPEG quality | 90 | Initial image quality for device review |
  | Decoded images | At most 64 MiB for the renderer | Bounded memory for image assembly |
  | Photo album capacity | At most 100 entries or 256 MiB per child, whichever occurs first | Bounded local storage |
  | Application photo capacity | At most 512 MiB across all photo albums | Bounded storage across child profiles |
  | Storage duration | Until explicit deletion | No automatic deletion of saved pictures |
  | Photo strip originals | Remove temporary individual pictures after Save or Discard | One photo album entry for each strip |
  | Deletion | One selected entry after a confirmation dialog | Protection against accidental input |
  | Export | No export or upload operation in the first version | Offline scope without recipient access |
  Capacity calculations include saved images, thumbnails, and metadata.
  Temporary image storage has a separate bound, selected from the qualified camera dimensions.
  Before a sequence starts, the photo store checks capacity for temporary data and the final result.
  Insufficient capacity stops the sequence with an explicit error. The application does not remove existing pictures to create space.
  The camera qualification must verify the proposed dimensions, image quality, memory limit, and temporary storage bound.
  A failed qualification requires a revised design value before implementation continues.

  Child photo albums organize pictures by profile ID. The child selector does not provide secrecy between children on the Portal.
  A profile name change keeps the same photo album.
  Future profile removal must include explicit confirmation for removal of its photo album.
  Future family transfer must complete the local data-removal procedure defined with P002 before another family uses the Portal.
  These future operations must include photos in their data policy. They are not additional first-version features.

  Proposed technical design:
  `PhotoBoothActivity` owns the screen and user commands.
  A camera adapter owns camera access, camera preview, image output, and resource release.
  Camera2 is the initial candidate because the current build uses platform APIs without AndroidX dependencies.
  The qualified Camera2 session supplies camera preview and YUV output together.
  The application encodes YUV images as JPEG pictures.
  Android documentation recommends CameraX for general camera applications.
  Device qualification must establish this choice before the remaining implementation stages.
  The adapter selects supported dimensions from the device characteristics.
  Image orientation and camera preview reflection require explicit handling.
  The proposed camera preview reflects the user like a mirror. Saved images preserve normal text direction.
  A renderer combines pictures and decorative frames within a defined memory limit.
  A photo store owns image files, thumbnails, and one current metadata schema.
  Proposed metadata contains `id`, `profile_id`, `created_at`, `kind`, `frame_id`, `width`, `height`, and `bytes`.
  A capture sequence keeps the profile ID selected at its start.
  Save completes the image and metadata together before the interface reports success.
  Repeated Save input produces one photo album entry.
  Temporary pictures remain separate from saved entries. Cancellation and process restart remove incomplete temporary data.
  Final images remain in application storage. The proposed first version has no photo upload operation.

  Proposed state and resource behavior:
  The main states are `permission-required`, `opening`, `preview`, `countdown`, `capturing`, `review`, `saving`, `album`, and `error`.
  Camera denial, camera loss, and insufficient storage produce explicit errors with applicable next actions.
  The camera adapter releases its resources on Back, Home, activity pause, and screensaver entry.
  Screensaver entry cancels an incomplete capture sequence.
  After an interrupted countdown or capture, screensaver wake returns to camera preview after resource acquisition.
  Screensaver entry keeps a completed review image and the current photo album screen. Wake returns to that screen.
  A Save already in progress completes once while the screensaver is visible.
  The wake touch takes no picture and activates no control.
  A screensaver callback in `PortalActivity` is necessary because the current overlay does not pause the activity.
  Pending camera callbacks must not save a discarded picture or restart the camera after exit.

  Proposed physical camera qualification procedure:
  Camera access and output checks belong to stage 1. The full application checks finish in stage 5.
  No physical camera result is recorded for this planning task.
  1. Record the Portal model, Android version, APK identity, camera IDs, and camera characteristics.
  2. Request camera access through the Photo Booth interface.
  3. Verify explicit interface states for permission denial and unavailable camera access.
  4. Select one camera and supported camera preview and YUV dimensions.
  5. Verify camera preview and YUV output in the same camera session.
     Verify that the application creates valid JPEG pictures from YUV output.
  6. Photograph a numbered chart with text through the countdown and capture controls.
  7. Verify crop, orientation, text direction, and correspondence between camera preview and saved image.
  8. Complete 20 open, capture, exit, and reopen cycles through the Android interface.
  9. Verify camera release through Back, Home, activity pause, and screensaver entry.
  10. Close and open the physical camera cover during camera preview and countdown.
  11. Record cover behavior and verify that the interface reports any camera error.
  12. Measure image memory and temporary storage during a four-picture sequence at the proposed dimensions.
  13. Record the selected camera configuration, measurements, and acceptance result.
  Camera cover behavior requires observation. The plan does not assume that a closed cover produces an Android error.
  If camera access or the required output combination fails, stop stage 1 with the native diagnostic.
  The plan must then select one supported camera contract before implementation continues.

  Proposed implementation sequence:
  | Stage | Scope | Proposed exit evidence |
  | --- | --- | --- |
  | 1 | Portal camera qualification | Camera permission, supported dimensions, preview, JPEG output, orientation, and repeated release pass on the device |
  | 2 | One complete picture flow | Home, countdown, picture review, Save, Retake, Discard, and resource release pass through the Android interface |
  | 3 | Photo album and persistence | Two profiles retain separate entries after restart and APK update, with correct deletion and storage errors |
  | 4 | Photo strips and decorative frames | Four distinct pictures appear in sequence, with correct crop, orientation, and complete cancellation |
  | 5 | Full device acceptance | Repeated sessions, screensaver transitions, camera cover behavior, and the Portal layout pass on the physical device |
  Each stage starts with a failing integration test through the real application entry point before production-code changes.
  A proposed `make test-android-photobooth` target exercises the Android interface and real camera pipeline with an emulator camera scene.
  Camera failure scenarios require controlled emulator or device conditions at the platform boundary.
  Existing toolbar, screensaver, and upgrade tests cover the affected shared behavior.
  `make ci` runs after the final application change. Physical camera acceptance remains a separate result.

  Implementation decision on 2026-09-06:
  The physical Portal supplies camera preview and YUV images at 1280 by 720 pixels through Camera2.
  Native JPEG output failed. The current contract uses continuous YUV output and JPEG encoding in the application.
  The same adapter serves the Portal and emulator.
  F003 records the source dimensions and temporary storage bound.
  Full application acceptance belongs to F007.
  The selected offline scope has no AI transformation or provider integration dependency.
  P002 applies to future family transfer. P003 applies only if a later request adds AI transformations.
  F003 through F007 record the implementation sequence for the accepted product values.

  Deliverables:
  - Record the selected first-version flow and image policy.
  - Record the physical camera qualification procedure and results during implementation.
  - Create concrete implementation issues for the selected stages after the product decisions.

  Validation:
  - Verify the source facts against the named Android files.
  - Verify that product assumptions remain proposals or open decisions.
  - Verify the distinction between emulator coverage and physical Portal acceptance.
  - Run the document language checker and Governor check.
  - Verify issue identifiers and `git diff --check`.

  Current planning result:
  The source review confirmed the Android build, Home controls, profile IDs, and screensaver behavior.
  The user selected the offline activity scope and accepted the detailed image policy for implementation.
  The language checker, Governor check, issue identifier check, and `git diff --check` passed.
  The language review covered the P004 changes against the official Issue 9 reference.
  Physical camera qualification and application acceptance remain pending.

  References:
  - `android/app/src/main/AndroidManifest.xml`, `android/build.sh`: Android runtime and build contracts.
  - `android/app/src/main/java/com/mprlab/portal/MainActivity.java`: Home controls.
  - `android/app/src/main/java/com/mprlab/portal/ProfileStore.java`: child identities.
  - `android/app/src/main/java/com/mprlab/portal/PortalActivity.java`: screensaver and navigation behavior.
  - [Android camera sessions](https://developer.android.com/media/camera/camera2/capture-sessions-requests).
  - [Android runtime permissions](https://developer.android.com/training/permissions/requesting).
