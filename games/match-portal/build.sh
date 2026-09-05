#!/usr/bin/env bash
set -euo pipefail
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
output="$root/android/build/match-portal"
mkdir -p "$output/cache"

fetch() {
  local name="$1" revision="$2" expected="$3" repository="$4"
  local archive="$output/cache/$name.tar.gz"
  if [[ ! -f "$archive" ]]; then
    curl --fail --location --silent --show-error \
      "https://codeload.github.com/SecUSo/$repository/tar.gz/$revision" -o "$archive"
  fi
  local actual
  actual="$(shasum -a 256 "$archive" | cut -d ' ' -f 1)"
  [[ "$actual" == "$expected" ]] || { printf 'Source hash mismatch: %s\n' "$archive" >&2; exit 1; }
}

fetch match 69e89f31ce5ae13a4cb11ca793dc7276af8000d0 \
  befc735e3ca5914d076d5653a5c337676b384f3131f8c8d4980c2ebcca08bb50 privacy-friendly-memo-game
fetch backup 254885602ee4501a572f56306fbf2b3712cfc834 \
  d2f089d78cd93c03d969ae9c7cace0f85dcb6169c55f014787be04a067a2ab40 privacy-friendly-backup-api

source_dir="$(mktemp -d "$output/source.XXXXXX")"
tar -xzf "$output/cache/match.tar.gz" --strip-components=1 -C "$source_dir"
mkdir -p "$source_dir/libs/privacy-friendly-backup-api"
tar -xzf "$output/cache/backup.tar.gz" --strip-components=1 -C "$source_dir/libs/privacy-friendly-backup-api"
cd "$source_dir"
GIT_CEILING_DIRECTORIES="$output" git apply --check "$root/games/match-portal/upstream.patch"
GIT_CEILING_DIRECTORIES="$output" git apply "$root/games/match-portal/upstream.patch"
cp -R "$root/games/match-portal/overlay/java/." app/src/main/java/
cp -R "$root/games/match-portal/overlay/res/." app/src/main/res/
bash gradlew :app:assembleDebug :app:assembleRelease --console=plain
cp app/build/outputs/apk/debug/app-debug.apk "$output/Match-Portal-debug.apk"
cp app/build/outputs/apk/release/app-release-unsigned.apk "$output/Match-Portal-unsigned.apk"
# Keep the exact source needed to reproduce this build with its upstream licenses.
tar --exclude='./.gradle' --exclude='*/build' --exclude='./local.properties' \
  -czf "$output/Match-Portal-source.tar.gz" .
if [[ -n "${PORTAL_KEYSTORE:-}" ]]; then
  : "${PORTAL_KEYSTORE_PASSWORD:?Set PORTAL_KEYSTORE_PASSWORD}"
  : "${PORTAL_KEY_PASSWORD:?Set PORTAL_KEY_PASSWORD}"
  signer="$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS_VERSION:-36.1.0}/apksigner"
  "$signer" sign --ks "$PORTAL_KEYSTORE" --ks-pass env:PORTAL_KEYSTORE_PASSWORD \
    --key-pass env:PORTAL_KEY_PASSWORD --out "$output/Match-Portal.apk" "$output/Match-Portal-unsigned.apk"
  "$signer" verify "$output/Match-Portal.apk"
fi
printf 'Match build and source: %s\n' "$output"
