#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to the Android SDK directory}"

build_tools_version="${BUILD_TOOLS_VERSION:-36.1.0}"
android_platform="${ANDROID_PLATFORM:-android-35}"
output_dir="${1:-build/local}"
source_dir="app/src/main"
tools_dir="$ANDROID_SDK_ROOT/build-tools/$build_tools_version"
android_jar="$ANDROID_SDK_ROOT/platforms/$android_platform/android.jar"

mkdir -p "$output_dir/classes" "$output_dir/dex"
"$tools_dir/aapt2" compile --dir "$source_dir/res" -o "$output_dir/compiled.zip"
"$tools_dir/aapt2" link -I "$android_jar" --manifest "$source_dir/AndroidManifest.xml" --java "$output_dir" \
  --min-sdk-version 28 --target-sdk-version 28 --version-code 12 --version-name 0.8.1 \
  -o "$output_dir/portal-unsigned.apk" "$output_dir/compiled.zip"
find "$source_dir/java" -name '*.java' -print | sort > "$output_dir/java-sources.txt"
javac -source 8 -target 8 -classpath "$android_jar" -d "$output_dir/classes" \
  @"$output_dir/java-sources.txt" "$output_dir/com/mprlab/portal/R.java"
find "$output_dir/classes" -name '*.class' -print | sort > "$output_dir/class-files.txt"
"$tools_dir/d8" --lib "$android_jar" --min-api 28 --output "$output_dir/dex" @"$output_dir/class-files.txt"
cp "$output_dir/portal-unsigned.apk" "$output_dir/portal-with-dex.apk"
zip -j -q "$output_dir/portal-with-dex.apk" "$output_dir/dex/classes.dex"
"$tools_dir/zipalign" -f 4 "$output_dir/portal-with-dex.apk" "$output_dir/Children-Portal-v0.8.1-aligned.apk"

if [[ -n "${PORTAL_KEYSTORE:-}" ]]; then
  : "${PORTAL_KEYSTORE_PASSWORD:?Set PORTAL_KEYSTORE_PASSWORD}"
  : "${PORTAL_KEY_PASSWORD:?Set PORTAL_KEY_PASSWORD}"
  "$tools_dir/apksigner" sign --ks "$PORTAL_KEYSTORE" --ks-pass "pass:$PORTAL_KEYSTORE_PASSWORD" \
    --key-pass "pass:$PORTAL_KEY_PASSWORD" --out "$output_dir/Children-Portal-v0.8.1.apk" \
    "$output_dir/Children-Portal-v0.8.1-aligned.apk"
  "$tools_dir/apksigner" verify --verbose "$output_dir/Children-Portal-v0.8.1.apk"
fi
