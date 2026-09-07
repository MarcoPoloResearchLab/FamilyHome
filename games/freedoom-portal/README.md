# FamilyHome Freedoom adapter

Freedoom uses Freedoom for Android 0.4.3.
`PortalGameActivity` supplies the FamilyHome entry point.
The entry point requires the same signing certificate as FamilyHome.
It validates the child profile, prepares game files, and opens the original game activity.
Saved games stay in a separate directory for each child profile.

The adaptation adds Fredoka Bold, bright menu controls, instructions, and status panels.
Preparation and error screens use the shared `PortalStyle` source.
The Android font scale selects the generated resource pack.

The manifest is `AndroidManifest.xml`.
The Java adapter is `src/net/nullsum/freedoom/PortalGameActivity.java`.
The original game activity has no exported entry point.
The game appears through the FamilyHome Games screen.

Run `make build-freedoom` from the repository root.
The [interface build guide](../engine-style/README.md) defines inputs, outputs, and validation.
The build keeps the upstream native libraries and license records.
The interface source archive contains the adaptation, not the complete upstream engine source.
Freedoom supplies free game data under its upstream license terms.
