# FamilyHome Freedoom adapter

This directory contains the FamilyHome adapter for Freedoom for Android 0.4.3 from `https://github.com/mkrupczak3/Freedoom-for-Android`.

The adaptation adds `PortalGameActivity`, a signature-protected entry point that:

- accepts only calls signed with the same key as `com.mprlab.portal`;
- prepares the bundled open-source Freedoom data in application-private storage;
- keeps saved games in a separate directory for each Children's Portal profile; and
- opens the original touch-enabled game activity directly, avoiding the launcher layout that is unreadable on PortalOS.

The Portal edition removes the original `LAUNCHER` and `LEANBACK_LAUNCHER` entry points. Children therefore cannot reach the engine’s technical arguments screen from PortalOS; the game appears only through an enabled Children's Portal profile.

The original game activity remains non-exported. The modified manifest is in `decoded/AndroidManifest.xml`, and the added Java source is in `portal-src/net/nullsum/freedoom/PortalGameActivity.java`.

Freedoom supplies free game data and is distinct from the commercial Doom data files. Upstream licensing is retained in the source tree and APK assets.
