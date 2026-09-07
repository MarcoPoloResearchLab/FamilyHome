package com.mprlab.portal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

final class CharacterView extends View {
    enum Kind { PENCIL, QUESTION, MUSIC, GAME, CAMERA, CALENDAR, BOOK, TOOTH, HOURGLASS, CLOCK }
    private final Kind kind;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    CharacterView(Context context, Kind kind) {
        super(context);
        this.kind = kind;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float scale = Math.min(getWidth(), getHeight()) / 128f;
        canvas.save();
        canvas.translate((getWidth() - scale * 128) / 2, (getHeight() - scale * 128) / 2);
        canvas.scale(scale, scale);
        limbs(canvas);
        switch (kind) {
            case PENCIL:
                canvas.save(); canvas.rotate(16, 64, 60);
                shape(canvas, 48, 12, 80, 35, 12, PortalStyle.CORAL);
                shape(canvas, 48, 30, 80, 90, 3, PortalStyle.YELLOW);
                shape(canvas, 48, 30, 80, 39, 2, Color.WHITE);
                path(canvas, new float[]{48, 90, 80, 90, 64, 115}, 0xffffd9a8);
                path(canvas, new float[]{58, 103, 70, 103, 64, 115}, Color.BLACK);
                line(canvas, 54, 43, 54, 78, 2, 0xffe6a929);
                face(canvas, 64, 57, .75f);
                canvas.restore(); break;
            case QUESTION:
                path(canvas, new float[]{44, 82, 33, 103, 65, 88}, PortalStyle.PURPLE);
                oval(canvas, 23, 14, 105, 93, PortalStyle.PURPLE);
                paint.setStyle(Paint.Style.FILL); paint.setColor(Color.BLACK);
                paint.setTypeface(PortalStyle.heading(getContext())); paint.setTextSize(52);
                paint.setTextAlign(Paint.Align.CENTER); canvas.drawText("?", 66, 62, paint);
                face(canvas, 64, 75, .55f); break;
            case MUSIC:
                path(canvas, new float[]{67, 75, 67, 18, 102, 10, 102, 41, 79, 45, 79, 86}, PortalStyle.BLUE);
                oval(canvas, 24, 53, 85, 101, PortalStyle.BLUE);
                face(canvas, 52, 73, .9f);
                line(canvas, 72, 25, 92, 20, 3, Color.WHITE); break;
            case GAME:
                Path controller = new Path();
                controller.moveTo(36, 26); controller.cubicTo(18, 23, 17, 44, 13, 70);
                controller.cubicTo(8, 100, 30, 92, 43, 79); controller.lineTo(85, 79);
                controller.cubicTo(102, 96, 119, 98, 114, 69); controller.lineTo(109, 42);
                controller.quadTo(104, 24, 91, 26); controller.close();
                drawPath(canvas, controller, PortalStyle.MINT);
                shape(canvas, 28, 42, 38, 66, 2, 0xff249b73);
                shape(canvas, 21, 49, 45, 59, 2, 0xff249b73);
                oval(canvas, 90, 40, 101, 51, PortalStyle.BLUE);
                oval(canvas, 98, 54, 109, 65, PortalStyle.CORAL);
                face(canvas, 65, 53, .75f); break;
            case CAMERA:
                shape(canvas, 44, 15, 76, 32, 5, PortalStyle.PURPLE);
                shape(canvas, 18, 29, 108, 88, 13, PortalStyle.CORAL);
                oval(canvas, 42, 36, 91, 84, Color.WHITE);
                oval(canvas, 49, 43, 84, 78, 0xff544386);
                dot(canvas, 65, 61, 11, Color.BLACK); dot(canvas, 60, 55, 5, Color.WHITE);
                shape(canvas, 26, 36, 40, 44, 3, PortalStyle.YELLOW);
                line(canvas, 50, 4, 46, 9, 3, PortalStyle.YELLOW);
                line(canvas, 69, 3, 72, 9, 3, PortalStyle.YELLOW); break;
            case CALENDAR:
                shape(canvas, 25, 21, 104, 97, 9, Color.WHITE);
                shape(canvas, 25, 21, 104, 43, 7, PortalStyle.CORAL);
                for (int x = 41; x < 95; x += 24) shape(canvas, x, 13, x + 6, 33, 3, Color.WHITE);
                face(canvas, 65, 62, .9f);
                for (int x = 39; x < 95; x += 19) shape(canvas, x, 82, x + 11, 88, 1, PortalStyle.BLUE);
                break;
            case BOOK:
                shape(canvas, 28, 20, 100, 99, 7, Color.WHITE);
                shape(canvas, 23, 13, 95, 89, 7, PortalStyle.BLUE);
                line(canvas, 34, 19, 34, 82, 3, 0xff438dbe);
                line(canvas, 42, 24, 80, 24, 3, Color.WHITE);
                face(canvas, 65, 54, 1f);
                line(canvas, 42, 95, 91, 95, 2, PortalStyle.SECONDARY); break;
            case TOOTH:
                Path tooth = new Path();
                tooth.moveTo(61, 25); tooth.cubicTo(22, 5, 18, 40, 28, 65);
                tooth.cubicTo(37, 100, 44, 112, 51, 80); tooth.quadTo(61, 56, 69, 80);
                tooth.cubicTo(76, 112, 85, 99, 94, 63); tooth.cubicTo(108, 16, 87, 14, 61, 25);
                drawPath(canvas, tooth, Color.WHITE); face(canvas, 61, 47, 1f);
                shape(canvas, 104, 45, 112, 91, 4, PortalStyle.BLUE);
                shape(canvas, 102, 27, 116, 50, 3, Color.WHITE);
                for (int y = 32; y < 48; y += 5) line(canvas, 106, y, 115, y, 2, PortalStyle.BLUE);
                break;
            case HOURGLASS:
                Path glass = new Path(); glass.moveTo(36, 23); glass.lineTo(93, 23);
                glass.cubicTo(94, 46, 77, 47, 73, 59); glass.cubicTo(75, 72, 94, 77, 92, 96);
                glass.lineTo(35, 96); glass.cubicTo(33, 76, 55, 68, 55, 59);
                glass.cubicTo(50, 47, 33, 44, 36, 23); drawPath(canvas, glass, 0xfffff2bb);
                path(canvas, new float[]{41, 84, 64, 65, 88, 84, 88, 92, 40, 92}, PortalStyle.YELLOW);
                shape(canvas, 29, 15, 99, 27, 5, PortalStyle.YELLOW);
                shape(canvas, 29, 94, 99, 107, 5, PortalStyle.YELLOW);
                face(canvas, 64, 41, .65f); break;
            case CLOCK:
                shape(canvas, 55, 9, 75, 23, 5, PortalStyle.CORAL);
                oval(canvas, 22, 25, 106, 107, PortalStyle.CORAL);
                oval(canvas, 29, 32, 99, 100, Color.WHITE);
                line(canvas, 64, 40, 64, 49, 3, Color.BLACK);
                line(canvas, 64, 49, 78, 42, 3, Color.BLACK);
                face(canvas, 64, 65, 1f); break;
        }
        canvas.restore();
    }

    private void limbs(Canvas canvas) {
        line(canvas, 43, 85, 39, 106, 5, Color.BLACK);
        line(canvas, 84, 85, 91, 106, 5, Color.BLACK);
        oval(canvas, 26, 104, 48, 115, Color.WHITE);
        oval(canvas, 84, 104, 105, 115, Color.WHITE);
        line(canvas, 27, 54, 13, 66, 5, Color.BLACK);
        line(canvas, 100, 57, 114, 40, 5, Color.BLACK);
        oval(canvas, 5, 60, 23, 77, Color.WHITE);
        oval(canvas, 106, 26, 122, 46, Color.WHITE);
        line(canvas, 111, 29, 109, 23, 3, Color.BLACK);
        line(canvas, 117, 29, 118, 22, 3, Color.BLACK);
    }

    private void face(Canvas canvas, float x, float y, float scale) {
        canvas.save(); canvas.translate(x, y); canvas.scale(scale, scale);
        oval(canvas, -20, -13, -3, 10, Color.WHITE);
        oval(canvas, 3, -16, 20, 9, Color.WHITE);
        dot(canvas, -9, 0, 5, Color.BLACK); dot(canvas, 11, -2, 5, Color.BLACK);
        dot(canvas, -7, -2, 1.6f, Color.WHITE); dot(canvas, 13, -4, 1.6f, Color.WHITE);
        Path smile = new Path(); smile.moveTo(-13, 15); smile.quadTo(1, 24, 16, 13);
        smile.cubicTo(10, 39, -6, 34, -13, 15); drawPath(canvas, smile, Color.BLACK);
        paint.setStyle(Paint.Style.FILL); paint.setColor(PortalStyle.CORAL);
        canvas.drawOval(-4, 24, 10, 30, paint);
        canvas.restore();
    }

    private void shape(Canvas canvas, float left, float top, float right, float bottom, float radius, int color) {
        Path path = new Path(); path.addRoundRect(left, top, right, bottom, radius, radius, Path.Direction.CW);
        drawPath(canvas, path, color);
    }
    private void oval(Canvas canvas, float left, float top, float right, float bottom, int color) {
        Path path = new Path(); path.addOval(left, top, right, bottom, Path.Direction.CW); drawPath(canvas, path, color);
    }
    private void path(Canvas canvas, float[] points, int color) {
        Path path = new Path(); path.moveTo(points[0], points[1]);
        for (int index = 2; index < points.length; index += 2) path.lineTo(points[index], points[index + 1]);
        path.close(); drawPath(canvas, path, color);
    }
    private void drawPath(Canvas canvas, Path path, int color) {
        paint.setStyle(Paint.Style.STROKE); paint.setColor(Color.WHITE); paint.setStrokeWidth(8);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL); paint.setColor(color); canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setColor(Color.BLACK); paint.setStrokeWidth(2.6f); canvas.drawPath(path, paint);
    }
    private void line(Canvas canvas, float x1, float y1, float x2, float y2, float width, int color) {
        paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(width); canvas.drawLine(x1, y1, x2, y2, paint);
    }
    private void dot(Canvas canvas, float x, float y, float radius, int color) {
        paint.setColor(color); paint.setStyle(Paint.Style.FILL); canvas.drawCircle(x, y, radius, paint);
    }
}
