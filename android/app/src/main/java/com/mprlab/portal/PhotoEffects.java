package com.mprlab.portal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

/** Local image effects. The caller owns the input and the returned image. */
final class PhotoEffects {
    enum Effect {
        NORMAL("Original", false), BIG_NOSE("Big nose", false), STRETCH("Stretch", false),
        MIRROR("Mirror twins", false), BUNNY("Bunny ears", true), GOOGLY("Googly eyes", true), PARTY("Party hat", true);
        final String label;
        final boolean needsFace;
        Effect(String label, boolean needsFace) { this.label = label; this.needsFace = needsFace; }
    }
    static final class Result {
        final Bitmap image;
        final int faces;
        Result(Bitmap image, int faces) { this.image = image; this.faces = faces; }
    }
    static Result apply(Bitmap source, Effect effect) {
        if (effect == Effect.NORMAL) return new Result(source, 0);
        int width = source.getWidth(), height = source.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        try {
            if (effect == Effect.MIRROR) {
                Rect half = new Rect(0, 0, width / 2, height);
                canvas.drawBitmap(source, half, half, paint);
                canvas.save(); canvas.translate(width, 0); canvas.scale(-1, 1);
                canvas.drawBitmap(source, half, new Rect(0, 0, width - width / 2, height), paint); canvas.restore();
            } else if (!effect.needsFace) {
                int grid = 40;
                float[] vertices = new float[(grid + 1) * (grid + 1) * 2];
                int offset = 0;
                for (int row = 0; row <= grid; row++) for (int col = 0; col <= grid; col++) {
                    float u = (float) col / grid, v = (float) row / grid;
                    float x = u * width, y = v * height;
                    if (effect == Effect.STRETCH) x = width * (.5f + .5f * (float) Math.sin(Math.PI * (u - .5f)));
                    else {
                        float dx = x - width / 2f, dy = y - height / 2f;
                        float radius = Math.min(width, height) * .48f;
                        float distance = (float) Math.hypot(dx, dy) / radius;
                        if (distance < 1) {
                            float zoom = 1 + .85f * (1 - distance) * (1 - distance);
                            x = width / 2f + dx * zoom; y = height / 2f + dy * zoom;
                        }
                    }
                    vertices[offset++] = x; vertices[offset++] = y;
                }
                canvas.drawBitmapMesh(source, grid, grid, vertices, 0, null, 0, paint);
            } else {
                canvas.drawBitmap(source, 0, 0, paint);
                return new Result(result, accessories(source, canvas, paint, effect));
            }
            return new Result(result, 0);
        } catch (RuntimeException failure) { result.recycle(); throw failure; }
    }
    private static int accessories(Bitmap source, Canvas canvas, Paint paint, Effect effect) {
        int visible = 0;
        for (PhotoFaces.Face face : PhotoFaces.find(source)) {
            float distance = face.eyes * source.getWidth();
            canvas.save(); canvas.translate(face.eyeX * source.getWidth(), face.eyeY * source.getHeight());
            canvas.rotate(face.roll);
            if (effect == Effect.BUNNY) {
                for (int side : new int[]{-1, 1}) {
                    canvas.save(); canvas.translate(side * distance * .65f, -distance * .9f); canvas.rotate(side * 14);
                    paint.setColor(0xfffff6ee); canvas.drawOval(-distance * .26f, -distance * 1.6f, distance * .26f, distance * .2f, paint);
                    paint.setColor(0xffff8bb6); canvas.drawOval(-distance * .13f, -distance * 1.4f, distance * .13f, 0, paint);
                    canvas.restore();
                }
                paint.setColor(0xffff8bb6); canvas.drawCircle(0, distance * .52f, distance * .15f, paint);
            } else if (effect == Effect.GOOGLY) {
                for (int side : new int[]{-1, 1}) {
                    float x = side * distance / 2;
                    paint.setColor(0xff483552); canvas.drawCircle(x, 0, distance * .48f, paint);
                    paint.setColor(Color.WHITE); canvas.drawCircle(x, 0, distance * .42f, paint);
                    paint.setColor(0xff222438); canvas.drawCircle(x + side * distance * .12f, distance * .08f, distance * .2f, paint);
                    paint.setColor(Color.WHITE); canvas.drawCircle(x + side * distance * .12f - distance * .05f, 0, distance * .06f, paint);
                }
            } else {
                Path hat = new Path(); hat.moveTo(-distance, -distance * .7f);
                hat.lineTo(distance * .12f, -distance * 2.9f); hat.lineTo(distance, -distance * .7f); hat.close();
                paint.setColor(0xff9b65e8); canvas.drawPath(hat, paint);
                paint.setColor(0xffffcc55); canvas.drawCircle(distance * .12f, -distance * 2.9f, distance * .23f, paint);
                canvas.drawCircle(-distance * .25f, -distance * 1.2f, distance * .12f, paint);
                canvas.drawCircle(distance * .25f, -distance * 1.7f, distance * .12f, paint);
                paint.setColor(0xff67d6cc); canvas.drawRoundRect(-distance, -distance * .85f, distance, -distance * .55f, distance * .15f, distance * .15f, paint);
            }
            canvas.restore(); visible++;
        }
        return visible;
    }
    private PhotoEffects() { }
}
