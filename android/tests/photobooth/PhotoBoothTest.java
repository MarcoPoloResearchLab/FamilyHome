package com.mprlab.portal.photoboothtest;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.KeyEvent;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;
import java.io.File;
import java.util.List;
import android.view.View;
import android.view.ViewGroup;

public final class PhotoBoothTest extends Instrumentation {
    private String phase;
    @Override public void onCreate(Bundle args) { super.onCreate(args); phase = args.getString("phase", "flow"); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            if (phase.equals("effects")) {
                java.lang.reflect.Method initialize = Class.forName("com.mprlab.portal.PhotoFaces").getDeclaredMethod("initialize", Context.class);
                initialize.setAccessible(true); initialize.invoke(null, getTargetContext());
                verifyAccessoryRenderer();
                result.putString("stream", "PhotoBooth effects passed: native face detection and all three accessories.\n"); finish(Activity.RESULT_OK, result); return;
            }
            if (phase.equals("persistence")) { verifyPersistence(); result.putString("stream", "PhotoBooth passed: restart, upgrade, second profile, deletion, and capacity.\n"); finish(Activity.RESULT_OK, result); return; }
            getTargetContext().getSharedPreferences("children_portal", Context.MODE_PRIVATE).edit()
                    .putString("profiles_json", "[{\"id\":\"photo-test\",\"name\":\"Photo Test\"}]")
                    .putString("active_profile_id", "photo-test").commit();
            Activity home = startActivitySync(new Intent().setClassName("com.mprlab.portal", "com.mprlab.portal.MainActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            ActivityMonitor monitor = addMonitor("com.mprlab.portal.PhotoBoothActivity", null, false);
            View[] tile = new View[1];
            runOnMainSync(() -> tile[0] = find(home.getWindow().getDecorView(), "Photo Booth. Take a picture"));
            if (tile[0] == null) throw new AssertionError("Missing control: Photo Booth. Take a picture");
            runOnMainSync(() -> tile[0].performClick());
            Activity booth = waitForMonitorWithTimeout(monitor, 4000);
            removeMonitor(monitor);
            if (booth == null) throw new AssertionError("Photo Booth did not open");
            await(booth, "Photo status", "Camera permission required");
            getUiAutomation().grantRuntimePermission("com.mprlab.portal", "android.permission.CAMERA");
            runOnMainSync(booth::finish); waitForIdleSync();
            booth = startActivitySync(new Intent().setClassName("com.mprlab.portal", "com.mprlab.portal.PhotoBoothActivity")
                    .putExtra("profile_id", "photo-test").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            await(booth, "Photo status", "Ready");
            verifyAccessoryRenderer();
            if (phase.equals("headroom")) {
                Class.forName("com.mprlab.portal.FramingContract").getMethod("verifyHeadroom", Instrumentation.class, Activity.class).invoke(null, this, booth);
                result.putString("stream", "PhotoBooth headroom passed: close portrait and adaptive strip crops.\n");
                finish(Activity.RESULT_OK, result); return;
            }
            Class.forName("com.mprlab.portal.FramingContract").getMethod("verifyPanel", Instrumentation.class, Activity.class).invoke(null, this, booth);
            if (phase.equals("panel")) {
                result.putString("stream", "PhotoBooth panel passed: visual options, compact rows, and filter navigation.\n");
                finish(Activity.RESULT_OK, result); return;
            }
            if (phase.equals("head_turn")) {
                Class.forName("com.mprlab.portal.FramingContract").getMethod("verifyHeadTurn", Instrumentation.class, Activity.class).invoke(null, this, booth);
                result.putString("stream", "PhotoBooth head turn passed: face selection in the captured head-turn scene.\n"); finish(Activity.RESULT_OK, result); return;
            }
            Class.forName("com.mprlab.portal.FramingContract").getMethod("verify", Instrumentation.class, Activity.class).invoke(null, this, booth);
            if (phase.equals("framing")) {
                result.putString("stream", "PhotoBooth framing passed: automatic tracking, group rectangles, touch selection, and zoom.\n");
                finish(Activity.RESULT_OK, result); return;
            }
            click(booth, "Filters");
            for (String filter : new String[]{"Big nose", "Stretch", "Bunny ears", "Googly eyes", "Party hat", "Mirror twins"}) {
                click(booth, filter);
                await(booth, "Filter status", filter.equals("Bunny ears") || filter.equals("Googly eyes") || filter.equals("Party hat")
                        ? "Face the camera" : filter + " · live");
                verifyLivePreview(booth);
            }
            click(booth, "Back to photo options");
            snapshot("photobooth-filter.png");
            click(booth, "Confetti");
            click(booth, "Take picture");
            await(booth, "Photo status", "3");
            await(booth, "Photo status", "Review");
            click(booth, "Retake"); await(booth, "Photo status", "Ready");
            click(booth, "Take picture"); await(booth, "Photo status", "Review");
            click(booth, "Discard"); await(booth, "Photo status", "Ready");
            click(booth, "Take picture"); await(booth, "Photo status", "Review");
            screensaver(booth);
            await(booth, "Photo status", "Review");
            snapshot("photobooth-review.png");
            Activity savingBooth = booth;
            runOnMainSync(() -> { View save = find(savingBooth.getWindow().getDecorView(), "Save"); save.performClick(); save.performClick(); });
            await(booth, "Photo status", "Saved");
            click(booth, "Album");
            await(booth, "Photo status", "1 picture");
            click(booth, "New picture");
            await(booth, "Photo status", "Ready");
            click(booth, "Take picture");
            await(booth, "Photo status", "3");
            screensaver(booth);
            await(booth, "Photo status", "Ready");
            File temporary = new File(getTargetContext().getFilesDir(), "photos/temporary");
            if (temporary.list().length != 0) throw new AssertionError("Cancelled capture retained temporary files");
            snapshot("photobooth-preview.png");
            click(booth, "Four pictures");
            click(booth, "Stars");
            click(booth, "Take picture");
            await(booth, "Photo status", "Review");
            click(booth, "Save");
            await(booth, "Photo status", "Saved");
            click(booth, "Album");
            await(booth, "Photo status", "2 pictures");
            snapshot("photobooth-album.png");
            verifyImages();
            result.putString("stream", "PhotoBooth passed: permission, capture, save, album, and four-picture strip.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            android.util.Log.e("PhotoBoothTest", "Qualification failed", error);
            if (error instanceof java.lang.reflect.InvocationTargetException) error = error.getCause();
            result.putString("stream", "PhotoBooth failed: " + error + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private void verifyPersistence() throws Exception {
        Activity booth = startActivitySync(new Intent().setClassName("com.mprlab.portal", "com.mprlab.portal.PhotoBoothActivity")
                .putExtra("profile_id", "photo-test").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        await(booth, "Photo status", "Ready");
        click(booth, "Album"); await(booth, "Photo status", "2 pictures"); verifyImages();
        runOnMainSync(booth::finish); waitForIdleSync();
        getTargetContext().getSharedPreferences("children_portal", Context.MODE_PRIVATE).edit()
                .putString("profiles_json", "[{\"id\":\"photo-test\",\"name\":\"Photo Test\"},{\"id\":\"other-child\",\"name\":\"Other Child\"}]").commit();
        booth = startActivitySync(new Intent().setClassName("com.mprlab.portal", "com.mprlab.portal.PhotoBoothActivity")
                .putExtra("profile_id", "other-child").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        await(booth, "Photo status", "Ready"); click(booth, "Album"); await(booth, "Photo status", "0 pictures");
        runOnMainSync(booth::finish); waitForIdleSync();
        booth = startActivitySync(new Intent().setClassName("com.mprlab.portal", "com.mprlab.portal.PhotoBoothActivity")
                .putExtra("profile_id", "photo-test").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        await(booth, "Photo status", "Ready"); click(booth, "Album"); await(booth, "Photo status", "2 pictures");
        File[] entries = new File(getTargetContext().getFilesDir(), "photos").listFiles(file -> !file.getName().equals("temporary"));
        click(booth, "Open photo " + entries[0].getName());
        click(booth, "Delete");
        getUiAutomation().waitForIdle(100, 3000);
        // Use the visible confirmation dialog through accessibility, including actual touch dispatch.
        android.view.accessibility.AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
        List<android.view.accessibility.AccessibilityNodeInfo> matches = root.findAccessibilityNodeInfosByText("Delete picture");
        if (matches.isEmpty()) throw new AssertionError("Missing photo deletion confirmation");
        matches.get(0).performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
        await(booth, "Photo status", "1 picture");
        click(booth, "New picture"); await(booth, "Photo status", "Ready");
        // Fill the real album with current-schema records to reach the public capacity boundary.
        File remaining = new File(getTargetContext().getFilesDir(), "photos").listFiles(file -> !file.getName().equals("temporary"))[0];
        JSONObject original = new JSONObject(new String(Files.readAllBytes(new File(remaining, "photo.json").toPath()), StandardCharsets.UTF_8));
        for (int index = 1; index < 100; index++) {
            String id = java.util.UUID.randomUUID().toString(); File copy = new File(remaining.getParentFile(), id); copy.mkdir();
            Files.copy(new File(remaining, "picture.jpg").toPath(), new File(copy, "picture.jpg").toPath());
            Files.copy(new File(remaining, "thumbnail.jpg").toPath(), new File(copy, "thumbnail.jpg").toPath());
            original.put("id", id); Files.write(new File(copy, "photo.json").toPath(), original.toString().getBytes(StandardCharsets.UTF_8));
        }
        click(booth, "Take picture"); await(booth, "Photo status", "Error: Photo album is full");
        for (File entry : remaining.getParentFile().listFiles()) {
            if (entry.equals(remaining) || entry.getName().equals("temporary")) continue;
            for (File file : entry.listFiles()) if (!file.delete()) throw new AssertionError("Remove capacity fixture failed");
            if (!entry.delete()) throw new AssertionError("Remove capacity fixture directory failed");
        }
        File savedImage = new File(remaining, "picture.jpg");
        long originalLength = savedImage.length();
        JSONObject large = new JSONObject(new String(Files.readAllBytes(new File(remaining, "photo.json").toPath()), StandardCharsets.UTF_8));
        try (java.io.RandomAccessFile file = new java.io.RandomAccessFile(savedImage, "rw")) { file.setLength(256L * 1024 * 1024); }
        large.put("bytes", savedImage.length() + new File(remaining, "thumbnail.jpg").length());
        Files.write(new File(remaining, "photo.json").toPath(), large.toString().getBytes(StandardCharsets.UTF_8));
        click(booth, "Try again"); await(booth, "Photo status", "Ready");
        click(booth, "Take picture"); await(booth, "Photo status", "Error: Photo album is full");
        try (java.io.RandomAccessFile file = new java.io.RandomAccessFile(savedImage, "rw")) { file.setLength(originalLength); }
        large.put("bytes", savedImage.length() + new File(remaining, "thumbnail.jpg").length());
        Files.write(new File(remaining, "photo.json").toPath(), large.toString().getBytes(StandardCharsets.UTF_8));
        // An actual filesystem collision exercises write failure without substituting storage logic.
        File temporary = new File(remaining.getParentFile(), "temporary");
        if (!temporary.delete() || !temporary.createNewFile()) throw new AssertionError("Create filesystem fault fixture failed");
        click(booth, "Try again"); await(booth, "Photo status", "Error: Read photo directory failed");
    }
    private void verifyImages() throws Exception {
        File root = new File(getTargetContext().getFilesDir(), "photos"); int count = 0; boolean strip = false;
        for (File entry : root.listFiles()) {
            if (entry.getName().equals("temporary")) { if (entry.list().length != 0) throw new AssertionError("Temporary pictures remain after Save"); continue; }
            JSONObject metadata = new JSONObject(new String(Files.readAllBytes(new File(entry, "photo.json").toPath()), StandardCharsets.UTF_8));
            Bitmap bitmap = BitmapFactory.decodeFile(new File(entry, "picture.jpg").getPath());
            if (bitmap == null) throw new AssertionError("Saved JPEG is invalid");
            if (!metadata.getString("profile_id").equals("photo-test")) throw new AssertionError("Wrong photo owner");
            if (metadata.getString("kind").equals("STRIP")) {
                strip = true;
                if (bitmap.getHeight() <= bitmap.getWidth() || bitmap.getHeight() > 3600 || bitmap.getWidth() > 1200)
                    throw new AssertionError("Photo strip dimensions are invalid");
                if (!metadata.getString("frame_id").equals("STARS")) throw new AssertionError("Frame selection was lost");
            } else if (Math.max(bitmap.getWidth(), bitmap.getHeight()) > 1600) throw new AssertionError("Single picture is oversized");
            // Every saved cell must retain the mirror effect after rendering, framing, and JPEG encoding.
            long difference = 0, samples = 0;
            for (int y = 50; y < bitmap.getHeight() - 50; y += 11) {
                for (int x = 40; x < bitmap.getWidth() / 2 - 5; x += 13) {
                    int a = bitmap.getPixel(x, y), b = bitmap.getPixel(bitmap.getWidth() - 1 - x, y);
                    difference += Math.abs(((a >> 16) & 255) - ((b >> 16) & 255))
                            + Math.abs(((a >> 8) & 255) - ((b >> 8) & 255)) + Math.abs((a & 255) - (b & 255));
                    samples += 3;
                }
            }
            if (difference / samples > 14) throw new AssertionError("Saved photo lost the mirror filter: " + difference / samples);
            bitmap.recycle(); count++;
        }
        if (count != 2 || !strip) throw new AssertionError("Expected one photo and one strip");
    }
    private void verifyAccessoryRenderer() throws Exception {
        // Focused native algorithm coverage supplements the real camera and UI flow.
        Bitmap source;
        try (java.io.InputStream input = getContext().getAssets().open("face.gif")) { source = BitmapFactory.decodeStream(input); }
        if (source == null) throw new AssertionError("Face fixture did not decode");
        Class<?> effects = Class.forName("com.mprlab.portal.PhotoEffects");
        Class<?> effect = Class.forName("com.mprlab.portal.PhotoEffects$Effect");
        java.lang.reflect.Method apply = effects.getDeclaredMethod("apply", Bitmap.class, effect); apply.setAccessible(true);
        for (Object choice : effect.getEnumConstants()) {
            String name = ((Enum<?>) choice).name();
            if (!name.equals("BUNNY") && !name.equals("GOOGLY") && !name.equals("PARTY")) continue;
            Object result = apply.invoke(null, source, choice);
            java.lang.reflect.Field faces = result.getClass().getDeclaredField("faces"); faces.setAccessible(true);
            java.lang.reflect.Field image = result.getClass().getDeclaredField("image"); image.setAccessible(true);
            Bitmap output = (Bitmap) image.get(result);
            try {
                if (faces.getInt(result) != 1) throw new AssertionError("Accessory face detection failed: " + name);
                int changed = 0;
                for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++)
                    if (source.getPixel(x, y) != output.getPixel(x, y)) changed++;
                if (changed < 50 || changed > source.getWidth() * source.getHeight() / 2)
                    throw new AssertionError("Accessory pixels are missing or misplaced: " + name + " " + changed);
                if (!phase.equals("effects")) try (FileOutputStream file = new FileOutputStream(new File(getTargetContext().getFilesDir(), "effect-" + name + ".png"))) {
                    output.compress(Bitmap.CompressFormat.PNG, 100, file);
                }
            } finally { output.recycle(); }
        }
        source.recycle();
    }
    private void verifyLivePreview(Activity booth) {
        boolean[] visible = {false};
        runOnMainSync(() -> {
            View view = find(booth.getWindow().getDecorView(), "Filtered camera preview");
            if (view instanceof android.widget.ImageView) {
                android.graphics.drawable.Drawable drawable = ((android.widget.ImageView) view).getDrawable();
                visible[0] = view.getVisibility() == View.VISIBLE && drawable instanceof android.graphics.drawable.BitmapDrawable
                        && !((android.graphics.drawable.BitmapDrawable) drawable).getBitmap().isRecycled();
            }
        });
        if (!visible[0]) throw new AssertionError("Live filter preview is missing");
    }
    private void screensaver(Activity booth) throws Exception {
        java.lang.reflect.Method show = booth.getClass().getSuperclass().getDeclaredMethod("showScreensaver"); show.setAccessible(true);
        runOnMainSync(() -> { try { show.invoke(booth); } catch (Exception error) { throw new AssertionError(error); } });
        waitForIdleSync();
        sendKeyDownUpSync(KeyEvent.KEYCODE_SPACE); waitForIdleSync();
    }
    private void snapshot(String name) throws Exception {
        try (FileOutputStream out = new FileOutputStream(new File(getTargetContext().getFilesDir(), name))) {
            Bitmap bitmap = getUiAutomation().takeScreenshot(); bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); bitmap.recycle();
        }
    }
    private void click(Activity activity, String label) {
        runOnMainSync(() -> {
            View view = find(activity.getWindow().getDecorView(), label);
            if (view == null || !view.isEnabled()) throw new AssertionError("Missing enabled control: " + label);
            view.performClick();
        });
        waitForIdleSync();
    }
    private void await(Activity activity, String label, String prefix) {
        long deadline = SystemClock.uptimeMillis() + 25000;
        String[] last = {""};
        do {
            runOnMainSync(() -> {
                View view = find(activity.getWindow().getDecorView(), label);
                last[0] = view instanceof TextView ? ((TextView) view).getText().toString() : "Missing " + label;
            });
            if (last[0].startsWith(prefix)) return;
            if (last[0].startsWith("Error:") && !prefix.startsWith("Error:")) throw new AssertionError(last[0]);
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Expected " + prefix + ", got " + last[0]);
    }
    private View find(View view, String label) {
        if (label.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = find(group.getChildAt(i), label);
                if (found != null) return found;
            }
        }
        return null;
    }
}
