package com.mprlab.portal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.format.DateFormat;
import android.view.View;
import java.util.Calendar;

final class ScreensaverClockView extends View {
    private static final int FACE = Color.rgb(242, 240, 255);
    private static final int HOUR = Color.rgb(76, 82, 99);
    private static final int MINUTE = Color.rgb(129, 138, 185);
    private static final int SECOND = Color.rgb(157, 134, 171);
    private static final int FACE_DP = 280;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path outline = new Path();
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            invalidate();
            postDelayed(this, 1000 - System.currentTimeMillis() % 1000);
        }
    };

    ScreensaverClockView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int preferred = Math.round(FACE_DP * getResources().getDisplayMetrics().density);
        int size = Math.min(resolveSize(preferred, widthSpec), resolveSize(preferred, heightSpec));
        setMeasuredDimension(size, size);
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        float radius = Math.min(width, height) / 2f;
        outline.reset();
        for (int degree = 0; degree <= 360; degree++) {
            double angle = Math.toRadians(degree - 90);
            double edge = radius * (.965 + .025 * Math.cos(12 * angle));
            float x = (float) (radius + edge * Math.cos(angle));
            float y = (float) (radius + edge * Math.sin(angle));
            if (degree == 0) outline.moveTo(x, y); else outline.lineTo(x, y);
        }
        outline.close();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Calendar now = Calendar.getInstance();
        float radius = getWidth() / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(FACE);
        canvas.drawPath(outline, paint);
        canvas.save();
        canvas.translate(radius, radius);
        canvas.save();
        canvas.rotate(-76);
        paint.setColor(HOUR);
        paint.setTextSize(radius * .165f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(DateFormat.format("EEE d", now).toString(), 0, -radius * .60f, paint);
        canvas.restore();
        float minutes = now.get(Calendar.MINUTE) + now.get(Calendar.SECOND) / 60f;
        float hours = now.get(Calendar.HOUR) + minutes / 60f;
        hand(canvas, hours * 30, radius * .44f, radius * .18f, HOUR);
        hand(canvas, minutes * 6, radius * .62f, radius * .16f, MINUTE);
        double secondsAngle = Math.toRadians(now.get(Calendar.SECOND) * 6 - 90);
        paint.setColor(SECOND);
        canvas.drawCircle((float) Math.cos(secondsAngle) * radius * .84f,
                (float) Math.sin(secondsAngle) * radius * .84f, radius * .08f, paint);
        canvas.restore();
    }

    private void hand(Canvas canvas, float degrees, float length, float width, int color) {
        canvas.save();
        canvas.rotate(degrees);
        paint.setColor(color);
        paint.setStrokeWidth(width);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(0, 0, 0, -length, paint);
        canvas.restore();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(tick);
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(tick);
        super.onDetachedFromWindow();
    }
}
