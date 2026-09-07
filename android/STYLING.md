# FamilyHome appearance contract

This document defines the required appearance for every FamilyHome interface.
It applies to existing screens, new screens, games, dialogs, menus, and future platform clients.
I003 introduced the appearance. I004 applies shared text roles and larger controls.

## Visual requirements

- Use a playful appearance with character illustrations and bright colors.
- Use a cream background and black text.
- Game titles can use a bright fill with a black outline.
- Game score panels can use a black background with white or bright text.
- Use a 3 dp black outline and a 4 dp solid shadow for control surfaces.
- Group Home and Games controls on large, flat color surfaces with shared 3 dp black borders.
- Use the complete screen for Home and Games, with no outer outline, shadow, or margins.
- Use square corners for these groups. Do not put shadows between their controls.
- Use Fredoka Bold, weight 700, for titles, section headings, and control labels.
- Use the medium system typeface for instructions, descriptions, and status messages.
- Use the shared text roles and control dimensions below.
- Increase the available space when text needs more room.
- Keep the main action visible when the options scroll.
- Show clear pressed, selected, focused, and disabled control states.
- Give each control an accessibility label that describes its action.
- Use character illustrations to identify activities and support their labels.

## Text roles

Android uses sp for text dimensions and dp for control dimensions.
Other platforms must use the equivalent units that support the selected font scale.

| Role | Typeface | Text dimension | Minimum control height |
| --- | --- | --- | --- |
| Screen title | Fredoka Bold | 32 sp | — |
| Main action | Fredoka Bold | 24 sp | 64 dp |
| Other action or option | Fredoka Bold | 22 sp | 60 dp |
| Section heading | Fredoka Bold | 22 sp | — |
| Instructions or status | System medium | 20 sp | — |
| Attribution link | System medium | 14 sp | 48 dp |
| Large value | Fredoka Bold | 48 sp | — |
| Countdown | Fredoka Bold | 76 sp | — |

A navigation control uses a 60 dp square surface inside the existing toolbar.
A symbol inside an instrument or game board can use dimensions that fit its touch region.
Such symbols must remain clear at the supported screen dimensions.
Use the standard text roles for the surrounding instructions and controls.

## Shared implementation

`PortalStyle` owns the native Android colors, text roles, outlines, shadows, and control states.
`CharacterView` draws the activity illustrations with native Android paths.
Use `PortalStyle.text`, `PortalStyle.button`, and `PortalStyle.primary` for native text and controls.
Use `PortalStyle.scroll` for native scroll containers.
The shared controls show keyboard focus with an outline.
Scroll containers preserve their colors when they receive keyboard focus.
Use the corresponding platform adapter for each separately installed game.
Each adapter must obey this contract.

- Match uses the shared native `PortalStyle` source and its Android theme adapter.
- Blocks uses `portal_theme.dart` and `portal_toolbar.dart`.
- Tiles uses `portal.scss`.
- Kart uses `games/engine-style/build.py` and `PortalAppearance.java`.

The game build commands include the shared Fredoka font and license.
The adapter files implement the values in this document for their respective platforms.

Keep game content, touch controls, scores, and saved data functional after appearance changes.
Keep Home and Back in the existing single toolbar.
Apply this appearance to loading, empty, error, offline, and confirmation states.
The screensaver keeps its black background and dim clock colors.
Its text uses the shared text roles.
The system camera permission window and other operating-system windows use the platform appearance.

Photo Booth uses a fixed main action below its scrollable options.
Home keeps five activity controls visible in one row at the Portal screen dimensions.
These five controls form one continuous strip with equal widths and shared borders.
The timer uses four equal quadrants without a section heading.
A control in the Home toolbar opens the running or paused countdown.
Hide this control when no timer is active.
Games uses four equal quadrants in two rows below the existing toolbar.
Each game has a large character illustration and a bold title near its upper-left corner.
Keep the missing-installation message and the game launch action in each quadrant.
`JoinedSurface` owns these group layouts. `PortalStyle.tile` supplies their pressed, focused, selected, and disabled states.
The child selector uses large colored surfaces with character illustrations and bold names.
Mark the active child with a check mark and a visible status message.
Keep Close visible when the child choices scroll.
The weather card permits scroll input when its larger text needs more space.
Separate the weather card into Now and Today reports.
Use current conditions for the Now report and its clothing illustrations.
Show the daily high, low, and precipitation chance in the Today report.
Give concrete daily recommendations for heat, precipitation, and cooler hours.
Show all applicable recommendations together and preserve forecast uncertainty.
Use a small, right-aligned Open-Meteo attribution link below the weather details.
Keep its touch region at least 48 dp high and link to the source license.
Drawing keeps its canvas, saved documents, brush controls, and share operation.

## Font

The application contains Fredoka from the Google Fonts repository.
The font operates without a network connection.
Its license is `app/src/main/res/raw/fredoka_license.txt`.
Its source is <https://github.com/google/fonts/tree/main/ofl/fredoka>.
Include the font and its license in each game build that uses Fredoka.

## Required validation

- Do a test of each changed screen through its public entry point.
- Verify text bounds, control dimensions, and navigation.
- Verify normal text and the 1.3 font scale.
- Capture screenshots of the changed screens and their relevant states.
- Verify the main action remains visible when secondary controls scroll.
- Verify game boards and instrument touch regions after layout changes.
- Run the applicable toolbar, weather, Photo Booth, and upgrade tests.
- Run `make ci` after the final source change.
- Record physical Portal acceptance separately from emulator results.

Run the native appearance test on a clean, dedicated emulator:

```sh
ANDROID_SERIAL=emulator-5588 make test-android-style
```

The test captures Home, Drawing, Ask, Music, Piano, Guitar, Games, Settings, the timer, and Photo Booth.
The output directory is `android/build/tests/style`.
