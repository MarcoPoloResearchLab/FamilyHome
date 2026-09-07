package com.mprlab.portal;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Controlled camera frames exercise the real Activity, detector, tracker, and touch transforms. */
public final class FramingContract {
    public static void verifyHeadroom(Instrumentation test, Activity booth) throws Exception {
        Bitmap portrait;
        try (java.io.InputStream input = test.getContext().getAssets().open("face.gif")) { portrait = BitmapFactory.decodeStream(input); }
        Bitmap large = Bitmap.createScaledBitmap(portrait, portrait.getWidth() * 2, portrait.getHeight() * 2, true);
        Bitmap scene = Bitmap.createBitmap(800, 500, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scene); canvas.drawColor(0xffb5b7bb); canvas.drawBitmap(large, 150, 0, null);
        portrait.recycle(); large.recycle();
        require(PhotoFaces.find(scene).size() == 1, "Close portrait must have one detected face");
        Field source = PhotoBoothActivity.class.getDeclaredField("frameSource"); source.setAccessible(true);
        Field displayed = PhotoBoothActivity.class.getDeclaredField("displayedFrame"); displayed.setAccessible(true);
        Object[] original = new Object[1];
        test.runOnMainSync(() -> { try {
            original[0] = source.get(booth);
            source.set(booth, (PhotoBoothActivity.FrameSource) callback -> callback.received(scene.copy(Bitmap.Config.ARGB_8888, true)));
        } catch (Exception failure) { throw new AssertionError(failure); } });
        try {
            await(test, booth, "Smile!");
            click(test, booth, "Zoom"); click(test, booth, "3×"); await(test, booth, "Smile!");
            click(test, booth, "Back to photo options");
            PhotoFraming.Crop[] crop = new PhotoFraming.Crop[1];
            test.runOnMainSync(() -> { try {
                PhotoFraming.Frame current = (PhotoFraming.Frame) displayed.get(booth);
                crop[0] = current.crop;
                Rect visible = current.crop.pixels(800, 500);
                // The fixture's hair begins at source y=56, well above the face detector's brow box.
                require(visible.top <= 56, "Zoom cuts the top of the head: crop begins at " + visible.top);
                require(visible.bottom >= (current.target.y + current.target.height * .65f) * 500, "Zoom must leave room below the chin");
            } catch (IllegalAccessException failure) { throw new AssertionError(failure); } });
            snapshot(test, "headroom");
            File jpeg = new File(test.getTargetContext().getCacheDir(), "headroom.jpg");
            try {
                try (FileOutputStream out = new FileOutputStream(jpeg)) { scene.compress(Bitmap.CompressFormat.JPEG, 100, out); }
                List<PhotoRenderer.Capture> shots = Arrays.asList(new PhotoRenderer.Capture(jpeg, crop[0]),
                        new PhotoRenderer.Capture(jpeg, PhotoFraming.Crop.FULL),
                        new PhotoRenderer.Capture(jpeg, new PhotoFraming.Crop(.5f, .5f, PhotoFraming.Zoom.NEAR)),
                        new PhotoRenderer.Capture(jpeg, crop[0]));
                Bitmap strip = PhotoRenderer.render(shots, PhotoRenderer.Kind.STRIP, PhotoRenderer.Frame.NONE, PhotoEffects.Effect.NORMAL);
                require(strip.getHeight() % 4 == 0, "Adaptive strip must retain four complete pictures"); strip.recycle();
            } finally { require(jpeg.delete(), "Remove headroom fixture"); }
        } finally {
            test.runOnMainSync(() -> { try { source.set(booth, original[0]); } catch (Exception failure) { throw new AssertionError(failure); } });
            scene.recycle();
        }
    }
    public static void verifyPanel(Instrumentation test, Activity booth) throws Exception {
        test.runOnMainSync(() -> {
            View root = booth.getWindow().getDecorView();
            Rect single = bounds(find(root, "One picture")), strip = bounds(find(root, "Four pictures"));
            require(single.top == strip.top && single.right < strip.left, "Picture modes must share one row");
            for (String label : new String[]{"One picture", "Four pictures", "None", "Stars", "Confetti"}) {
                android.widget.Button button = (android.widget.Button) find(root, label);
                require(button.getCompoundDrawables()[1] != null, "Visual option needs an icon: " + label);
                bounds(button);
            }
            Rect none = bounds(find(root, "None")), stars = bounds(find(root, "Stars")), confetti = bounds(find(root, "Confetti"));
            require(none.top == stars.top && stars.top == confetti.top && none.right < stars.left && stars.right < confetti.left,
                    "Frame previews must share one separated row");
            bounds(find(root, "Filters")); bounds(find(root, "Zoom")); bounds(find(root, "Take picture"));
        });
        click(test, booth, "Four pictures"); click(test, booth, "Stars");
        snapshot(test, "options");
        click(test, booth, "Filters");
        test.waitForIdleSync();
        test.runOnMainSync(() -> {
            bounds(find(booth.getWindow().getDecorView(), "Back to photo options"));
            bounds(find(booth.getWindow().getDecorView(), "Take picture"));
        });
        click(test, booth, "Big nose"); awaitFilter(test, booth, "Big nose");
        snapshot(test, "filters-panel");
        test.runOnMainSync(() -> scroll(booth.getWindow().getDecorView()).fullScroll(View.FOCUS_DOWN));
        test.waitForIdleSync();
        test.runOnMainSync(() -> {
            bounds(find(booth.getWindow().getDecorView(), "Back to photo options"));
            bounds(find(booth.getWindow().getDecorView(), "Take picture"));
        });
        click(test, booth, "Back to photo options");
        test.runOnMainSync(() -> {
            View root = booth.getWindow().getDecorView();
            require(find(root, "Four pictures").isSelected() && find(root, "Stars").isSelected(), "Panel navigation must preserve choices");
        });
        click(test, booth, "Filters");
        test.runOnMainSync(booth::onBackPressed); test.waitForIdleSync();
        test.runOnMainSync(() -> require(find(booth.getWindow().getDecorView(), "One picture") != null && !booth.isFinishing(), "Back must return to options without leaving Photo Booth"));
        click(test, booth, "Filters"); click(test, booth, "Original"); click(test, booth, "Back to photo options");
        click(test, booth, "One picture"); click(test, booth, "None");
    }
    private static Rect bounds(View view) {
        require(view != null, "Missing panel control");
        Rect visible = new Rect();
        require(view.getGlobalVisibleRect(visible) && visible.width() == view.getWidth() && visible.height() == view.getHeight(), "Panel control must be fully visible: " + view.getContentDescription());
        return visible;
    }
    private static android.widget.ScrollView scroll(View view) {
        if (view instanceof android.widget.ScrollView) return (android.widget.ScrollView) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            android.widget.ScrollView found = scroll(((ViewGroup) view).getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }
    private static void awaitFilter(Instrumentation test, Activity booth, String prefix) {
        long end = SystemClock.uptimeMillis() + 8000;
        boolean[] ready = {false};
        do {
            test.runOnMainSync(() -> ready[0] = ((TextView) find(booth.getWindow().getDecorView(), "Filter status")).getText().toString().startsWith(prefix));
            if (ready[0]) return;
            SystemClock.sleep(40);
        } while (SystemClock.uptimeMillis() < end);
        throw new AssertionError("Filter preview did not update");
    }
    public static void verifyHeadTurn(Instrumentation test, Activity booth) throws Exception {
        Bitmap screen;
        try (java.io.InputStream input = test.getContext().getAssets().open("head-turn.png")) { screen = BitmapFactory.decodeStream(input); }
        Bitmap scene = Bitmap.createBitmap(screen, 20, 157, 970, 546);
        screen.recycle();
        Field field = PhotoBoothActivity.class.getDeclaredField("frameSource"); field.setAccessible(true);
        test.runOnMainSync(() -> { try { field.set(booth, (PhotoBoothActivity.FrameSource) callback -> callback.received(scene.copy(Bitmap.Config.ARGB_8888, true))); } catch (Exception error) { throw new AssertionError(error); } });
        await(test, booth, "Smile!");
        require(PhotoFaces.find(scene).size() == 1, "Head-turn scene must detect one face");
    }
    public static void verify(Instrumentation test, Activity booth) throws Exception {
        Bitmap portrait;
        try (java.io.InputStream input = test.getContext().getAssets().open("face.gif")) { portrait = BitmapFactory.decodeStream(input); }
        Bitmap single = scene(portrait, 50, false), moved = scene(portrait, 75, true), empty = scene(portrait, -1, false);
        Bitmap covered = moved.copy(Bitmap.Config.ARGB_8888, true);
        PhotoFaces.Face coverFace = PhotoFaces.find(moved).stream().min(java.util.Comparator.comparingDouble(f -> f.x)).get();
        Paint cover = new Paint(); cover.setColor(0xff555555);
        new Canvas(covered).drawRect((coverFace.x - coverFace.span * .6f) * 800, (coverFace.y - coverFace.span * 1.2f) * 400,
                (coverFace.x + coverFace.span * .6f) * 800, (coverFace.y + coverFace.span * 1.8f) * 400, cover);
        require(PhotoFaces.find(covered).stream().noneMatch(f -> f.x < .5f), "Occluded subject must have no face detection");
        List<PhotoFaces.Face> detected = PhotoFaces.find(single);
        require(detected.size() == 1, "Controlled frame must have one face");
        require(PhotoFaces.find(moved).size() == 2, "Controlled frame must have two faces");
        AtomicReference<Bitmap> frame = new AtomicReference<>(single);
        Field sourceField = PhotoBoothActivity.class.getDeclaredField("frameSource"); sourceField.setAccessible(true);
        Object[] original = new Object[1];
        test.runOnMainSync(() -> {
            try {
                original[0] = sourceField.get(booth);
                sourceField.set(booth, (PhotoBoothActivity.FrameSource) callback -> callback.received(frame.get().copy(Bitmap.Config.ARGB_8888, true)));
            } catch (Exception failure) { throw new AssertionError(failure); }
        });
        try {
            // No button or menu is needed to follow the only visible person.
            await(test, booth, "Smile!");
            test.runOnMainSync(() -> {
                require(find(booth.getWindow().getDecorView(), "Choose person") == null, "Manual setup must be absent");
                require(find(booth.getWindow().getDecorView(), "Take picture").isEnabled(), "Automatic tracking must enable capture");
            });
            snapshot(test, "automatic-single");
            frame.set(moved);
            await(test, booth, "Tap your face");
            test.runOnMainSync(() -> {
                Bitmap preview = ((android.graphics.drawable.BitmapDrawable) ((ImageView) find(booth.getWindow().getDecorView(), "Filtered camera preview")).getDrawable()).getBitmap();
                require(preview.getWidth() == moved.getWidth(), "Group selection must show the full scene");
                int outlined = 0;
                for (int y = 0; y < preview.getHeight(); y++) for (int x = 0; x < preview.getWidth(); x++)
                    if (preview.getPixel(x, y) == 0xffffcc55) outlined++;
                require(outlined > 100, "Group preview must draw tracking rectangles");
                require(!find(booth.getWindow().getDecorView(), "Take picture").isEnabled(), "Group must wait for a face tap");
                View hint = find(booth.getWindow().getDecorView(), "Framing status");
                Rect visible = new Rect();
                require(hint.getGlobalVisibleRect(visible) && visible.height() == hint.getHeight(), "Tap instruction must stay fully visible above the shutter");
            });
            snapshot(test, "automatic-group");
            tapFace(test, booth, .02f, .02f, 800, 400);
            await(test, booth, "Tap your face");
            List<PhotoFaces.Face> faces = PhotoFaces.find(moved);
            PhotoFaces.Face left = faces.stream().min(java.util.Comparator.comparingDouble(f -> f.x)).get();
            PhotoFaces.Face right = faces.stream().max(java.util.Comparator.comparingDouble(f -> f.x)).get();
            tapFace(test, booth, left.x - left.span * .4f, left.y, 800, 400);
            await(test, booth, "Smile!");
            assertTarget(test, booth, false);
            click(test, booth, "Zoom");
            test.runOnMainSync(() -> require(find(booth.getWindow().getDecorView(), "Choose person") == null, "Zoom must not hide manual tracking setup"));
            for (String zoom : new String[]{"3×", "1.5×", "2×"}) {
                click(test, booth, zoom); await(test, booth, "Smile!");
            }
            click(test, booth, "Back to photo options");
            frame.set(covered);
            long heldUntil = SystemClock.uptimeMillis() + 1500;
            while (SystemClock.uptimeMillis() < heldUntil) {
                await(test, booth, "Smile!"); SystemClock.sleep(80);
            }
            frame.set(empty);
            await(test, booth, "Hold still");
            frame.set(moved);
            await(test, booth, "Smile!");
            click(test, booth, "Take picture");
            frame.set(empty);
            await(test, booth, "Step into the picture");
            // Loss cancels capture and resumes detection without opening a settings menu.
            test.runOnMainSync(() -> require(find(booth.getWindow().getDecorView(), "Take picture") != null, "Loss must return to picture controls"));
            frame.set(moved);
            await(test, booth, "Tap your face");
            tapFace(test, booth, right.x, right.y, 800, 400);
            await(test, booth, "Smile!");
            assertTarget(test, booth, true);
            frame.set(empty); await(test, booth, "Step into the picture");
            frame.set(single); await(test, booth, "Smile!");
            assertTarget(test, booth, false);
            // Empty scenes retain optional manual zoom and still permit ordinary camera pictures.
            frame.set(empty); await(test, booth, "Step into the picture");
            click(test, booth, "Zoom");
            for (String zoom : new String[]{"1×", "1.5×", "3×", "2×"}) {
                click(test, booth, zoom); await(test, booth, "Step into the picture");
                float factor = zoom.equals("1×") ? 1 : zoom.equals("1.5×") ? 1.5f : zoom.equals("3×") ? 3 : 2;
                test.runOnMainSync(() -> {
                    Bitmap preview = ((android.graphics.drawable.BitmapDrawable) ((ImageView) find(booth.getWindow().getDecorView(), "Filtered camera preview")).getDrawable()).getBitmap();
                    require(preview.getWidth() == Math.round(empty.getWidth() / factor), "Preview zoom width mismatch");
                    require(preview.getHeight() == Math.round(empty.getHeight() / factor), "Preview zoom height mismatch");
                    Rect crop = new PhotoFraming.Crop(.5f, .5f, zoom.equals("1×") ? PhotoFraming.Zoom.WIDE : zoom.equals("1.5×") ? PhotoFraming.Zoom.NEAR : zoom.equals("3×") ? PhotoFraming.Zoom.CLOSE : PhotoFraming.Zoom.PORTRAIT).pixels(empty.getWidth(), empty.getHeight());
                    require(preview.getPixel(20, 20) == empty.getPixel(crop.left + 20, crop.top + 20), "Preview crop pixels mismatch");
                });
            }
            click(test, booth, "Back to photo options");
        } finally {
            test.runOnMainSync(() -> { try { sourceField.set(booth, original[0]); } catch (Exception error) { throw new AssertionError(error); } });
            // Requests copy the immutable source synchronously on main, so these fixtures can now be released.
            portrait.recycle(); single.recycle(); moved.recycle(); empty.recycle(); covered.recycle();
        }
        geometryAndStrip(test.getTargetContext());
    }
    private static void assertTarget(Instrumentation test, Activity booth, boolean right) throws Exception {
        Field displayed = PhotoBoothActivity.class.getDeclaredField("displayedFrame"); displayed.setAccessible(true);
        test.runOnMainSync(() -> {
            try {
                PhotoFraming.Frame value = (PhotoFraming.Frame) displayed.get(booth);
                require(value != null && value.target != null && (value.target.x > .5f) == right, "Mirrored preview tracked the wrong person");
            } catch (IllegalAccessException error) { throw new AssertionError(error); }
        });
    }
    private static void snapshot(Instrumentation test, String name) throws Exception {
        test.getUiAutomation().waitForIdle(100, 3000);
        Bitmap bitmap = test.getUiAutomation().takeScreenshot();
        try (FileOutputStream out = new FileOutputStream(new File(test.getTargetContext().getFilesDir(), "photobooth-" + name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } finally { bitmap.recycle(); }
    }
    private static PhotoFraming.Frame update(PhotoFraming tracker, List<PhotoFaces.Face> faces, float aspect, long time) {
        return tracker.update(faces, faces, aspect, time);
    }
    private static Bitmap scene(Bitmap portrait, int left, boolean second) {
        Bitmap result = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result); canvas.drawColor(0xffb5b7bb);
        Paint background = new Paint(); background.setColor(0xff80868d);
        for (int x = 0; x < 800; x += 37) canvas.drawLine(x, 0, x, 400, background);
        for (int y = 0; y < 400; y += 37) canvas.drawLine(0, y, 800, y, background);
        if (left >= 0) canvas.drawBitmap(portrait, left, 70, null);
        if (second) canvas.drawBitmap(portrait, 470, 70, null);
        return result;
    }
    private static void geometryAndStrip(Context context) throws Exception {
        PhotoFraming tracker = new PhotoFraming();
        PhotoFaces.Face face = new PhotoFaces.Face(.3f, .4f, .2f, .3f, .3f, .4f, .08f, 0);
        require(update(tracker, Collections.singletonList(face), 2, 100).mode == PhotoFraming.Mode.FOLLOWING, "One face must start automatically");
        List<PhotoFaces.Face> group = Arrays.asList(face, new PhotoFaces.Face(.7f, .4f, .2f, .3f, .7f, .4f, .08f, 0));
        PhotoFraming.Frame overview = update(tracker, group, 2, 200);
        require(overview.mode == PhotoFraming.Mode.SELECTING && overview.crop == PhotoFraming.Crop.FULL, "Multiple people must show selection boxes");
        tracker.tap(overview, .3f, .4f, 2, 200);
        require(update(tracker, group, 2, 300).mode == PhotoFraming.Mode.LOST, "Ambiguous subject measurement must lose selection");
        require(update(tracker, Collections.singletonList(face), 2, 400).mode == PhotoFraming.Mode.FOLLOWING, "A lone returning person must restart automatically");
        require(update(tracker, Collections.singletonList(face), 2, 1501).mode == PhotoFraming.Mode.LOST, "Stale detection must lose selection");
        update(tracker, Collections.singletonList(face), 2, 2000);
        PhotoFraming.Frame gap = update(tracker, Collections.emptyList(), 2, 2200);
        require(gap.mode == PhotoFraming.Mode.HOLDING && !gap.ready(2200), "Brief gaps must hold framing and block capture");
        require(update(tracker, Collections.singletonList(face), 2, 2400).mode == PhotoFraming.Mode.FOLLOWING, "A brief gap must preserve selection");
        require(update(tracker, Collections.emptyList(), 2, 3300).mode == PhotoFraming.Mode.LOST, "A sustained gap must lose selection");
        require(update(tracker, group, 2, 3400).mode == PhotoFraming.Mode.SELECTING, "A returning group must get tappable boxes automatically");
        Rect edge = new PhotoFraming.Crop(0, 1, PhotoFraming.Zoom.CLOSE).pixels(801, 403);
        require(edge.left == 0 && edge.bottom == 403 && edge.width() == 267, "Crop must clamp at image edges");
        Bitmap source = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < 400; y++) for (int x = 0; x < 800; x++) source.setPixel(x, y, Color.rgb(x / 4, y / 2, 60));
        File jpeg = new File(context.getCacheDir(), "framing-contract.jpg");
        try (FileOutputStream out = new FileOutputStream(jpeg)) { source.compress(Bitmap.CompressFormat.JPEG, 100, out); }
        source.recycle(); source = BitmapFactory.decodeFile(jpeg.getPath());
        List<PhotoRenderer.Capture> captures = new ArrayList<>();
        for (float x : new float[]{.25f, .4f, .6f, .75f}) captures.add(new PhotoRenderer.Capture(jpeg, new PhotoFraming.Crop(x, .5f, PhotoFraming.Zoom.PORTRAIT)));
        Bitmap strip = PhotoRenderer.render(captures, PhotoRenderer.Kind.STRIP, PhotoRenderer.Frame.NONE, PhotoEffects.Effect.NORMAL);
        try {
            require(strip.getWidth() == 400 && strip.getHeight() == 800, "Strip crop dimensions mismatch");
            for (int index = 0; index < 4; index++) {
                Rect crop = captures.get(index).crop.pixels(800, 400);
                require(strip.getPixel(50, index * 200 + 50) == source.getPixel(crop.left + 50, crop.top + 50), "Strip lost a per-picture crop");
            }
        } finally { strip.recycle(); source.recycle(); require(jpeg.delete(), "Remove framing fixture"); }
    }
    private static void tapFace(Instrumentation test, Activity booth, float x, float y, int width, int height) {
        test.runOnMainSync(() -> {
            View view = find(booth.getWindow().getDecorView(), "Filtered camera preview");
            int[] location = new int[2]; ((View) view.getParent()).getLocationOnScreen(location);
            int[] decor = new int[2]; booth.getWindow().getDecorView().getLocationOnScreen(decor);
            float scale = Math.min((float) view.getWidth() / width, (float) view.getHeight() / height);
            float px = location[0] - decor[0] + (view.getWidth() - width * scale) / 2 + (1 - x) * width * scale;
            float py = location[1] - decor[1] + (view.getHeight() - height * scale) / 2 + y * height * scale;
            long now = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, px, py, 0);
            MotionEvent up = MotionEvent.obtain(now, now + 10, MotionEvent.ACTION_UP, px, py, 0);
            booth.dispatchTouchEvent(down); booth.dispatchTouchEvent(up); down.recycle(); up.recycle();
        });
    }
    private static void click(Instrumentation test, Activity booth, String label) {
        test.runOnMainSync(() -> {
            View view = find(booth.getWindow().getDecorView(), label);
            require(view != null && view.isEnabled(), "Missing framing control: " + label); view.performClick();
        });
    }
    private static void await(Instrumentation test, Activity booth, String prefix) {
        long end = SystemClock.uptimeMillis() + 8000;
        String[] value = {""};
        do {
            test.runOnMainSync(() -> { View view = find(booth.getWindow().getDecorView(), "Framing status"); value[0] = view == null ? "missing" : ((TextView) view).getText().toString(); });
            if (value[0].startsWith(prefix)) return;
            SystemClock.sleep(40);
        } while (SystemClock.uptimeMillis() < end);
        throw new AssertionError("Expected " + prefix + ", got " + value[0]);
    }
    private static View find(View view, String label) {
        if (label.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View found = find(((ViewGroup) view).getChildAt(i), label); if (found != null) return found;
        }
        return null;
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private FramingContract() { }
}
