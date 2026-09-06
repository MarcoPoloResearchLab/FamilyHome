package com.mprlab.portal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import java.util.Collections;
import java.util.List;

/** Main-thread framing state. Face locations are spatial measurements, not identities. */
final class PhotoFraming {
    enum Zoom {
        WIDE(1, "1×"), NEAR(1.5f, "1.5×"), PORTRAIT(2, "2×"), CLOSE(3, "3×");
        final float factor;
        final String label;
        Zoom(float factor, String label) { this.factor = factor; this.label = label; }
    }
    enum Mode { SEARCHING, SELECTING, FOLLOWING, HOLDING, LOST }
    private static final long DETECTION_GAP_MS = 800;
    static final long MAX_FRAME_AGE_MS = 1000;
    static final class Crop {
        static final Crop FULL = new Crop(.5f, .5f, Zoom.WIDE);
        final float x, y, fraction;
        Crop(float x, float y, Zoom zoom) {
            this(x, y, 1 / zoom.factor);
        }
        private Crop(float x, float y, float fraction) {
            this.fraction = fraction;
            this.x = Math.max(fraction / 2, Math.min(1 - fraction / 2, x));
            this.y = Math.max(fraction / 2, Math.min(1 - fraction / 2, y));
        }
        Rect pixels(int width, int height) {
            int w = Math.max(1, Math.round(width * fraction)), h = Math.max(1, Math.round(height * fraction));
            int left = Math.max(0, Math.min(width - w, Math.round(x * width - w / 2f)));
            int top = Math.max(0, Math.min(height - h, Math.round(y * height - h / 2f)));
            return new Rect(left, top, left + w, top + h);
        }
        Bitmap apply(Bitmap source) {
            Rect bounds = pixels(source.getWidth(), source.getHeight());
            return Bitmap.createBitmap(source, bounds.left, bounds.top, bounds.width(), bounds.height());
        }
    }
    static final class Frame {
        final Crop crop;
        final List<PhotoFaces.Face> faces;
        final PhotoFaces.Face target;
        final Mode mode;
        final String label;
        final long time;
        Frame(Crop crop, List<PhotoFaces.Face> faces, PhotoFaces.Face target, Mode mode, String label, long time) {
            this.crop = crop; this.faces = faces; this.target = target; this.mode = mode; this.label = label; this.time = time;
        }
        boolean ready(long now) { return (mode == Mode.SEARCHING || mode == Mode.FOLLOWING) && now - time <= MAX_FRAME_AGE_MS; }
    }
    private Zoom zoom = Zoom.WIDE;
    private Mode mode = Mode.SEARCHING;
    private PhotoFaces.Face target;
    private Crop crop = Crop.FULL;
    private long lastSeen, selection;
    long selection() { return selection; }
    PhotoFaces.Face target() { return target; }
    Zoom zoom() { return zoom; }
    Mode mode() { return mode; }
    private boolean chosenByTap, zoomChosen;
    void zoom(Zoom value) {
        zoom = value; zoomChosen = true;
        crop = target == null ? new Crop(crop.x, crop.y, zoom) : fitHead(target, crop);
    }
    void suspend() { mode = Mode.SEARCHING; target = null; chosenByTap = false; crop = Crop.FULL; }
    private void follow(PhotoFaces.Face face, long now) {
        target = face; selection++; mode = Mode.FOLLOWING; lastSeen = now;
        if (!zoomChosen) zoom = Zoom.PORTRAIT;
        crop = fitHead(target, null);
    }
    private Crop fitHead(PhotoFaces.Face face, Crop previous) {
        // A face box starts near the eyebrows. Reserve space for the crown, hair, and shoulders.
        float left = Math.max(0, face.x - face.span * .9f), right = Math.min(1, face.x + face.span * .9f);
        float top = Math.max(0, face.y - face.height * 1.05f), bottom = Math.min(1, face.y + face.height * 1.25f);
        float fraction = Math.min(1, Math.max(1 / zoom.factor, Math.max(right - left, bottom - top)));
        float x = (left + right) / 2, y = (top + bottom) / 2;
        if (previous != null) {
            // Widen immediately for a closer head; move inward gradually when there is room.
            fraction = Math.max(fraction, previous.fraction * .85f + fraction * .15f);
            x = previous.x * .55f + x * .45f; y = previous.y * .55f + y * .45f;
        }
        x = Math.max(right - fraction / 2, Math.min(left + fraction / 2, x));
        y = Math.max(bottom - fraction / 2, Math.min(top + fraction / 2, y));
        return new Crop(x, y, fraction);
    }
    void tap(Frame displayed, float x, float y, float aspect, long now) {
        if (mode != Mode.SELECTING || displayed.mode != Mode.SELECTING || now - displayed.time > MAX_FRAME_AGE_MS) return;
        PhotoFaces.Face selected = null;
        for (PhotoFaces.Face face : displayed.faces) {
            if (Math.abs(x - face.x) <= face.span / 2 && Math.abs(y - face.y) <= face.height / 2) {
                if (selected != null) return;
                selected = face;
            }
        }
        if (selected == null) return;
        chosenByTap = true;
        follow(selected, now);
    }
    Frame update(List<PhotoFaces.Face> faces, List<PhotoFaces.Face> measurements, float aspect, long now) {
        // A lone person needs no setup. A group gets a full-frame, tappable overview.
        if (mode == Mode.SEARCHING || mode == Mode.SELECTING || mode == Mode.LOST) {
            if (faces.size() == 1) { chosenByTap = false; follow(faces.get(0), now); }
            else {
                mode = faces.isEmpty() ? Mode.SEARCHING : Mode.SELECTING;
                crop = faces.isEmpty() && zoomChosen ? new Crop(.5f, .5f, zoom) : Crop.FULL;
            }
            return frame(faces, now);
        }
        if (!chosenByTap && faces.size() > 1) {
            mode = Mode.SELECTING; target = null; crop = Crop.FULL;
            return frame(faces, now);
        }
        if (mode == Mode.FOLLOWING || mode == Mode.HOLDING) {
            if (now - lastSeen > MAX_FRAME_AGE_MS || measurements.size() > 1
                    || (measurements.isEmpty() && now - lastSeen > DETECTION_GAP_MS)) {
                String reason = now - lastSeen > MAX_FRAME_AGE_MS ? "stale measurement"
                        : measurements.size() > 1 ? "ambiguous faces"
                        : measurements.isEmpty() ? "no subject measurement" : "face geometry changed";
                android.util.Log.d("PhotoBoothTracking", "Selection lost: " + reason);
                mode = Mode.LOST; target = null; chosenByTap = false; crop = Crop.FULL;
            } else if (measurements.isEmpty()) mode = Mode.HOLDING;
            else {
                mode = Mode.FOLLOWING; target = measurements.get(0); lastSeen = now;
                crop = fitHead(target, crop);
            }
        }
        return frame(faces, now);
    }
    private Frame frame(List<PhotoFaces.Face> faces, long now) {
        String label = mode == Mode.SELECTING ? "Tap your face"
                : mode == Mode.HOLDING ? "Hold still…"
                : mode == Mode.FOLLOWING ? "Smile!" : "Step into the picture";
        return new Frame(crop, Collections.unmodifiableList(faces), target, mode, label, now);
    }
    static void drawFaces(Bitmap bitmap, List<PhotoFaces.Face> faces) {
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); paint.setColor(0xffffcc55); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3);
        for (PhotoFaces.Face face : faces) {
            float x = face.x * bitmap.getWidth(), y = face.y * bitmap.getHeight();
            float halfWidth = face.span * bitmap.getWidth() / 2, halfHeight = face.height * bitmap.getHeight() / 2;
            canvas.drawRoundRect(x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight, 8, 8, paint);
        }
    }
}
