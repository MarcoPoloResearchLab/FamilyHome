#!/usr/bin/env bash
set -euo pipefail
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
game="${1:?Specify blocks or tiles}"
case "$game" in
  blocks)
    repository=brandonp2412/BlockDrop
    revision=019db6c2fc69937160901572192982b5cb843f4b
    expected=8bcf8c415b5975d03dbc36ce2af1df617207183544e1b58c28461c57ac6c0bd3
    title=Blocks
    : "${FLUTTER_ROOT:?Set FLUTTER_ROOT to Flutter 3.44.0}"
    [[ "$(git -C "$FLUTTER_ROOT" rev-parse HEAD)" == 559ffa3f75e7402d65a8def9c28389a9b2e6fe42 ]] || { echo 'Flutter revision mismatch.' >&2; exit 1; }
    ;;
  tiles)
    repository=gvtulder/tessel
    revision=7da4fa622e1f2510894f85d98e3d1653b81184f4
    expected=e713c761cc0c2db18954d5e4a5e98400d0fcc4f918b88580bcb0a22488bca8f7
    title=Tiles
    ;;
  *) echo 'Specify blocks or tiles.' >&2; exit 2 ;;
esac
output="$root/android/build/$game-portal"
mkdir -p "$output/cache"
archive="$output/cache/source.tar.gz"
if [[ ! -f "$archive" ]]; then
  curl --fail --location --silent --show-error "https://codeload.github.com/$repository/tar.gz/$revision" -o "$archive"
fi
[[ "$(shasum -a 256 "$archive" | cut -d ' ' -f 1)" == "$expected" ]] || { echo 'Source hash mismatch.' >&2; exit 1; }
source_dir="$(mktemp -d "$output/source.XXXXXX")"
tar -xzf "$archive" --strip-components=1 -C "$source_dir"
cd "$source_dir"
GIT_CEILING_DIRECTORIES="$output" git apply --check "$root/games/$game-portal/upstream.patch"
GIT_CEILING_DIRECTORIES="$output" git apply "$root/games/$game-portal/upstream.patch"
cp -R "$root/games/$game-portal/overlay/." .
case "$game" in
  blocks)
    "$FLUTTER_ROOT/bin/flutter" pub get --enforce-lockfile
    "$FLUTTER_ROOT/bin/flutter" build apk --release --target-platform android-arm64 --build-number 443 --build-name 1.0.47-portal1
    cp build/app/outputs/flutter-apk/app-release.apk "$output/$title-Portal-build.apk"
    ;;
  tiles)
    npm ci
    npm run build-cap-html
    npx --no-install cap sync android
    (cd android && bash gradlew assembleDebug assembleRelease --console=plain)
    cp android/app/build/outputs/apk/debug/app-debug.apk "$output/$title-Portal-debug.apk"
    cp android/app/build/outputs/apk/release/app-release-unsigned.apk "$output/$title-Portal-build.apk"
    ;;
esac
tar --exclude='./node_modules' --exclude='*/build' --exclude='*/.gradle' --exclude='./.dart_tool' \
  --exclude='./.parcel-cache' --exclude='*/local.properties' --exclude='./.flutter-plugins-dependencies' \
  -czf "$output/$title-Portal-source.tar.gz" .
if [[ -n "${PORTAL_KEYSTORE:-}" ]]; then
  : "${PORTAL_KEYSTORE_PASSWORD:?Set PORTAL_KEYSTORE_PASSWORD}"
  : "${PORTAL_KEY_PASSWORD:?Set PORTAL_KEY_PASSWORD}"
  signer="$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS_VERSION:-36.1.0}/apksigner"
  "$signer" sign --ks "$PORTAL_KEYSTORE" --ks-pass env:PORTAL_KEYSTORE_PASSWORD \
    --key-pass env:PORTAL_KEY_PASSWORD --out "$output/$title-Portal.apk" "$output/$title-Portal-build.apk"
  "$signer" verify "$output/$title-Portal.apk"
fi
printf '%s build and source: %s\n' "$title" "$output"
