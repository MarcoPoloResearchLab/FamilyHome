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
output=build/tests/ask
tools_dir="$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS_VERSION:-36.1.0}"
android_jar="$ANDROID_SDK_ROOT/platforms/${ANDROID_PLATFORM:-android-35}/android.jar"
mkdir -p "$output/classes" "$output/dex"
keystore="$output/test-keystore.jks"
if [[ ! -f "$keystore" ]]; then
  keytool -genkeypair -noprompt -keystore "$keystore" -storepass android -keypass android \
    -alias test -keyalg RSA -validity 3650 -dname 'CN=Ask Test' >/dev/null
fi
original_font_scale="$("$adb" shell settings get system font_scale | tr -d '\r')"
cleanup() {
  "$adb" shell settings put system font_scale "$original_font_scale" >/dev/null
  "$adb" uninstall com.mprlab.portal.asktest >/dev/null 2>&1 || true
  "$adb" uninstall com.mprlab.portal >/dev/null 2>&1 || true
}
trap cleanup EXIT
ANDROID_DEBUGGABLE=1 FAMILYHOME_SERVICE_BASE_URL=http://127.0.0.1:18765 \
FAMILYHOME_DEVICE_TOKEN=familyhome-ask-test-token-000000 \
PORTAL_KEYSTORE="$keystore" PORTAL_KEYSTORE_PASSWORD=android PORTAL_KEY_PASSWORD=android \
APK_BASENAME=ask-app ./build.sh "$output/app"
"$tools_dir/aapt2" link -I "$android_jar" --manifest tests/ask/AndroidManifest.xml -o "$output/test.apk"
javac -source 8 -target 8 -classpath "$android_jar" -d "$output/classes" tests/ask/AskTest.java
"$tools_dir/d8" --lib "$android_jar" --min-api 28 --output "$output/dex" "$output"/classes/com/mprlab/portal/asktest/*.class
zip -j -q "$output/test.apk" "$output/dex/classes.dex"
"$tools_dir/zipalign" -f 4 "$output/test.apk" "$output/test-aligned.apk"
"$tools_dir/apksigner" sign --ks "$keystore" --ks-pass pass:android --key-pass pass:android \
  --out "$output/test-signed.apk" "$output/test-aligned.apk"
"$adb" install "$output/app/ask-app.apk"
"$adb" install "$output/test-signed.apk"
"$adb" shell pm grant com.mprlab.portal android.permission.RECORD_AUDIO
for scale in 1.0 1.3; do
  "$adb" shell settings put system font_scale "$scale"
  "$adb" shell am force-stop com.mprlab.portal
  result="$("$adb" shell am instrument -w -e phase "${ASK_TEST_PHASE:-all}" com.mprlab.portal.asktest/.AskTest)"
  printf '%s\n' "$result"
  for screen in ready thinking answer error recording; do
    "$adb" exec-out run-as com.mprlab.portal cat "files/ask-$screen.png" > "$output/$screen-$scale.png" 2>/dev/null || true
  done
  [[ "$result" == *'Ask passed:'* ]]
done
"$adb" shell pm revoke com.mprlab.portal android.permission.RECORD_AUDIO
"$adb" shell pm set-permission-flags com.mprlab.portal android.permission.RECORD_AUDIO user-set user-fixed
result="$("$adb" shell am instrument -w -e phase denied com.mprlab.portal.asktest/.AskTest)"
printf '%s\n' "$result"
[[ "$result" == *'Ask passed:'* ]]
