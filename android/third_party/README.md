# Photo Booth vision dependencies

`vision-dependencies.sh` pins each artifact URL and SHA-256.
The first Android build downloads these artifacts into the ignored `build/dependencies` directory.
Each later build verifies the cached bytes before use.
The build includes the model, native libraries, Java dependencies, and license notices in the APK.

| Component | Version | Source | License |
| --- | --- | --- | --- |
| OpenCV | 4.12.0 | Maven Central, `org.opencv:opencv` | Apache 2.0 |
| YuNet | `face_detection_yunet_2023mar.onnx` | OpenCV Zoo commit `47534e27c9851bb1128ccc0102f1145e27f23f98` | MIT |
| Kotlin standard library | 1.5.20 | Maven Central, OpenCV dependency | Apache 2.0 |
| JetBrains annotations | 13.0 | Maven Central, Kotlin dependency | Apache 2.0 |

OpenCV packages native libraries for arm64-v8a, armeabi-v7a, x86, and x86_64.
The universal APK includes all four architectures for physical devices and emulators.
The device requires no model download, OpenCV Manager, or Google Play services.
Face images remain on the device.
The application does not include the private head-turn fixture or the public test portrait.

YuNet replaces the Android platform face detector for filters and person tracking.
The current 2023 model uses the OpenCV 4.12 inference interface.
The build does not select models or dependency versions at runtime.
