#!/usr/bin/env bash
# Source this file from an Android build. Artifacts are pinned and verified before use.
set -euo pipefail
vision_output="$1"
vision_cache="$(pwd)/build/dependencies"
mkdir -p "$vision_cache" "$vision_output/assets" "$vision_output/lib"
vision_fetch() {
  local name="$1" digest="$2" url="$3" destination="$vision_cache/$1"
  if [[ ! -f "$destination" ]]; then
    local download
    download="$(mktemp "$destination.XXXXXX")"
    if ! curl --fail --location --silent --show-error "$url" -o "$download"; then rm -f "$download"; return 1; fi
    if [[ "$(shasum -a 256 "$download" | cut -d ' ' -f 1)" != "$digest" ]]; then
      rm -f "$download"; echo "Vision dependency digest mismatch: $name" >&2; return 1
    fi
    mv "$download" "$destination"
  fi
  [[ "$(shasum -a 256 "$destination" | cut -d ' ' -f 1)" == "$digest" ]] || { echo "Vision dependency digest mismatch: $name" >&2; return 1; }
}
vision_fetch opencv-4.12.0.aar f71846a313388d9da667a59be1e921669fcf792d1ba264b3898253ca789bc3f0 https://repo.maven.apache.org/maven2/org/opencv/opencv/4.12.0/opencv-4.12.0.aar
vision_fetch face_detection_yunet_2023mar.onnx 8f2383e4dd3cfbb4553ea8718107fc0423210dc964f9f4280604804ed2552fa4 https://media.githubusercontent.com/media/opencv/opencv_zoo/47534e27c9851bb1128ccc0102f1145e27f23f98/models/face_detection_yunet/face_detection_yunet_2023mar.onnx
vision_fetch kotlin-stdlib-1.5.20.jar 80cd79c26aac46d72d782de1ecb326061e93c6e688d994b48627ffd668ba63a8 https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/1.5.20/kotlin-stdlib-1.5.20.jar
vision_fetch kotlin-stdlib-common-1.5.20.jar 9819529804bf9296e3853acd5ae824df95d8f8c61309e7768b7cae5ca1361d36 https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-common/1.5.20/kotlin-stdlib-common-1.5.20.jar
vision_fetch kotlin-stdlib-jdk7-1.5.20.jar b110f6d20204303099af0d5f2c846ac60bc6ae5663ef5f22e726ca4627359d06 https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-jdk7/1.5.20/kotlin-stdlib-jdk7-1.5.20.jar
vision_fetch kotlin-stdlib-jdk8-1.5.20.jar a7e9cffe569c43eb8f0fe3139978b0943fe92abcc513f7cf04544f2797f8d38a https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-jdk8/1.5.20/kotlin-stdlib-jdk8-1.5.20.jar
vision_fetch annotations-13.0.jar ace2a10dc8e2d5fd34925ecac03e4988b2c0f851650c94b8cef49ba1bd111478 https://repo.maven.apache.org/maven2/org/jetbrains/annotations/13.0/annotations-13.0.jar
unzip -qo "$vision_cache/opencv-4.12.0.aar" classes.jar 'res/*' 'jni/*' -d "$vision_output"
cp -R "$vision_output/jni/." "$vision_output/lib/"
cp "$vision_cache/face_detection_yunet_2023mar.onnx" "$vision_output/assets/face.onnx"
cp third_party/*-LICENSE.txt "$vision_output/assets/"
vision_jars=("$vision_output/classes.jar" "$vision_cache/kotlin-stdlib-1.5.20.jar" "$vision_cache/kotlin-stdlib-common-1.5.20.jar" "$vision_cache/kotlin-stdlib-jdk7-1.5.20.jar" "$vision_cache/kotlin-stdlib-jdk8-1.5.20.jar" "$vision_cache/annotations-13.0.jar")
vision_classpath="$(IFS=:; echo "${vision_jars[*]}")"
