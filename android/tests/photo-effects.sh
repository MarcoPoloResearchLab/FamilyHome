#!/usr/bin/env bash
set -euo pipefail
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
: "${ANDROID_SERIAL:?Set ANDROID_SERIAL to the qualification device}"
cd "$(dirname "${BASH_SOURCE[0]}")/.."
adb="$ANDROID_SDK_ROOT/platform-tools/adb"
package=com.mprlab.portal.photoeffectsqualification
if "$adb" shell pm path "$package" | rg -q '^package:'; then
    echo 'The qualification application already exists on the device.' >&2
    exit 2
fi
output=build/tests/photo-effects
tools_dir="$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS_VERSION:-36.1.0}"
android_jar="$ANDROID_SDK_ROOT/platforms/${ANDROID_PLATFORM:-android-35}/android.jar"
rm -rf "$output/classes" "$output/dex"
source ./vision-dependencies.sh "$output/vision"
mkdir -p "$output/classes" "$output/dex"
keystore="$output/test-keystore.jks"
if [[ ! -f "$keystore" ]]; then
    keytool -genkeypair -noprompt -keystore "$keystore" -storepass android -keypass android \
        -alias test -keyalg RSA -validity 3650 -dname 'CN=Photo Effects Qualification' >/dev/null
fi
"$tools_dir/aapt2" link -I "$android_jar" --manifest tests/photo-effects/AndroidManifest.xml \
    -A tests/photobooth/assets -A "$vision_output/assets" -o "$output/test.apk"
javac -source 8 -target 8 -classpath "$android_jar:$vision_classpath" -d "$output/classes" \
    app/src/main/java/com/mprlab/portal/PhotoEffects.java app/src/main/java/com/mprlab/portal/PhotoFaces.java tests/photobooth/PhotoBoothTest.java
"$tools_dir/d8" --lib "$android_jar" --min-api 28 --output "$output/dex" \
    "$output/classes/com/mprlab/portal/"*.class "$output/classes/com/mprlab/portal/photoboothtest/"*.class "${vision_jars[@]}"
zip -j -q "$output/test.apk" "$output/dex/"*.dex
native_apk="$(cd "$output" && pwd)/test.apk"
(cd "$vision_output" && zip -qr "$native_apk" lib)
"$tools_dir/zipalign" -f 4 "$output/test.apk" "$output/test-aligned.apk"
"$tools_dir/apksigner" sign --ks "$keystore" --ks-pass pass:android --key-pass pass:android \
    --out "$output/photo-effects.apk" "$output/test-aligned.apk"
"$adb" install "$output/photo-effects.apk"
cleanup() { "$adb" uninstall "$package" >/dev/null; }
trap cleanup EXIT
"$adb" shell am instrument -w -e phase effects "$package/com.mprlab.portal.photoboothtest.PhotoBoothTest" > "$output/result.txt"
cat "$output/result.txt"
rg -q 'PhotoBooth effects passed:' "$output/result.txt"
