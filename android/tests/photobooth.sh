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
output=build/tests/photobooth
tools_dir="$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS_VERSION:-36.1.0}"
android_jar="$ANDROID_SDK_ROOT/platforms/${ANDROID_PLATFORM:-android-35}/android.jar"
mkdir -p "$output/classes" "$output/dex"
keystore="$output/test-keystore.jks"
if [[ ! -f "$keystore" ]]; then
  keytool -genkeypair -noprompt -keystore "$keystore" -storepass android -keypass android \
    -alias test -keyalg RSA -validity 3650 -dname 'CN=PhotoBooth Test' >/dev/null
fi
cleanup() {
  "$adb" uninstall com.mprlab.portal.photoboothtest >/dev/null 2>&1 || true
  "$adb" uninstall com.mprlab.portal >/dev/null 2>&1 || true
}
trap cleanup EXIT
ANDROID_DEBUGGABLE=1 \
FAMILYHOME_SERVICE_BASE_URL=https://familyhome.invalid \
FAMILYHOME_DEVICE_TOKEN=familyhome-photobooth-test-token-000000 \
PORTAL_KEYSTORE="$keystore" PORTAL_KEYSTORE_PASSWORD=android PORTAL_KEY_PASSWORD=android \
APK_BASENAME=photobooth-app bash ./build.sh "$output/app"
asset_args=(-A tests/photobooth/assets)
if [[ "${PHOTO_BOOTH_PHASE:-flow}" == head_turn ]]; then
  : "${PHOTO_BOOTH_HEAD_TURN_FRAME:?Provide the private 1280 by 800 Portal camera screenshot}"
  mkdir -p "$output/head-turn-assets"
  cp "$PHOTO_BOOTH_HEAD_TURN_FRAME" "$output/head-turn-assets/head-turn.png"
  asset_args+=(-A "$output/head-turn-assets")
fi
"$tools_dir/aapt2" link -I "$android_jar" --manifest tests/photobooth/AndroidManifest.xml "${asset_args[@]}" -o "$output/test.apk"
javac -source 8 -target 8 -classpath "$android_jar:$output/app/classes" -d "$output/classes" tests/photobooth/PhotoBoothTest.java tests/photobooth/FramingContract.java
"$tools_dir/d8" --lib "$android_jar" --min-api 28 --output "$output/dex" "$output/classes/com/mprlab/portal/photoboothtest/"*.class "$output/classes/com/mprlab/portal/"*.class
zip -j -q "$output/test.apk" "$output/dex/classes.dex"
"$tools_dir/zipalign" -f 4 "$output/test.apk" "$output/test-aligned.apk"
"$tools_dir/apksigner" sign --ks "$keystore" --ks-pass pass:android --key-pass pass:android \
  --out "$output/test-signed.apk" "$output/test-aligned.apk"
"$adb" install "$output/app/photobooth-app.apk"
"$adb" install "$output/test-signed.apk"
result="$("$adb" shell am instrument -w -e phase "${PHOTO_BOOTH_PHASE:-flow}" com.mprlab.portal.photoboothtest/.PhotoBoothTest)"
printf '%s\n' "$result"
if [[ "${PHOTO_BOOTH_PHASE:-flow}" == headroom ]]; then
  [[ "$result" == *'PhotoBooth headroom passed:'* ]]
  "$adb" exec-out run-as com.mprlab.portal cat files/photobooth-headroom.png > "$output/headroom.png"
  exit
fi
if [[ "${PHOTO_BOOTH_PHASE:-flow}" == panel ]]; then
  [[ "$result" == *'PhotoBooth panel passed:'* ]]
  for screen in options filters-panel; do
    "$adb" exec-out run-as com.mprlab.portal cat "files/photobooth-$screen.png" > "$output/$screen.png"
  done
  exit
fi
if [[ "${PHOTO_BOOTH_PHASE:-flow}" == head_turn ]]; then
  [[ "$result" == *'PhotoBooth head turn passed:'* ]]
  exit
fi
if [[ "${PHOTO_BOOTH_PHASE:-flow}" == framing ]]; then
  [[ "$result" == *'PhotoBooth framing passed:'* ]]
  for screen in automatic-single automatic-group; do
    "$adb" exec-out run-as com.mprlab.portal cat "files/photobooth-$screen.png" > "$output/$screen.png"
  done
  exit
fi
[[ "$result" == *'PhotoBooth passed:'* ]]


for effect in BUNNY GOOGLY PARTY; do
  "$adb" exec-out run-as com.mprlab.portal cat "files/effect-$effect.png" > "$output/effect-$effect.png"
done
for screen in review preview album filter automatic-single automatic-group; do
  "$adb" exec-out run-as com.mprlab.portal cat "files/photobooth-$screen.png" > "$output/$screen.png"
done
"$adb" shell am force-stop com.mprlab.portal
"$adb" install -r "$output/app/photobooth-app.apk"
result="$("$adb" shell am instrument -w -e phase persistence com.mprlab.portal.photoboothtest/.PhotoBoothTest)"
printf '%s\n' "$result"
[[ "$result" == *'PhotoBooth passed:'* ]]
