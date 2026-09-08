package com.mprlab.portal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

final class CharacterView extends View {
    enum Kind { PENCIL, QUESTION, MUSIC, GAME, CAMERA, CALENDAR, BOOK, TOOTH, HOURGLASS, CLOCK,
        KART, BLOCKS, TILES, MATCH, PIANO, GUITAR }
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
        switch (kind) {
            case KART: kart(canvas); canvas.restore(); return;
            case BLOCKS: blocks(canvas); canvas.restore(); return;
            case TILES: tiles(canvas); canvas.restore(); return;
            case MATCH: matchingCards(canvas); canvas.restore(); return;
            case PIANO: piano(canvas); canvas.restore(); return;
            case GUITAR: guitar(canvas); canvas.restore(); return;
            default: limbs(canvas);
        }
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

    private void kart(Canvas canvas) {
        line(canvas, 8, 62, 25, 62, 3, Color.BLACK);
        line(canvas, 4, 73, 20, 73, 3, Color.BLACK);
        line(canvas, 12, 84, 27, 84, 3, Color.BLACK);
        shape(canvas, 77, 54, 106, 69, 5, PortalStyle.PURPLE);
        shape(canvas, 33, 63, 50, 94, 6, Color.BLACK);
        shape(canvas, 90, 61, 111, 94, 6, Color.BLACK);
        shape(canvas, 51, 48, 84, 80, 10, PortalStyle.BLUE);
        oval(canvas, 40, 10, 87, 58, PortalStyle.YELLOW);
        shape(canvas, 40, 25, 88, 49, 10, Color.WHITE);
        face(canvas, 65, 35, .62f);
        path(canvas, new float[]{42, 17, 51, 7, 74, 7, 83, 17}, PortalStyle.CORAL);
        line(canvas, 54, 60, 67, 71, 6, Color.BLACK);
        oval(canvas, 62, 63, 86, 76, Color.BLACK);
        oval(canvas, 62, 60, 73, 70, Color.WHITE);
        path(canvas, new float[]{35, 73, 74, 77, 95, 65, 110, 80, 105, 100, 37, 108, 23, 91}, PortalStyle.CORAL);
        path(canvas, new float[]{49, 78, 65, 80, 74, 100, 58, 103}, PortalStyle.YELLOW);
        shape(canvas, 21, 91, 40, 115, 6, Color.BLACK);
        shape(canvas, 96, 87, 117, 113, 6, Color.BLACK);
        line(canvas, 27, 97, 27, 108, 3, PortalStyle.SECONDARY);
        line(canvas, 110, 94, 110, 106, 3, PortalStyle.SECONDARY);
        shape(canvas, 39, 98, 95, 111, 5, Color.WHITE);
        line(canvas, 48, 104, 85, 104, 3, Color.BLACK);
    }

    private void blocks(Canvas canvas) {
        line(canvas, 23, 8, 23, 16, 3, Color.BLACK);
        line(canvas, 46, 5, 46, 13, 3, Color.BLACK);
        canvas.save(); canvas.rotate(-12, 37, 44);
        block(canvas, 15, 23, PortalStyle.YELLOW);
        block(canvas, 15, 43, PortalStyle.YELLOW);
        block(canvas, 35, 43, PortalStyle.YELLOW);
        block(canvas, 55, 43, PortalStyle.YELLOW);
        face(canvas, 45, 47, .4f);
        canvas.restore();
        line(canvas, 93, 6, 93, 14, 3, Color.BLACK);
        canvas.save(); canvas.rotate(13, 94, 40);
        block(canvas, 84, 20, PortalStyle.BLUE);
        block(canvas, 64, 40, PortalStyle.BLUE);
        block(canvas, 84, 40, PortalStyle.BLUE);
        block(canvas, 104, 40, PortalStyle.BLUE);
        canvas.restore();
        canvas.save(); canvas.rotate(-7, 58, 91);
        block(canvas, 28, 73, PortalStyle.CORAL);
        block(canvas, 48, 73, PortalStyle.CORAL);
        block(canvas, 48, 93, PortalStyle.CORAL);
        block(canvas, 68, 93, PortalStyle.CORAL);
        face(canvas, 59, 90, .52f);
        canvas.restore();
        path(canvas, new float[]{100, 75, 100, 90, 94, 90, 105, 102, 117, 90, 110, 90, 110, 75}, PortalStyle.MINT);
    }

    private void block(Canvas canvas, float x, float y, int color) {
        shape(canvas, x, y, x + 20, y + 20, 2, color);
        line(canvas, x + 5, y + 5, x + 14, y + 5, 2, Color.WHITE);
    }

    private void tiles(Canvas canvas) {
        limbs(canvas);
        canvas.save(); canvas.rotate(-10, 64, 60);
        shape(canvas, 24, 17, 104, 97, 7, Color.WHITE);
        int[] colors = {PortalStyle.CORAL, PortalStyle.BLUE, PortalStyle.YELLOW,
            PortalStyle.MINT, PortalStyle.PURPLE, PortalStyle.CORAL,
            PortalStyle.YELLOW, PortalStyle.BLUE, PortalStyle.MINT};
        for (int row = 0; row < 3; row++) for (int column = 0; column < 3; column++) {
            float x = 28 + column * 24, y = 21 + row * 24;
            shape(canvas, x, y, x + 24, y + 24, 1, colors[row * 3 + column]);
            path(canvas, new float[]{x + 2, y + 2, x + 22, y + 2, x + 2, y + 22},
                colors[(row * 3 + column + 4) % colors.length]);
        }
        face(canvas, 64, 52, .85f);
        canvas.restore();
    }

    private void matchingCards(Canvas canvas) {
        canvas.save(); canvas.rotate(-14, 38, 64);
        pictureCard(canvas, 8, 24);
        canvas.restore();
        canvas.save(); canvas.rotate(12, 88, 64);
        pictureCard(canvas, 61, 31);
        canvas.restore();
        line(canvas, 52, 12, 56, 19, 3, Color.BLACK);
        line(canvas, 70, 10, 66, 19, 3, Color.BLACK);
        line(canvas, 91, 16, 80, 22, 3, Color.BLACK);
    }

    private void pictureCard(Canvas canvas, float x, float y) {
        shape(canvas, x, y, x + 53, y + 75, 7, Color.WHITE);
        shape(canvas, x + 5, y + 6, x + 48, y + 64, 4, PortalStyle.BLUE);
        canvas.save(); canvas.translate(x + 26.5f, y + 32);
        path(canvas, new float[]{0, -20, 7, -7, 21, -5, 11, 6, 14, 21, 0, 14,
            -14, 21, -11, 6, -21, -5, -7, -7}, PortalStyle.YELLOW);
        face(canvas, 0, 0, .43f);
        canvas.restore();
        line(canvas, x + 19, y + 69, x + 34, y + 69, 2, PortalStyle.PURPLE);
    }

    private void piano(Canvas canvas) {
        limbs(canvas);
        shape(canvas, 23, 23, 103, 92, 7, PortalStyle.PURPLE);
        shape(canvas, 19, 15, 107, 27, 4, PortalStyle.CORAL);
        line(canvas, 31, 33, 94, 33, 2, Color.WHITE);
        face(canvas, 64, 48, .9f);
        shape(canvas, 19, 76, 107, 99, 3, PortalStyle.CORAL);
        shape(canvas, 25, 77, 101, 94, 1, Color.WHITE);
        for (int key = 1; key < 10; key++) {
            float x = 25 + key * 7.6f;
            line(canvas, x, 78, x, 93, 1, Color.BLACK);
        }
        for (int key : new int[]{1, 2, 4, 5, 6, 8, 9}) {
            float x = 25 + key * 7.6f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
            canvas.drawRoundRect(x - 2, 77, x + 2, 87, .5f, .5f, paint);
        }
    }

    private void guitar(Canvas canvas) {
        limbs(canvas);
        Path body = new Path();
        body.moveTo(63, 46);
        body.cubicTo(37, 33, 27, 51, 39, 67);
        body.cubicTo(45, 77, 22, 76, 29, 94);
        body.cubicTo(38, 114, 91, 111, 99, 92);
        body.cubicTo(106, 75, 83, 76, 89, 66);
        body.cubicTo(102, 47, 86, 35, 63, 46);
        body.close();
        drawPath(canvas, body, PortalStyle.CORAL);
        shape(canvas, 57, 17, 71, 60, 2, PortalStyle.PURPLE);
        for (int fret = 0; fret < 5; fret++) line(canvas, 58, 28 + fret * 6, 70, 28 + fret * 6, 1, Color.BLACK);
        for (int peg = 0; peg < 3; peg++) {
            line(canvas, 50, 8 + peg * 5, 78, 8 + peg * 5, 3, Color.BLACK);
        }
        shape(canvas, 55, 3, 73, 23, 3, PortalStyle.MINT);
        for (int string = 0; string < 6; string++)
            line(canvas, 59 + string * 2, 24, 59 + string * 2, 60, .6f, Color.WHITE);
        oval(canvas, 50, 65, 79, 93, PortalStyle.YELLOW);
        oval(canvas, 54, 69, 75, 89, Color.BLACK);
        face(canvas, 64, 64, .72f);
        shape(canvas, 48, 97, 82, 102, 2, PortalStyle.PURPLE);
        for (int string = 0; string < 6; string++)
            line(canvas, 57 + string * 3, 92, 57 + string * 3, 99, .8f, Color.WHITE);
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
