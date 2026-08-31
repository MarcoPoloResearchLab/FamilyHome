# Reviewed game builds

FamilyHome does not bundle game APKs. The following builds were inspected and accepted on the 1280 × 800 ARM64 Portal on 2026-08-31. The package and activity names are the runtime contract used by `GameCatalog`.

| FamilyHome name | Upstream | Version | Package | SHA-256 |
| --- | --- | --- | --- | --- |
| Blocks | [Block Drop](https://github.com/brandonp2412/BlockDrop) | 1.0.47 (442, ARM64) | `com.blockdrop.game` | `91eee8c21e4d6b0792096bd7b265fdd0c572434d8a190da629da7c4223a393b0` |
| Tiles | [Tessel](https://github.com/gvtulder/tessel) | 1.0.23 (10023) | `net.vantulder.tessel` | `d6cfb17fc5dbec16ebf560228425361386ce4c86fd9affc3452428de7b99d7a8` |
| Match | [Privacy Friendly Memo Game](https://github.com/SecUSo/privacy-friendly-memo-game) | 1.1.3-google (100) | `org.secuso.privacyfriendlymemory` | `82f83d9ea572240acabe45bcff9809f724d26f474896b150dcfd49d84037d8c6` |

The builds came from the F-Droid repository. Before installation, Android build tools confirmed the package, version, minimum SDK, launch activity, requested permissions, signature, and full-file digest.

## Permission review

- Blocks requests `INTERNET`, `ACCESS_NETWORK_STATE`, and `ACCESS_WIFI_STATE`. Its upstream manifest declares these permissions. The game remains separately uninstallable and FamilyHome does not grant it access to FamilyHome profile data.
- Tiles requests no Android platform permission. It declares only its package-scoped dynamic-receiver permission.
- Match requests no Android permission.

## Signing certificates

| Game | Certificate SHA-256 |
| --- | --- |
| Blocks | `011ee1a6e4e5ecd675f67fbf3d78ad82614a7a7a3f24ed71cc9c417154a0f0fd` |
| Tiles | `e505799c9beec31b95a9ff1c3d650b0812260cc1401c7b260ac5f37cd8a9abe4` |
| Match | `9dd96e14306c971f999f05e00599c8c9c17f83657972875bd22678dd937438bd` |

Recheck all fields and update this ledger before accepting a newer APK. APK files and signing material stay outside Git.
