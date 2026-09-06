package com.mprlab.portal;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/** Small illustrated previews for the photo format and decorative frame controls. */
final class PhotoOptionIcon extends Drawable {
    enum Art { SINGLE, STRIP, NONE, STARS, CONFETTI }
    private final Art art;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    PhotoOptionIcon(Art art) { this.art = art; }
    @Override public void draw(Canvas canvas) {
        int save = canvas.save();
        canvas.translate(getBounds().left, getBounds().top);
        canvas.scale(getBounds().width() / 80f, getBounds().height() / 64f);
        if (art == Art.STRIP) {
            box(canvas, 24, 1, 56, 63, PortalStyle.WHITE);
            for (int i = 0; i < 4; i++) {
                canvas.save(); canvas.translate(29, 5 + i * 14); canvas.scale(.36f, .23f);
                portrait(canvas); canvas.restore();
            }
        } else {
            int color = art == Art.STARS ? 0xff252449 : art == Art.CONFETTI ? 0xfffff5e8 : PortalStyle.WHITE;
            box(canvas, 2, 2, 78, 62, color);
            canvas.save(); canvas.translate(11, 10); portrait(canvas); canvas.restore();
            if (art == Art.STARS) for (float[] p : new float[][]{{8,8},{71,8},{8,55},{71,55}}) star(canvas, p[0], p[1]);
            if (art == Art.CONFETTI) {
                int[] colors = {PortalStyle.CORAL, PortalStyle.BLUE, PortalStyle.PURPLE, PortalStyle.MINT};
                for (int i = 0; i < 12; i++) {
                    paint.setColor(colors[i % colors.length]); paint.setStyle(Paint.Style.FILL);
                    float x = 7 + (i % 6) * 13, y = i < 6 ? 6 : 57;
                    canvas.drawCircle(x, y, 2.5f, paint);
                }
            }
        }
        canvas.restoreToCount(save);
    }
    private void portrait(Canvas canvas) {
        int save = canvas.save(); canvas.clipRect(0, 0, 58, 44);
        paint.setStyle(Paint.Style.FILL); paint.setColor(PortalStyle.BLUE); canvas.drawRect(0, 0, 58, 44, paint);
        paint.setColor(PortalStyle.PURPLE); canvas.drawOval(9, 29, 49, 60, paint);
        paint.setColor(PortalStyle.YELLOW); canvas.drawCircle(29, 20, 14, paint);
        paint.setColor(PortalStyle.INK); canvas.drawCircle(24, 18, 1.6f, paint); canvas.drawCircle(34, 18, 1.6f, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.8f); canvas.drawArc(23, 18, 35, 29, 15, 150, false, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.restoreToCount(save);
    }
    private void box(Canvas canvas, float left, float top, float right, float bottom, int color) {
        paint.setColor(color); paint.setStyle(Paint.Style.FILL); canvas.drawRoundRect(left, top, right, bottom, 3, 3, paint);
        paint.setColor(PortalStyle.INK); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2);
        canvas.drawRoundRect(left, top, right, bottom, 3, 3, paint); paint.setStyle(Paint.Style.FILL);
    }
    private void star(Canvas canvas, float x, float y) {
        Path path = new Path();
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5; float radius = i % 2 == 0 ? 5 : 2.3f;
            float px = x + (float) Math.cos(angle) * radius, py = y + (float) Math.sin(angle) * radius;
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        path.close(); paint.setColor(PortalStyle.YELLOW); paint.setStyle(Paint.Style.FILL); canvas.drawPath(path, paint);
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
