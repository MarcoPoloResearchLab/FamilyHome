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
export_evidence() {
    local staging
    staging="$(mktemp -d "$output/export.XXXXXX")" || return 1
    if ! "$adb" exec-out run-as "$package" tar -cf - -C files . > "$staging/evidence.tar"; then
        rm -r "$staging"
        return 1
    fi
    if ! tar -xf "$staging/evidence.tar" -C "$staging"; then
        rm -r "$staging"
        return 1
    fi
    for artifact in camera-qualification.jpg camera-qualification.png; do
        if [[ -f "$staging/$artifact" ]]; then
            if ! mv "$staging/$artifact" "$output/$artifact"; then
                rm -r "$staging"
                return 1
            fi
        fi
    done
    rm -r "$staging"
}
cleanup() {
    local status=$?
    trap - EXIT
    if export_evidence; then
        if ! "$adb" uninstall "$package" >/dev/null; then
            echo "Remove qualification application $package: uninstall failed" >&2
            if [[ "$status" == 0 ]]; then status=1; fi
        fi
    else
        echo "Export camera evidence failed. Application $package and its original evidence remain on $ANDROID_SERIAL." >&2
        echo "Copy the evidence before removing the qualification application." >&2
        if [[ "$status" == 0 ]]; then status=1; fi
    fi
    exit "$status"
}
trap cleanup EXIT
"$adb" shell getprop ro.product.model > "$output/device.txt"
"$adb" shell getprop ro.build.version.release >> "$output/device.txt"
shasum -a 256 "$output/camera-qualification.apk" > "$output/apk.sha256"
rotation=fixed
if [[ "$ANDROID_SERIAL" == emulator-* ]]; then rotation=dynamic; fi
for phase in permission capture; do
    if [[ "$phase" == capture ]]; then "$adb" shell pm grant "$package" android.permission.CAMERA; fi
    "$adb" shell am force-stop "$package"
    "$adb" shell am instrument -w -e rotation "$rotation" -e phase "$phase" "$package/com.mprlab.portal.CameraQualificationTest" > "$output/$phase.txt"
    cat "$output/$phase.txt"
    if ! rg -q 'Camera qualification passed:' "$output/$phase.txt"; then exit 1; fi
done
