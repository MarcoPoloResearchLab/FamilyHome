# Photo Booth camera controls

## Physical result

The camera investigation used the connected Portal on 2026-09-06.
The device runs Android 9.
The qualification application used ordinary Android camera access.

| Item | Observed value |
| --- | --- |
| Public camera ID | `0` |
| Active image area | 1280 by 720 pixels |
| Maximum digital zoom | 8.0 |
| Crop type | Center only |
| Camera face-detection modes | Off only |
| Maximum camera face count | 0 |
| Public crop request | `android.scaler.cropRegion` |

These values describe reported camera support. This investigation did not qualify digital zoom output.
The camera qualification passed 20 capture, release, and reopen cycles before the report.
Run `make test-android-camera` to repeat the camera checks and retrieve the reported controls.

## Portal tracking service

The installed `com.facebook.alohasdk.virtualcameramanager` package exposes `VirtualCameraManagerService`.
That service requires `android.permission.CAMERA_PRIV`.
The permission has `signature|privileged|preinstalled` protection on this Portal.
FamilyHome has ordinary camera permission and cannot bind to that protected service.
The system also reports a camera client for `com.facebook.portal.aiservice`.
That system client does not expose face positions through the public camera results.

Meta describes Smart Camera as local camera processing.
Its current sample application uses a standard CameraX preview with camera selection.
The reviewed camera sample has no Spotlight command or person-selection interface.

## Application controls

Photo Booth uses a local image crop for both camera preview and saved pictures.
This approach permits an off-center crop without the protected Portal service.
Zoom choices are 1×, 1.5×, 2×, and 3×.
A higher zoom produces fewer source pixels. The application does not enlarge the saved picture to conceal this loss.

1. Open Photo Booth.
2. Step into the picture.
3. If several persons appear, tap your face inside its yellow rectangle.
4. Select Take picture.

Photo Booth follows one visible person automatically with a maximum zoom of 2×.
The crop includes space above the detected face for the head and hair.
When the person moves closer, the crop immediately expands to include the head and shoulders.
The crop contracts gradually when the person moves farther away.
Several faces produce a full camera image with rectangles for touch selection.
After a tap, Photo Booth follows the selected person while the other persons remain visible.
The optional Zoom menu changes the maximum zoom. The head can require a wider crop than this setting.
Person tracking requires no setup menu.

After tracking loss, Photo Booth automatically detects persons again.
One returning person starts person tracking. Several returning persons produce rectangles for touch selection.

The preview reflects the scene. Saved pictures keep normal text direction.
Each picture in a strip uses the crop visible at its own shutter event.
Different crop dimensions use a common output size without additional cropping or enlargement.

Face detection uses the bundled YuNet model through OpenCV 4.12.0.
The model detects face boxes and eye landmarks at a maximum input dimension of 320 pixels.
Person tracking uses the full camera image before the image crop or photo filter.
Zoom changes do not change the coordinates used for person tracking.
Optical flow follows points around the selected head and upper body between face detections.
The detector runs every fourth frame and when optical flow has no points.
Face measurements correct the tracked position and size.
The image crop uses face-box dimensions instead of eye spacing, which changes during a head turn.
The model and native libraries work without a network connection or Google Play services.
Tracking uses temporary face positions and sizes. It does not identify persons or store face measurements.

A head turn can preserve person tracking when optical flow supplies a valid motion measurement.
A gap without face or motion measurements holds the last crop for up to 800 milliseconds.
Capture remains unavailable during that gap.
A longer gap, stale measurement, or ambiguous face match stops the current person tracking and restarts detection.
The application cancels a countdown when the selected person disappears.
A captured picture must match the selected face or the tracked image points.

Rapid movement, a face turned fully away, poor light, or overlapping faces can interrupt tracking.
These position and motion measurements cannot establish identity when persons exchange positions between camera samples.
Physical acceptance must include motion and a second person. Controlled images do not prove these conditions on the Portal.

## Sources

- [Meta Portal application guide](https://developers.meta.com/horizon/documentation/android-apps/portal-create-app/).
- [Meta camera sample](https://github.com/meta-quest/portal-samples/blob/main/app/src/main/java/com/meta/portal/sampleapp/CameraSection.kt).
- [Meta Smart Camera description](https://about.fb.com/news/2018/11/portal-privacy-and-ads/).
- [Android camera characteristics](https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics).
- [YuNet model and license](https://github.com/opencv/opencv_zoo/tree/47534e27c9851bb1128ccc0102f1145e27f23f98/models/face_detection_yunet).
- [OpenCV face detector API](https://docs.opencv.org/4.12.0/javadoc/org/opencv/objdetect/FaceDetectorYN.html).
