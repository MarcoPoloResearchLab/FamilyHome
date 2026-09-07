package com.mprlab.portal;

import android.content.Context;
import android.graphics.Bitmap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceDetectorYN;

/** Local YuNet inference, with normalized face boxes and eye landmarks. */
final class PhotoFaces {
    static final long MEMORY_ALLOWANCE = 16L * 1024 * 1024;
    private static byte[] model;
    static synchronized void initialize(Context context) throws IOException {
        if (model != null) return;
        try { System.loadLibrary("opencv_java4"); }
        catch (LinkageError failure) { throw new IOException("Load local face detector failed", failure); }
        Core.setNumThreads(2);
        try (InputStream input = context.getAssets().open("face.onnx"); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int length;
            while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
            model = output.toByteArray();
        }
    }
    static final class Face {
        final float x, y, span, height, eyeX, eyeY, eyes, roll;
        Face(float x, float y, float span, float height, float eyeX, float eyeY, float eyes, float roll) {
            for (float value : new float[]{x, y, span, height, eyeX, eyeY, eyes, roll})
                if (!Float.isFinite(value)) throw new IllegalArgumentException("Invalid camera face geometry");
            if (x < 0 || x > 1 || y < 0 || y > 1 || span <= 0 || span > 1 || height <= 0 || height > 1 || eyes <= 0 || eyes > 1)
                throw new IllegalArgumentException("Invalid camera face bounds");
            this.x = x; this.y = y; this.span = span; this.height = height;
            this.eyeX = eyeX; this.eyeY = eyeY; this.eyes = eyes; this.roll = roll;
        }
        Face translated(float dx, float dy) {
            return new Face(x + dx, y + dy, span, height, eyeX + dx, eyeY + dy, eyes, roll);
        }
    }
    private static final ThreadLocal<Detection> detections = new ThreadLocal<>();
    private static final class Detection {
        final Mat rgba = new Mat(), bgr = new Mat(), output = new Mat();
        final FaceDetectorYN detector;
        Detection() {
            if (model == null) throw new IllegalStateException("Photo face model is not initialized");
            MatOfByte weights = new MatOfByte(model), config = new MatOfByte();
            try { detector = FaceDetectorYN.create("onnx", weights, config, new Size(320, 320), .75f, .3f, 100); }
            finally { weights.release(); config.release(); }
        }
    }
    static void releaseWorker() {
        Detection analysis = detections.get();
        if (analysis != null) {
            analysis.rgba.release(); analysis.bgr.release(); analysis.output.release();
            detections.remove();
        }
    }
    static List<Face> find(Bitmap source) {
        Detection analysis = detections.get();
        if (analysis == null) { analysis = new Detection(); detections.set(analysis); }
        float scale = 320f / Math.max(source.getWidth(), source.getHeight());
        int width = Math.max(32, Math.round(source.getWidth() * scale / 32) * 32);
        int height = Math.max(32, Math.round(source.getHeight() * scale / 32) * 32);
        Size size = new Size(width, height);
        analysis.detector.setInputSize(size);
        Bitmap scaled = Bitmap.createScaledBitmap(source, width, height, true);
        try { Utils.bitmapToMat(scaled, analysis.rgba); }
        finally { if (scaled != source) scaled.recycle(); }
        Imgproc.cvtColor(analysis.rgba, analysis.bgr, Imgproc.COLOR_RGBA2BGR);
        analysis.detector.detect(analysis.bgr, analysis.output);
        List<Face> result = new ArrayList<>();
        for (int row = 0; row < analysis.output.rows(); row++) {
            float[] values = new float[15]; analysis.output.get(row, 0, values);
            float x = (values[4] + values[6]) / (2 * width), y = (values[5] + values[7]) / (2 * height);
            float dx = (values[6] - values[4]) / width * source.getWidth();
            float dy = (values[7] - values[5]) / height * source.getHeight();
            float eyes = (float) Math.hypot(dx, dy) / source.getWidth();
            float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
            if (angle > 90) angle -= 180; else if (angle < -90) angle += 180;
            // A box intersecting the image edge can extend outside it; only visible landmark centers are selectable.
            if (x < 0 || x > 1 || y < 0 || y > 1) continue;
            float left = Math.max(0, values[0] / width), top = Math.max(0, values[1] / height);
            float right = Math.min(1, (values[0] + values[2]) / width), bottom = Math.min(1, (values[1] + values[3]) / height);
            result.add(new Face((left + right) / 2, (top + bottom) / 2, right - left, bottom - top, x, y, eyes, angle));
        }
        return result;
    }
    private PhotoFaces() { }
}
