#!/usr/bin/env bash
set -euo pipefail
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
: "${ANDROID_SERIAL:?Set ANDROID_SERIAL to a dedicated clean emulator}"
[[ "$ANDROID_SERIAL" == emulator-* ]] || { echo 'A dedicated emulator is required.' >&2; exit 2; }
cd "$(dirname "${BASH_SOURCE[0]}")/.."
adb="$ANDROID_SDK_ROOT/platform-tools/adb"
if "$adb" shell pm path com.mprlab.portal | grep -q '^package:'; then
  echo 'The emulator must have no FamilyHome installation.' >&2
  exit 2
fi
output=build/tests/style
tools_dir="$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS_VERSION:-36.1.0}"
android_jar="$ANDROID_SDK_ROOT/platforms/${ANDROID_PLATFORM:-android-35}/android.jar"
mkdir -p "$output/classes" "$output/dex"
keystore="$output/test-keystore.jks"
if [[ ! -f "$keystore" ]]; then
  keytool -genkeypair -noprompt -keystore "$keystore" -storepass android -keypass android \
    -alias test -keyalg RSA -validity 3650 -dname 'CN=Style Test' >/dev/null
fi
animation_keys=(window_animation_scale transition_animation_scale animator_duration_scale)
animation_values=()
for key in "${animation_keys[@]}"; do animation_values+=("$("$adb" shell settings get global "$key" | tr -d '\r')"); done
original_font_scale="$("$adb" shell settings get system font_scale | tr -d '\r')"
cleanup() {
  for i in "${!animation_keys[@]}"; do
    if [[ "${animation_values[$i]}" == null ]]; then
      "$adb" shell settings delete global "${animation_keys[$i]}" >/dev/null
    else
      "$adb" shell settings put global "${animation_keys[$i]}" "${animation_values[$i]}" >/dev/null
    fi
  done
  "$adb" shell settings put system font_scale "$original_font_scale" >/dev/null
  "$adb" uninstall com.mprlab.portal.styletest >/dev/null 2>&1 || true
  "$adb" uninstall com.mprlab.portal >/dev/null 2>&1 || true
}
trap cleanup EXIT
for key in "${animation_keys[@]}"; do "$adb" shell settings put global "$key" 0; done
ANDROID_DEBUGGABLE=1 \
FAMILYHOME_SERVICE_BASE_URL=https://familyhome.invalid \
FAMILYHOME_DEVICE_TOKEN=familyhome-style-test-token-000000 \
PORTAL_KEYSTORE="$keystore" PORTAL_KEYSTORE_PASSWORD=android PORTAL_KEY_PASSWORD=android \
APK_BASENAME=style-app ./build.sh "$output/app"
"$tools_dir/aapt2" link -I "$android_jar" --manifest tests/style/AndroidManifest.xml -o "$output/test.apk"
javac -source 8 -target 8 -classpath "$android_jar" -d "$output/classes" tests/style/StyleTest.java
"$tools_dir/d8" --lib "$android_jar" --min-api 28 --output "$output/dex" "$output/classes/com/mprlab/portal/styletest/"*.class
zip -j -q "$output/test.apk" "$output/dex/classes.dex"
"$tools_dir/zipalign" -f 4 "$output/test.apk" "$output/test-aligned.apk"
"$tools_dir/apksigner" sign --ks "$keystore" --ks-pass pass:android --key-pass pass:android \
  --out "$output/test-signed.apk" "$output/test-aligned.apk"
"$adb" install "$output/app/style-app.apk"
"$adb" install "$output/test-signed.apk"
"$adb" shell pm grant com.mprlab.portal android.permission.CAMERA
for scale in 1.0 1.3; do
  "$adb" shell settings put system font_scale "$scale"
  "$adb" shell am force-stop com.mprlab.portal
  result="$("$adb" shell am instrument -w com.mprlab.portal.styletest/.StyleTest)"
  printf '%s\n' "$result"
  for screen in home drawing ask music piano guitar games games-scrolled settings timer photobooth photobooth-filters photobooth-scrolled; do
    "$adb" exec-out run-as com.mprlab.portal cat "files/style-$screen.png" > "$output/$screen-$scale.png" 2>/dev/null || true
  done
  [[ "$result" == *'Style passed:'* ]]
done
