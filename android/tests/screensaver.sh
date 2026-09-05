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
output=build/tests/screensaver
tools_dir="$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS_VERSION:-36.1.0}"
android_jar="$ANDROID_SDK_ROOT/platforms/${ANDROID_PLATFORM:-android-35}/android.jar"
mkdir -p "$output/classes" "$output/dex"
keystore="$output/test-keystore.jks"
if [[ ! -f "$keystore" ]]; then
  keytool -genkeypair -noprompt -keystore "$keystore" -storepass android -keypass android \
    -alias test -keyalg RSA -validity 3650 -dname 'CN=Screensaver Test' >/dev/null
fi
cleanup() {
  "$adb" uninstall com.mprlab.portal.screensavertest >/dev/null 2>&1 || true
  "$adb" uninstall com.mprlab.portal >/dev/null 2>&1 || true
}
trap cleanup EXIT
ANDROID_DEBUGGABLE=1 \
FAMILYHOME_SERVICE_BASE_URL=https://familyhome.invalid \
FAMILYHOME_DEVICE_TOKEN=familyhome-screensaver-test-token-000000 \
PORTAL_KEYSTORE="$keystore" PORTAL_KEYSTORE_PASSWORD=android PORTAL_KEY_PASSWORD=android \
APK_BASENAME=screensaver-app ./build.sh "$output/app"
"$tools_dir/aapt2" link -I "$android_jar" --manifest tests/screensaver/AndroidManifest.xml -o "$output/test.apk"
javac -source 8 -target 8 -classpath "$android_jar" -d "$output/classes" tests/screensaver/ScreensaverTest.java
"$tools_dir/d8" --lib "$android_jar" --min-api 28 --output "$output/dex" "$output/classes/com/mprlab/portal/screensavertest/"*.class
zip -j -q "$output/test.apk" "$output/dex/classes.dex"
"$tools_dir/zipalign" -f 4 "$output/test.apk" "$output/test-aligned.apk"
"$tools_dir/apksigner" sign --ks "$keystore" --ks-pass pass:android --key-pass pass:android \
  --out "$output/test-signed.apk" "$output/test-aligned.apk"
"$adb" install "$output/app/screensaver-app.apk"
"$adb" install "$output/test-signed.apk"
result="$("$adb" shell am instrument -w com.mprlab.portal.screensavertest/.ScreensaverTest)"
printf '%s\n' "$result"
[[ "$result" == *'Screensaver passed:'* ]]

for screen in settings black clock; do
  "$adb" exec-out run-as com.mprlab.portal cat "files/screensaver-$screen.png" > "$output/$screen.png"
done

"$adb" shell am force-stop com.mprlab.portal
result="$("$adb" shell am instrument -w -e phase persistence com.mprlab.portal.screensavertest/.ScreensaverTest)"
printf '%s\n' "$result"
[[ "$result" == *'Screensaver passed: selections survive process restart.'* ]]
