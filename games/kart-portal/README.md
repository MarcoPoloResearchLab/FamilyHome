# FamilyHome Kart adapter

Kart uses the SuperTuxKart 1.5 engine.
The FamilyHome adaptation adds a generated skin, Fredoka Bold, and an Android appearance adapter.
`PortalAppearance.java` applies the skin and font scale before the native engine starts.
It keeps the other configuration values and saved data.
The native renderer supplies white and bright score text.
A black score panel keeps this text clear inside the cream results screen.

Run `make build-kart` from the repository root.
The [interface build guide](../engine-style/README.md) defines inputs, outputs, and validation.
The build keeps the upstream native libraries and asset license records.
The [game guide](../../android/GAMES.md) records the upstream source and license requirements.
