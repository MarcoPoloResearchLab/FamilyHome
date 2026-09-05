# Combined toolbar audit

Issue I002 applies to every application and game opened from FamilyHome.
The audit uses a 1280 by 800 screen with density 160.

| Screen or game | Result | Validation |
| --- | --- | --- |
| Home | One profile, clock, and Settings row. | Physical Portal review. |
| Games, Settings, Ask, Drawing, Music | One 72 dp toolbar with Back and Home. | Native screen integration test. |
| Piano | Navigation, title, and note status share one 72 dp row. | Native screen integration test. |
| Guitar | Navigation, title, chord controls, and note status share one 72 dp row. | Native screen integration test. |
| Match | Navigation, game menu, and game status share one 64 dp row. | Signed APK integration test. |
| Blocks | Navigation, score, level, lines, and Settings share one 64 dp row. | Signed APK integration test. |
| Tiles | Navigation and menu tabs share one 64 dp row. The active game menu uses the same row. | Signed APK integration test and web view review. |
| Freedoom | Fullscreen game controls. No stacked headers. | Physical Portal menu and gameplay review. |
| Kart | Fullscreen game controls. Setup screens have one game header. | Physical Portal menu, setup, gameplay, and pause review. |

The native test covers all seven child screens.
Back returns Piano and Guitar to Music. Home opens the main Home screen.
The test checks navigation touch targets and clipping.
The upgrade test checks profiles, drawings, timer settings, and private files.

The Match test covers the main menu, active game, Help, Back confirmation, and Home.
The Blocks test covers the score row, Settings, Back, and Home.
The Tiles test covers all six menu tabs, active gameplay, the game menu, Back, and Home.
Blocks also uses the shared toolbar in its multiplayer screens.
A two-device multiplayer session is outside this toolbar validation.

## Build artifacts

The signed artifacts are local build outputs:

- `android/build/toolbar-preview/FamilyHome-combined-toolbar.apk`
- `android/build/match-portal/Match-Portal.apk`
- `android/build/blocks-portal/Blocks-Portal.apk`
- `android/build/tiles-portal/Tiles-Portal.apk`

Each game output directory contains its matching source archive.
The build recipes verify pinned upstream source hashes.
The Blocks recipe uses Flutter 3.44.0. The Tiles recipe uses the upstream npm package lock.

Screen captures are under `android/build/tests/toolbar/`, `android/build/toolbar-audit/`, and `android/build/match-portal/`.

## Installation status

The user approved the replacements and their game data resets.
All four updates are installed on Portal `819PGF02P011MG16`.
Each installed APK hash matches its final artifact.
FamilyHome kept its child profile and home settings.
Match, Blocks, and Tiles data was reset as approved.

Physical review found a 60 px top inset in Blocks after Android hid its system bar.
The Flutter layout now removes that unused top inset.
The corrected toolbar starts at the top of the screen.
The physical checks cover Blocks gameplay, Settings, Back, and Home.

The device checks also cover Match gameplay and its Back confirmation, Tiles settings and gameplay, and native application navigation.
The native checks include Guitar chord controls and the Piano return to Music.
Android UI automation did not return an accessibility tree on this device during acceptance.
Physical acceptance uses screenshots, touch input, and resumed-activity checks.
Emulator integration tests remain separate evidence.

Installed hashes are in `android/build/toolbar-preview/portal-installation.json`.
Physical screenshots are in `android/build/toolbar-preview/portal/`.
The Portal is on Home.

Freedoom and Kart keep their existing fullscreen layouts.
They have no duplicate toolbar row to remove.
