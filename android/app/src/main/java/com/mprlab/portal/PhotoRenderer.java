package com.mprlab.portal;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

import java.io.File;
import java.io.IOException;
import java.util.List;

final class PhotoRenderer {
    static final long MEMORY_LIMIT = 64L * 1024 * 1024;
    enum Kind {
        SINGLE(1, "One picture"), STRIP(4, "Four pictures");
        final int count;
        final String label;
        Kind(int count, String label) { this.count = count; this.label = label; }
    }
    enum Frame {
        NONE("None"), STARS("Stars"), CONFETTI("Confetti");
        final String label;
        Frame(String label) { this.label = label; }
    }

    static final class Capture {
        final File file;
        final PhotoFraming.Crop crop;
        Capture(File file, PhotoFraming.Crop crop) { this.file = file; this.crop = crop; }
    }

    static Bitmap render(List<Capture> files, Kind kind, Frame frame, PhotoEffects.Effect effect) throws IOException {
        if (files.size() != kind.count) throw new IllegalArgumentException("Incomplete photo sequence");
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(files.get(0).file.getPath(), bounds);
        validate(bounds.outWidth, bounds.outHeight);
        int sourceWidth = bounds.outWidth, sourceHeight = bounds.outHeight;
        int width = sourceWidth, height = sourceHeight;
        long largestCrop = 0;
        for (Capture capture : files) {
            Rect area = capture.crop.pixels(sourceWidth, sourceHeight);
            width = Math.min(width, area.width()); height = Math.min(height, area.height());
            largestCrop = Math.max(largestCrop, (long) area.width() * area.height());
        }
        int border = frame == Frame.NONE ? 0 : 28;
        float scale = kind == Kind.SINGLE
                ? Math.min(1f, 1600f / Math.max(width + border * 2, height + border * 2))
                : Math.min(1f, Math.min((1200f - border * 2) / width, (3600f - border * 5) / (height * 4)));
        int cellWidth = Math.max(1, (int) (width * scale));
        int cellHeight = Math.max(1, (int) (height * scale));
        int outputWidth = cellWidth + border * 2;
        int outputHeight = kind.count * cellHeight + border * (kind.count + 1);
        // A single framed result also observes the final 1600-pixel bound.
        if (kind == Kind.SINGLE && Math.max(outputWidth, outputHeight) > 1600) {
            float correction = (1600f - border * 2) / Math.max(cellWidth, cellHeight);
            cellWidth = Math.max(1, (int) (cellWidth * correction));
            cellHeight = Math.max(1, (int) (cellHeight * correction));
            outputWidth = cellWidth + border * 2; outputHeight = cellHeight + border * 2;
        }
        if (4L * outputWidth * outputHeight + 4L * sourceWidth * sourceHeight + 8L * largestCrop + PhotoFaces.MEMORY_ALLOWANCE > MEMORY_LIMIT)
            throw new IOException("Photo renderer memory limit exceeded");
        Bitmap result = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(result); canvas.drawColor(frame == Frame.STARS ? 0xff252449 : 0xfffff5e8);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            for (int index = 0; index < files.size(); index++) {
                BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(files.get(index).file.getPath(), options);
                validate(options.outWidth, options.outHeight);
                if (options.outWidth != sourceWidth || options.outHeight != sourceHeight)
                    throw new IOException("Photo dimensions changed during the sequence. Retake the pictures.");
                Bitmap image = BitmapFactory.decodeFile(files.get(index).file.getPath());
                if (image == null) throw new IOException("Read camera picture " + (index + 1) + " failed");
                Bitmap filtered = null, cropped = null;
                try {
                    cropped = files.get(index).crop.apply(image);
                    filtered = PhotoEffects.apply(cropped, effect).image;
                    int top = border + index * (cellHeight + border);
                    canvas.drawBitmap(filtered, null, new Rect(border, top, border + cellWidth, top + cellHeight), paint);
                } finally { if (filtered != null && filtered != cropped) filtered.recycle(); if (cropped != null && cropped != image) cropped.recycle(); image.recycle(); }
            }
            if (frame != Frame.NONE) decorate(canvas, frame, outputWidth, outputHeight, border, cellHeight, kind.count);
            return result;
        } catch (IOException | RuntimeException error) { result.recycle(); throw error; }
    }

    private static void validate(int width, int height) throws IOException {
        if (width <= 0 || height <= 0 || width > PortalCamera.MAX_IMAGE_SIDE || height > PortalCamera.MAX_IMAGE_SIDE)
            throw new IOException("Camera picture dimensions are invalid");
    }

    private static void decorate(Canvas canvas, Frame frame, int width, int height, int border, int cellHeight, int count) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int[] colors = {0xffffcc55, 0xffff7e87, 0xff67d6cc, 0xffac9afa};
        for (int band = 0; band <= count; band++) {
            float y = border / 2f + band * (cellHeight + border);
            for (int x = border / 2; x < width; x += 42) {
                paint.setColor(colors[(x / 42 + band) % colors.length]);
                mark(canvas, paint, frame, x, y);
            }
        }
        for (int y = border + 26; y < height - border; y += 42) {
            paint.setColor(colors[(y / 42) % colors.length]);
            mark(canvas, paint, frame, border / 2f, y);
            mark(canvas, paint, frame, width - border / 2f, y);
        }
    }

    private static void mark(Canvas canvas, Paint paint, Frame frame, float x, float y) {
        if (frame == Frame.CONFETTI) { canvas.drawCircle(x, y, 5, paint); return; }
        Path star = new Path();
        for (int point = 0; point < 10; point++) {
            double angle = -Math.PI / 2 + point * Math.PI / 5;
            float radius = point % 2 == 0 ? 9 : 4;
            float px = x + (float) Math.cos(angle) * radius, py = y + (float) Math.sin(angle) * radius;
            if (point == 0) star.moveTo(px, py); else star.lineTo(px, py);
        }
        star.close(); canvas.drawPath(star, paint);
    }
    private PhotoRenderer() { }
}
