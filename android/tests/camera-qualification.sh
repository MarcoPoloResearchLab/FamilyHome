#!/usr/bin/env bash
set -euo pipefail
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
: "${ANDROID_SERIAL:?Set ANDROID_SERIAL to the qualification device}"
cd "$(dirname "${BASH_SOURCE[0]}")/.."
adb="$ANDROID_SDK_ROOT/platform-tools/adb"
tools_dir="$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS_VERSION:-36.1.0}"
android_jar="$ANDROID_SDK_ROOT/platforms/${ANDROID_PLATFORM:-android-35}/android.jar"
output=build/tests/camera-qualification
package=com.mprlab.portal.cameraqualification
if "$adb" shell pm path "$package" | rg -q '^package:'; then
    echo 'The device already contains the qualification application. Preserve its evidence before removal.' >&2
    exit 2
fi
mkdir -p "$output/classes" "$output/dex"
keystore="$output/test-keystore.jks"
if [[ ! -f "$keystore" ]]; then
    keytool -genkeypair -noprompt -keystore "$keystore" -storepass android -keypass android \
        -alias test -keyalg RSA -validity 3650 -dname 'CN=Camera Qualification Test' >/dev/null
fi
"$tools_dir/aapt2" link -I "$android_jar" --manifest tests/camera-qualification/AndroidManifest.xml -o "$output/test.apk"
javac -source 8 -target 8 -classpath "$android_jar" -d "$output/classes" \
    app/src/main/java/com/mprlab/portal/PortalCamera.java tests/camera-qualification/*.java
"$tools_dir/d8" --lib "$android_jar" --min-api 28 --output "$output/dex" "$output/classes/com/mprlab/portal/"*.class
zip -j -q "$output/test.apk" "$output/dex/classes.dex"
"$tools_dir/zipalign" -f 4 "$output/test.apk" "$output/test-aligned.apk"
"$tools_dir/apksigner" sign --ks "$keystore" --ks-pass pass:android --key-pass pass:android \
    --out "$output/camera-qualification.apk" "$output/test-aligned.apk"
"$adb" install "$output/camera-qualification.apk"
cleanup() { "$adb" uninstall "$package" >/dev/null; }
trap cleanup EXIT
"$adb" shell getprop ro.product.model > "$output/device.txt"
"$adb" shell getprop ro.build.version.release >> "$output/device.txt"
shasum -a 256 "$output/camera-qualification.apk" > "$output/apk.sha256"
for phase in permission capture; do
    if [[ "$phase" == capture ]]; then "$adb" shell pm grant "$package" android.permission.CAMERA; fi
    "$adb" shell am force-stop "$package"
    "$adb" shell am instrument -w -e phase "$phase" "$package/com.mprlab.portal.CameraQualificationTest" > "$output/$phase.txt"
    cat "$output/$phase.txt"
    if ! rg -q 'Camera qualification passed:' "$output/$phase.txt"; then exit 1; fi
done
for artifact in camera-qualification.jpg camera-qualification.png; do
    "$adb" exec-out run-as "$package" cat "files/$artifact" > "$output/$artifact"
done
