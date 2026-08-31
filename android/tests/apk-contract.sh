#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to the Android SDK directory}"

android_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$android_root"

manifest="app/src/main/AndroidManifest.xml"
version_code="$(sed -n 's/.*android:versionCode="\([^"]*\)".*/\1/p' "$manifest" | head -1)"
version_name="$(sed -n 's/.*android:versionName="\([^"]*\)".*/\1/p' "$manifest" | head -1)"
output_dir="build/tests/apk-contract"
apk="$output_dir/Children-Portal-v$version_name-aligned.apk"
tools_dir="$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS_VERSION:-36.1.0}"
aapt="$tools_dir/aapt"

require_text() {
  local text="$1"
  local expected="$2"
  local description="$3"
  if [[ "$text" != *"$expected"* ]]; then
    printf 'APK contract failed: %s; expected %s\n' "$description" "$expected" >&2
    exit 1
  fi
}

FAMILYHOME_SERVICE_BASE_URL=https://familyhome.invalid \
FAMILYHOME_DEVICE_TOKEN=familyhome-ci-device-token-000000000 \
ANDROID_DEBUGGABLE=1 \
./build.sh "$output_dir"

badging="$($aapt dump badging "$apk")"
require_text "$badging" "package: name='com.mprlab.portal' versionCode='$version_code' versionName='$version_name'" "package and version identity"
require_text "$badging" "sdkVersion:'28'" "minimum Android version"
require_text "$badging" "targetSdkVersion:'28'" "target Android version"

xmltree="$($aapt dump xmltree "$apk" AndroidManifest.xml)"
for component in MainActivity AskActivity DrawingActivity PianoActivity SettingsActivity ShareProvider; do
  require_text "$xmltree" "$component" "manifest component $component"
done
require_text "$xmltree" "android.intent.category.HOME" "HOME intent"
require_text "$xmltree" "android.intent.category.LAUNCHER" "launcher intent"

dex_strings="$(unzip -p "$apk" classes.dex | strings)"
require_text "$dex_strings" "https://familyhome.invalid" "generated service URL"
require_text "$dex_strings" "familyhome-ci-device-token-000000000" "generated device token"
if [[ "$dex_strings" == *"LLM_PROXY_SECRET"* ]]; then
  printf '%s\n' 'APK contract failed: the LLM Proxy secret boundary reached the Android APK' >&2
  exit 1
fi

printf 'Android APK contract passed for com.mprlab.portal %s (%s).\n' "$version_name" "$version_code"
