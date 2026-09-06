package com.mprlab.portal;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.TextureView;
import android.view.Surface;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;

public final class CameraQualificationTest extends Instrumentation {
    private Activity activity;
    private String phase;
    private boolean checkRotation;
    private String resultDetails = "";
    @Override public void onCreate(Bundle args) { super.onCreate(args); phase = args.getString("phase", "capture"); checkRotation = args.getString("rotation", "dynamic").equals("dynamic"); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            getTargetContext().getFilesDir();
            activity = startActivitySync(new Intent().setClassName(getTargetContext().getPackageName(),
                    "com.mprlab.portal.CameraQualificationActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            if (phase.equals("permission")) {
                awaitStatus("Camera permission required");
                requireDisabled("Take picture");
                resultDetails = "Permission denial leaves capture unavailable.";
            } else {
                for (int cycle = 0; cycle < 20; cycle++) {
                    resultDetails += "\nCycle " + cycle;
                    awaitStatus("Preview ready");
                    click("Take picture");
                    awaitStatus("JPEG ready " + (cycle + 1) + " · ");
                    Bitmap jpeg = BitmapFactory.decodeFile(new File(getTargetContext().getFilesDir(), "camera-qualification.jpg").getPath());
                    if (jpeg == null || jpeg.getWidth() > 1600 || jpeg.getHeight() > 1600)
                        throw new AssertionError("Camera JPEG cannot be decoded within the image size limit");
                    jpeg.recycle();
                    if (cycle == 0) {
                        resultDetails = text("Camera details") + "\n" + text("Camera status");
                        try (FileOutputStream file = new FileOutputStream(new File(getTargetContext().getFilesDir(), "camera-qualification.png"))) {
                            Bitmap screenshot = getUiAutomation().takeScreenshot();
                            if (!screenshot.compress(Bitmap.CompressFormat.PNG, 100, file)) throw new AssertionError("Screenshot encoding failed");
                            screenshot.recycle();
                        }
                    }
                    click("Release camera"); awaitStatus("Camera released"); requireDisabled("Take picture");
                    if (cycle < 19) click("Open camera");
                }
                // A pause while opening must close the candidate adapter without a late preview callback.
                runOnMainSync(() -> { find(activity.getWindow().getDecorView(), "Open camera").performClick(); activity.finish(); });
                waitForIdleSync();
                SystemClock.sleep(500);
                activity = startActivitySync(new Intent().setClassName(getTargetContext().getPackageName(),
                        "com.mprlab.portal.CameraQualificationActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                awaitStatus("Preview ready");
                resultDetails += "\nHome pause check";
                runOnMainSync(() -> activity.startActivity(new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
                awaitStatus("Camera released");
                runOnMainSync(() -> getTargetContext().startActivity(new Intent().setClassName(getTargetContext().getPackageName(),
                        "com.mprlab.portal.CameraQualificationActivity")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));
                awaitStatus("Preview ready");
                if (checkRotation) verifyHalfTurn();
                else resultDetails += "\nPhysical Portal uses its fixed display rotation.";
                resultDetails += "\n20 preview/JPEG/release/reopen cycles, pause during open, and resume passed.";
                resultDetails += cameraControls();
            }
            runOnMainSync(activity::finish);
            result.putString("stream", "Camera qualification passed: " + resultDetails + "\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "Camera qualification failed: " + error + "\n" + resultDetails + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private String cameraControls() throws Exception {
        android.hardware.camera2.CameraManager manager = getTargetContext().getSystemService(android.hardware.camera2.CameraManager.class);
        StringBuilder details = new StringBuilder();
        for (String id : manager.getCameraIdList()) {
            android.hardware.camera2.CameraCharacteristics c = manager.getCameraCharacteristics(id);
            details.append("\nCamera ").append(id)
                    .append(" active array=").append(c.get(android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE))
                    .append(" max zoom=").append(c.get(android.hardware.camera2.CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM))
                    .append(" crop type=").append(c.get(android.hardware.camera2.CameraCharacteristics.SCALER_CROPPING_TYPE))
                    .append(" face modes=").append(java.util.Arrays.toString(c.get(android.hardware.camera2.CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES)))
                    .append(" max faces=").append(c.get(android.hardware.camera2.CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT));
            for (android.hardware.camera2.CaptureRequest.Key<?> key : c.getAvailableCaptureRequestKeys()) {
                String name = key.getName();
                if (name.contains("facebook") || name.contains("zoom") || name.contains("crop") || name.contains("face"))
                    details.append("\nRequest key ").append(name);
            }
        }
        return details.toString();
    }
    private void verifyHalfTurn() {
        try {
            getUiAutomation().setRotation(UiAutomation.ROTATION_FREEZE_0);
            awaitRotation(Surface.ROTATION_0);
            float[] before = previewAxis();
            getUiAutomation().setRotation(UiAutomation.ROTATION_FREEZE_180);
            awaitRotation(Surface.ROTATION_180);
            long deadline = SystemClock.uptimeMillis() + 10000;
            do {
                float[] after = previewAxis();
                if (Math.abs(before[0] + after[0]) < .001f
                        && Math.abs(before[1] + after[1]) < .001f) {
                    resultDetails += "\nPreview follows a 180-degree display rotation.";
                    return;
                }
                SystemClock.sleep(50);
            } while (SystemClock.uptimeMillis() < deadline);
            throw new AssertionError("Preview transform did not follow the 180-degree display rotation");
        } finally {
            getUiAutomation().setRotation(UiAutomation.ROTATION_UNFREEZE);
        }
    }
    private void awaitRotation(int rotation) {
        long deadline = SystemClock.uptimeMillis() + 10000;
        do {
            int[] current = new int[1];
            runOnMainSync(() -> current[0] = activity.getWindow().getDecorView().getDisplay().getRotation());
            if (current[0] == rotation) { waitForIdleSync(); return; }
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Display did not rotate to " + rotation);
    }
    private float[] previewAxis() {
        float[] axis = {1f, 0f};
        runOnMainSync(() -> {
            TextureView preview = (TextureView) find(activity.getWindow().getDecorView(), "Camera preview");
            Matrix transform = preview.getTransform(new Matrix());
            transform.mapVectors(axis);
        });
        return axis;
    }
    private void awaitStatus(String prefix) {
        long deadline = SystemClock.uptimeMillis() + 10000;
        String last;
        do {
            last = text("Camera status");
            if (last.startsWith(prefix)) return;
            if (last.startsWith("Camera error:")) throw new AssertionError(last);
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Expected " + prefix + ", got " + last);
    }
    private String text(String label) {
        String[] result = new String[1];
        runOnMainSync(() -> result[0] = ((TextView) find(activity.getWindow().getDecorView(), label)).getText().toString());
        return result[0];
    }
    private void click(String label) { runOnMainSync(() -> find(activity.getWindow().getDecorView(), label).performClick()); waitForIdleSync(); }
    private void requireDisabled(String label) {
        boolean[] enabled = new boolean[1];
        runOnMainSync(() -> enabled[0] = find(activity.getWindow().getDecorView(), label).isEnabled());
        if (enabled[0]) throw new AssertionError(label + " is enabled without a camera session");
    }
    private View find(View view, String label) {
        if (label.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = find(group.getChildAt(i), label); if (found != null) return found;
            }
        }
        return null;
    }
}
