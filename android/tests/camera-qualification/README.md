# Camera qualification

Set `ANDROID_SDK_ROOT` and `ANDROID_SERIAL` for the selected qualification device.
Run this command from the repository root:

```sh
make test-android-camera
```

The test checks camera access, YUV output, JPEG encoding, camera release, and activity resume.
The emulator test also checks a 180-degree display rotation.
The physical Portal test records its fixed display rotation.
The camera session updates the preview transform when the display rotation changes.
Camera release removes the display listener.

The runner exports available JPEG and screenshot files before it removes the qualification application.
This export also occurs after a test failure.
Files go to `android/build/tests/camera-qualification/`.
The runner keeps the original test failure status.

If export fails, the runner keeps the qualification application and its original files on the device.
Copy those files before you remove the application.
The next run stops if the qualification application is still installed.

## Recovery tests

Select a dedicated emulator through `ANDROID_SERIAL`.
Run this command from the repository root:

```sh
make test-android-camera-recovery
```

These tests use the real camera qualification flow.
An ADB wrapper injects a qualification failure, an export failure, or both failures.
The tests check the exported files, retained device files, application removal, and failure status.
