#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to the Android SDK directory}"
: "${FAMILYHOME_SERVICE_BASE_URL:?Set FAMILYHOME_SERVICE_BASE_URL to the deployed FamilyHome service URL}"
: "${FAMILYHOME_DEVICE_TOKEN:?Set FAMILYHOME_DEVICE_TOKEN to the deployment device token}"

if [[ ${#FAMILYHOME_DEVICE_TOKEN} -lt 32 ]]; then
  printf '%s\n' 'FAMILYHOME_DEVICE_TOKEN must contain at least 32 characters' >&2
  exit 2
fi
if [[ "$FAMILYHOME_SERVICE_BASE_URL" == *$'\n'* || "$FAMILYHOME_DEVICE_TOKEN" == *$'\n'* ]]; then
  printf '%s\n' 'FamilyHome Android configuration values must be single-line strings' >&2
  exit 2
fi

build_tools_version="${BUILD_TOOLS_VERSION:-36.1.0}"
android_platform="${ANDROID_PLATFORM:-android-35}"
output_dir="${1:-build/local}"
source_dir="${ANDROID_SOURCE_DIR:-app/src/main}"
manifest="$source_dir/AndroidManifest.xml"
tools_dir="$ANDROID_SDK_ROOT/build-tools/$build_tools_version"
android_jar="$ANDROID_SDK_ROOT/platforms/$android_platform/android.jar"

version_code="${APP_VERSION_CODE:-$(sed -n 's/.*android:versionCode="\([^"]*\)".*/\1/p' "$manifest" | head -1)}"
version_name="${APP_VERSION_NAME:-$(sed -n 's/.*android:versionName="\([^"]*\)".*/\1/p' "$manifest" | head -1)}"
if [[ -z "$version_code" || -z "$version_name" ]]; then
  printf '%s\n' "Android manifest must declare versionCode and versionName: $manifest" >&2
  exit 2
fi
apk_basename="${APK_BASENAME:-Children-Portal-v$version_name}"

generated_dir="$output_dir/generated"
generated_package_dir="$generated_dir/com/mprlab/portal"
mkdir -p "$output_dir/classes" "$output_dir/dex" "$generated_package_dir"

escape_java_string() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  printf '%s' "$value"
}

escaped_service_url="$(escape_java_string "${FAMILYHOME_SERVICE_BASE_URL%/}")"
escaped_device_token="$(escape_java_string "$FAMILYHOME_DEVICE_TOKEN")"
printf '%s\n' \
  'package com.mprlab.portal;' \
  '' \
  'final class RuntimeConfig {' \
  "    static final String SERVICE_BASE_URL = \"$escaped_service_url\";" \
  "    static final String DEVICE_TOKEN = \"$escaped_device_token\";" \
  '' \
  '    private RuntimeConfig() {' \
  '    }' \
  '}' > "$generated_package_dir/RuntimeConfig.java"
"$tools_dir/aapt2" compile --dir "$source_dir/res" -o "$output_dir/compiled.zip"
link_arguments=(
  -I "$android_jar"
  --manifest "$manifest"
  --java "$output_dir"
  --min-sdk-version 28
  --target-sdk-version 28
  --version-code "$version_code"
  --version-name "$version_name"
)
if [[ "${ANDROID_DEBUGGABLE:-0}" == "1" ]]; then
  link_arguments+=(--debug-mode)
fi
"$tools_dir/aapt2" link "${link_arguments[@]}" \
  -o "$output_dir/portal-unsigned.apk" "$output_dir/compiled.zip"
find "$source_dir/java" -name '*.java' -print | sort > "$output_dir/java-sources.txt"
printf '%s\n' "$generated_package_dir/RuntimeConfig.java" >> "$output_dir/java-sources.txt"
javac -source 8 -target 8 -classpath "$android_jar" -d "$output_dir/classes" \
  @"$output_dir/java-sources.txt" "$output_dir/com/mprlab/portal/R.java"
find "$output_dir/classes" -name '*.class' -print | sort > "$output_dir/class-files.txt"
"$tools_dir/d8" --lib "$android_jar" --min-api 28 --output "$output_dir/dex" @"$output_dir/class-files.txt"
cp "$output_dir/portal-unsigned.apk" "$output_dir/portal-with-dex.apk"
zip -j -q "$output_dir/portal-with-dex.apk" "$output_dir/dex/classes.dex"
"$tools_dir/zipalign" -f 4 "$output_dir/portal-with-dex.apk" "$output_dir/$apk_basename-aligned.apk"

if [[ -n "${PORTAL_KEYSTORE:-}" ]]; then
  : "${PORTAL_KEYSTORE_PASSWORD:?Set PORTAL_KEYSTORE_PASSWORD}"
  : "${PORTAL_KEY_PASSWORD:?Set PORTAL_KEY_PASSWORD}"
  "$tools_dir/apksigner" sign --ks "$PORTAL_KEYSTORE" --ks-pass "pass:$PORTAL_KEYSTORE_PASSWORD" \
    --key-pass "pass:$PORTAL_KEY_PASSWORD" --out "$output_dir/$apk_basename.apk" \
    "$output_dir/$apk_basename-aligned.apk"
  "$tools_dir/apksigner" verify --verbose "$output_dir/$apk_basename.apk"
fi
