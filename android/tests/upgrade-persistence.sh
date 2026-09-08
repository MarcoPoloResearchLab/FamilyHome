#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to the Android SDK directory}"

android_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$android_root"

adb="$ANDROID_SDK_ROOT/platform-tools/adb"
tools_dir="$ANDROID_SDK_ROOT/build-tools/${BUILD_TOOLS_VERSION:-36.1.0}"
apksigner="$tools_dir/apksigner"
package_name="com.mprlab.portal"
ui_package="com.mprlab.portal.upgradeuitest"
serial="${ANDROID_SERIAL:-}"

if [[ -z "$serial" ]]; then
  emulator_list="$($adb devices | awk '$2 == "device" && $1 ~ /^emulator-/ {print $1}')"
  emulator_count="$(printf '%s\n' "$emulator_list" | awk 'NF {count++} END {print count+0}')"
  if [[ "$emulator_count" != "1" ]]; then
    printf '%s\n' 'Set ANDROID_SERIAL to one dedicated running emulator.' >&2
    exit 2
  fi
  serial="$emulator_list"
fi

if [[ "$serial" != emulator-* && "${ALLOW_PHYSICAL_DEVICE_TESTS:-0}" != "1" ]]; then
  printf 'Refusing upgrade test on non-emulator device %s.\n' "$serial" >&2
  exit 2
fi
if [[ "$($adb -s "$serial" get-state 2>/dev/null || true)" != "device" ]]; then
  printf 'Android device is unavailable: %s\n' "$serial" >&2
  exit 2
fi
existing_app=0
if "$adb" -s "$serial" shell pm path "$package_name" 2>/dev/null | grep -q '^package:'; then
  existing_app=1
  if [[ "${ALLOW_REPLACE_EXISTING_TEST_APP:-0}" != "1" ]]; then
    printf 'Refusing to replace existing %s data on %s. Use a dedicated clean emulator.\n' "$package_name" "$serial" >&2
    exit 2
  fi
fi

original_size="$($adb -s "$serial" shell wm size | tr -d '\r')"
original_density="$($adb -s "$serial" shell wm density | tr -d '\r')"
original_size_override="$(printf '%s\n' "$original_size" | awk -F': ' '/Override size/ {print $2}')"
original_density_override="$(printf '%s\n' "$original_density" | awk -F': ' '/Override density/ {print $2}')"
immersive_setting="immersive_mode_confirmations"
original_immersive_confirmation="$($adb -s "$serial" shell settings get secure "$immersive_setting" | tr -d '\r')"
animation_settings=(window_animation_scale transition_animation_scale animator_duration_scale)
original_animation_values=()
for setting in "${animation_settings[@]}"; do
  original_animation_values+=("$($adb -s "$serial" shell settings get global "$setting" | tr -d '\r')")
done

cleanup() {
  "$adb" -s "$serial" uninstall "$ui_package" >/dev/null 2>&1 || true
  local index
  for index in "${!animation_settings[@]}"; do
    if [[ "${original_animation_values[$index]}" == "null" ]]; then
      "$adb" -s "$serial" shell settings delete global "${animation_settings[$index]}" >/dev/null
    else
      "$adb" -s "$serial" shell settings put global "${animation_settings[$index]}" "${original_animation_values[$index]}" >/dev/null
    fi
  done
  if [[ "$original_immersive_confirmation" == "null" ]]; then
    "$adb" -s "$serial" shell settings delete secure "$immersive_setting" >/dev/null
  else
    "$adb" -s "$serial" shell settings put secure "$immersive_setting" "$(printf '%q' "$original_immersive_confirmation")" >/dev/null
  fi
  if [[ "${KEEP_TEST_APP:-0}" != "1" ]]; then
    "$adb" -s "$serial" uninstall "$package_name" >/dev/null 2>&1 || true
  fi
  if [[ -n "$original_size_override" ]]; then
    "$adb" -s "$serial" shell wm size "$original_size_override" >/dev/null 2>&1 || true
  else
    "$adb" -s "$serial" shell wm size reset >/dev/null 2>&1 || true
  fi
  if [[ -n "$original_density_override" ]]; then
    "$adb" -s "$serial" shell wm density "$original_density_override" >/dev/null 2>&1 || true
  else
    "$adb" -s "$serial" shell wm density reset >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

if [[ "$existing_app" == "1" ]]; then
  "$adb" -s "$serial" uninstall "$package_name" >/dev/null
fi

output_root="build/tests/upgrade"
fixture_output="$output_root/fixture"
current_output="$output_root/current"
keystore="$output_root/test-keystore.jks"
mkdir -p "$output_root"

if [[ ! -f "$keystore" ]]; then
  keytool -genkeypair -noprompt \
    -keystore "$keystore" -storepass android -keypass android \
    -alias familyhome-test -keyalg RSA -keysize 2048 -validity 10000 \
    -dname 'CN=FamilyHome Regression Tests, OU=Tests, O=MPR Lab, C=US' >/dev/null
fi

build_apk() {
  local source_dir="$1"
  local output_dir="$2"
  ANDROID_SOURCE_DIR="$source_dir" \
  FAMILYHOME_SERVICE_BASE_URL=https://familyhome.invalid \
  FAMILYHOME_DEVICE_TOKEN=familyhome-upgrade-test-token-000000 \
  ANDROID_DEBUGGABLE=1 \
  ./build.sh "$output_dir"
}

sign_apk() {
  local aligned="$1"
  local signed="$2"
  "$apksigner" sign \
    --ks "$keystore" --ks-key-alias familyhome-test \
    --ks-pass pass:android --key-pass pass:android \
    --out "$signed" "$aligned"
  "$apksigner" verify --verbose "$signed" >/dev/null
}

build_apk tests/fixtures/upgrade-v1/app/src/main "$fixture_output"
build_apk app/src/main "$current_output"

fixture_apk="$fixture_output/fixture-signed.apk"
current_version="$(sed -n 's/.*android:versionName="\([^"]*\)".*/\1/p' app/src/main/AndroidManifest.xml | head -1)"
current_apk="$current_output/current-signed.apk"
sign_apk "$fixture_output/Children-Portal-v0.8.0-upgrade-fixture-aligned.apk" "$fixture_apk"
sign_apk "$current_output/Children-Portal-v$current_version-aligned.apk" "$current_apk"

android_jar="$ANDROID_SDK_ROOT/platforms/${ANDROID_PLATFORM:-android-35}/android.jar"
ui_output="$output_root/ui"
rm -rf "$ui_output/classes" "$ui_output/dex"
mkdir -p "$ui_output/classes" "$ui_output/dex"
"$tools_dir/aapt2" link -I "$android_jar" --manifest tests/upgrade-ui/AndroidManifest.xml -o "$ui_output/test.apk"
javac -source 8 -target 8 -classpath "$android_jar" -d "$ui_output/classes" tests/upgrade-ui/UpgradeUiFlow.java
"$tools_dir/d8" --lib "$android_jar" --min-api 28 --output "$ui_output/dex" "$ui_output/classes/com/mprlab/portal/upgradeuitest/"*.class
zip -j -q "$ui_output/test.apk" "$ui_output/dex/classes.dex"
"$tools_dir/zipalign" -f 4 "$ui_output/test.apk" "$ui_output/test-aligned.apk"
sign_apk "$ui_output/test-aligned.apk" "$ui_output/test-signed.apk"
"$adb" -s "$serial" install "$ui_output/test-signed.apk" >/dev/null

"$adb" -s "$serial" shell wm size 1280x800 >/dev/null
"$adb" -s "$serial" shell wm density 160 >/dev/null
"$adb" -s "$serial" shell settings put secure "$immersive_setting" confirmed
for setting in "${animation_settings[@]}"; do
  "$adb" -s "$serial" shell settings put global "$setting" 0
done
"$adb" -s "$serial" install "$fixture_apk" >/dev/null
"$adb" -s "$serial" shell am start -W -n "$package_name/.MainActivity" >/dev/null

legacy_preferences="$($adb -s "$serial" exec-out run-as "$package_name" cat shared_prefs/children_portal.xml)"
if [[ "$legacy_preferences" != *"Alice"* || "$legacy_preferences" != *"sunset-drawing"* ]]; then
  printf '%s\n' 'Upgrade test failed: legacy fixture did not seed its state.' >&2
  exit 1
fi

"$adb" -s "$serial" install -r "$current_apk" >/dev/null
"$adb" -s "$serial" shell am force-stop "$package_name"
"$adb" -s "$serial" shell am start -W -n "$package_name/.MainActivity" >/dev/null

ui_result="$("$adb" -s "$serial" shell am instrument -w -r "$ui_package/.UpgradeUiFlow")"
printf '%s\n' "$ui_result" > "$output_root/navigation.instrumentation.txt"
if [[ "$ui_result" != *'INSTRUMENTATION_CODE: -1'* || "$ui_result" != *'INSTRUMENTATION_RESULT: result=Upgrade UI navigation passed'* ]]; then
  printf 'Upgrade test failed: UI navigation.\n%s\n' "$ui_result" >&2
  "$adb" -s "$serial" shell dumpsys window windows > "$output_root/windows.txt"
  "$adb" -s "$serial" logcat -d -t 300 '*:E' >&2 || true
  exit 1
fi

preferences="$($adb -s "$serial" exec-out run-as "$package_name" cat shared_prefs/children_portal.xml)"
for expected in Alice Bob alice-profile bob-profile Sunset sunset-drawing legacy_marker preserve-me 90210 555000 \
    drawing_color_alice-profile drawing_size_alice-profile \
    drawing_eraser_alice-profile; do
  if [[ "$preferences" != *"$expected"* ]]; then
    printf 'Upgrade test failed: normalized preferences are missing %s\n' "$expected" >&2
    exit 1
  fi
done
for removed in enabled_game_ids kart_enabled; do
  if [[ "$preferences" == *"$removed"* ]]; then
    printf 'Upgrade test failed: migrated preferences still contain legacy key %s\n' "$removed" >&2
    exit 1
  fi
done
sentinel="$($adb -s "$serial" exec-out run-as "$package_name" cat files/upgrade-sentinel.txt)"
if [[ "$sentinel" != "legacy-private-file" ]]; then
  printf '%s\n' 'Upgrade test failed: app-private file did not survive replacement.' >&2
  exit 1
fi

installed_version="$($adb -s "$serial" shell dumpsys package "$package_name" \
  | sed -n 's/^[[:space:]]*versionCode=\([0-9]*\).*/\1/p' \
  | head -1)"
expected_version="$(sed -n 's/.*android:versionCode="\([^"]*\)".*/\1/p' app/src/main/AndroidManifest.xml | head -1)"
if [[ "$installed_version" != "$expected_version" ]]; then
  printf 'Upgrade test failed: installed versionCode is %s, expected %s.\n' "$installed_version" "$expected_version" >&2
  exit 1
fi

printf 'Android upgrade preservation passed on %s: fixture -> versionCode %s.\n' "$serial" "$expected_version"
