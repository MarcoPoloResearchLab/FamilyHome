package com.mprlab.portal;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MusicActivity extends PortalActivity {
    private static final int INK = Color.rgb(36, 49, 71);
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.addView(label("Music", 27, INK), new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(32), dp(26), dp(32), dp(32));
        content.addView(label("What will you play today?", 25, INK));
        LinearLayout cards = new LinearLayout(this);
        cards.setPadding(0, dp(24), 0, 0);
        addInstrument(cards, "Piano", "Tap the keys. Find your melody.", Color.rgb(67, 114, 235), PianoActivity.class);
        addInstrument(cards, "Guitar", "Pick a note. Strum a chord.", Color.rgb(195, 112, 43), GuitarActivity.class);
        content.addView(cards, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(PortalToolbar.screen(this, toolbar, content, Color.rgb(255, 248, 234)));
    }
    private void addInstrument(LinearLayout cards, String title, String subtitle, int color, Class<?> activity) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(24), dp(20), dp(24), dp(24));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE); background.setCornerRadius(dp(26));
        card.setBackground(background); card.setElevation(dp(3));
        card.addView(new InstrumentArt(title), new LinearLayout.LayoutParams(-1, 0, 1));
        card.addView(label(title, 32, color));
        TextView description = label(subtitle, 18, INK); description.setPadding(0, dp(10), 0, dp(10));
        card.addView(description);
        card.setContentDescription(title); card.setFocusable(true);
        card.setOnClickListener(view -> {
            Intent intent = new Intent(this, activity);
            if (getIntent().getExtras() != null) intent.putExtras(getIntent().getExtras());
            startActivity(intent);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(8), 0, dp(8), dp(4)); cards.addView(card, params);
    }
    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return view;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private final class InstrumentArt extends View {
        private final String instrument;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        InstrumentArt(String instrument) { super(MusicActivity.this); this.instrument = instrument; }
        private void drawPiano(Canvas canvas, float width, float height) {
            float scale = Math.min(width / 390f, height / 390f) * .94f;
            canvas.save();
            canvas.translate(width / 2, height / 2);
            canvas.scale(scale, scale);
            canvas.translate(-178, -192);
            paint.setStyle(Paint.Style.FILL);

            // A cabinet and raised lid make this an upright piano, with a shallow side face.
            Path side = new Path();
            side.moveTo(305, 64); side.lineTo(321, 49); side.lineTo(321, 293);
            side.lineTo(305, 309); side.close();
            paint.setColor(0xff1e365e); canvas.drawPath(side, paint);
            paint.setColor(0xff233f6b);
            canvas.drawRoundRect(49, 268, 68, 338, 4, 4, paint);
            canvas.drawRoundRect(287, 268, 306, 338, 4, 4, paint);
            paint.setColor(0xff142a49);
            canvas.drawRoundRect(44, 331, 73, 340, 3, 3, paint);
            canvas.drawRoundRect(282, 331, 311, 340, 3, 3, paint);

            paint.setShader(new LinearGradient(45, 62, 308, 297,
                    new int[]{0xff457dc3, 0xff2d5795, 0xff203d6c}, null, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(45, 62, 306, 309, 5, 5, paint);
            paint.setShader(null);
            Path lid = new Path();
            lid.moveTo(36, 62); lid.lineTo(51, 47); lid.lineTo(321, 47); lid.lineTo(309, 62); lid.close();
            paint.setColor(0xff6a9bd8); canvas.drawPath(lid, paint);
            paint.setColor(0xff203d68); canvas.drawRoundRect(36, 60, 311, 69, 2, 2, paint);
            paint.setColor(0xffa8c9ec); paint.setStrokeWidth(1.5f); canvas.drawLine(40, 61, 308, 61, paint);

            // Inset panels, warm metal accents, and edge highlights balance the guitar detail.
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.6f); paint.setColor(0xff83aade);
            canvas.drawRoundRect(61, 80, 289, 165, 3, 3, paint);
            paint.setColor(0xff5275a8); canvas.drawRoundRect(65, 251, 286, 294, 3, 3, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xffe7c280); canvas.drawRoundRect(158, 134, 191, 138, 2, 2, paint);
            paint.setColor(0xff8db4e4); paint.setStrokeWidth(1.5f);
            canvas.drawLine(70, 87, 280, 87, paint);
            canvas.drawLine(62, 92, 62, 156, paint);
            paint.setColor(0xff152a48); canvas.drawRoundRect(38, 179, 313, 241, 4, 4, paint);
            paint.setColor(0xffc49053); canvas.drawRect(49, 181, 302, 184, paint);

            float keyWidth = 250f / 14;
            // Draw all white keys first so subsequent keys cannot cover their black neighbors.
            for (int key = 0; key < 14; key++) {
                float left = 50 + key * keyWidth;
                paint.setShader(new LinearGradient(0, 186, 0, 230,
                        new int[]{0xffe4e4df, 0xfffffdf3}, null, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(left, 186, left + keyWidth - 1, 229, 1.5f, 1.5f, paint);
                paint.setShader(null); paint.setColor(0xffc1c8d0);
                canvas.drawRect(left + .5f, 229, left + keyWidth - 1, 233, paint);
            }
            for (int key = 0; key < 14; key++) {
                int note = key % 7;
                if (note == 2 || note == 6) continue;
                float left = 50 + (key + .7f) * keyWidth;
                float right = 50 + (key + 1.3f) * keyWidth;
                paint.setColor(0xff111c2b); canvas.drawRoundRect(left, 185, right, 213, 1.5f, 1.5f, paint);
                paint.setColor(0xff35465d); canvas.drawRoundRect(left + 1, 185, right - 1, 208, 1, 1, paint);
                paint.setColor(0xff66768a); paint.setStrokeWidth(.7f); canvas.drawLine(left + 1, 187, right - 1, 187, paint);
            }
            paint.setColor(0xff4977b2); canvas.drawRoundRect(35, 235, 315, 245, 2, 2, paint);
            paint.setColor(0xff89abd4); paint.setStrokeWidth(1.4f); canvas.drawLine(38, 236, 312, 236, paint);
            paint.setColor(0xff213d67); canvas.drawRoundRect(37, 178, 49, 239, 2, 2, paint);
            canvas.drawRoundRect(301, 178, 313, 239, 2, 2, paint);

            for (int pedal = 0; pedal < 3; pedal++) {
                float x = 155 + pedal * 19;
                paint.setColor(0xff967044); canvas.drawRoundRect(x - 2, 299, x + 2, 323, 2, 2, paint);
                paint.setColor(0xffd7ad68); canvas.drawRoundRect(x - 5, 316, x + 5, 331, 4, 4, paint);
                paint.setColor(0xffffdfa0); paint.setStrokeWidth(1.4f); canvas.drawLine(x - 2, 319, x - 2, 326, paint);
            }
            canvas.restore();
        }
        private void drawGuitar(Canvas canvas, float width, float height) {
            float scale = Math.min(width / 350f, height / 410f) * .94f;
            canvas.save();
            canvas.translate(width / 2, height / 2);
            canvas.scale(scale, scale);
            canvas.rotate(28);
            canvas.translate(-160, -210);

            Path body = new Path();
            body.moveTo(160, 207);
            body.cubicTo(140, 207, 131, 200, 115, 211);
            body.cubicTo(94, 225, 96, 251, 109, 267);
            body.cubicTo(120, 282, 109, 289, 95, 305);
            body.cubicTo(63, 341, 85, 383, 126, 391);
            body.cubicTo(145, 395, 175, 395, 194, 391);
            body.cubicTo(235, 383, 257, 341, 225, 305);
            body.cubicTo(211, 289, 200, 282, 211, 267);
            body.cubicTo(224, 251, 226, 225, 205, 211);
            body.cubicTo(189, 200, 180, 207, 160, 207);
            body.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(91, 235, 225, 379,
                    new int[]{0xfff2c982, 0xffdfa34f, 0xffbd7432}, null, Shader.TileMode.CLAMP));
            canvas.drawPath(body, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(5); paint.setColor(0xff87502c);
            canvas.drawPath(body, paint);
            canvas.save(); canvas.translate(160, 300); canvas.scale(.95f, .96f); canvas.translate(-160, -300);
            paint.setStrokeWidth(2); paint.setColor(0xffffe3ab); canvas.drawPath(body, paint); canvas.restore();
            paint.setStyle(Paint.Style.FILL);

            // The neck, frets, and tuning machines give the silhouette its guitar identity.
            paint.setColor(0xff9b5f36); canvas.drawRoundRect(143, 93, 177, 239, 3, 3, paint);
            paint.setColor(0xff4c352e); canvas.drawRect(147, 98, 173, 239, paint);
            paint.setStrokeWidth(1.4f); paint.setColor(0xffbaa38a);
            for (int fret = 0; fret < 12; fret++) {
                float y = 110 + fret * 10.5f; canvas.drawLine(148, y, 172, y, paint);
            }
            paint.setColor(0xfff6e6cd);
            for (int fret : new int[]{2, 4, 6, 8}) canvas.drawCircle(160, 104 + fret * 10.5f, 1.8f, paint);
            canvas.drawCircle(155, 230, 1.6f, paint); canvas.drawCircle(165, 230, 1.6f, paint);

            paint.setColor(0xffa6a9ad);
            for (int peg = 0; peg < 3; peg++) {
                float y = 47 + peg * 20;
                canvas.drawRoundRect(131, y - 2, 149, y + 2, 2, 2, paint);
                canvas.drawRoundRect(171, y - 2, 189, y + 2, 2, 2, paint);
                canvas.drawRoundRect(127, y - 5, 136, y + 5, 3, 3, paint);
                canvas.drawRoundRect(184, y - 5, 193, y + 5, 3, 3, paint);
            }
            Path head = new Path();
            head.moveTo(145, 28); head.quadTo(160, 22, 175, 28);
            head.lineTo(177, 92); head.quadTo(174, 100, 171, 104);
            head.lineTo(149, 104); head.quadTo(146, 100, 143, 92); head.close();
            paint.setColor(0xff9b5f36); canvas.drawPath(head, paint);
            paint.setColor(0xfff8dfae); canvas.drawRect(147, 101, 173, 105, paint);
            for (int peg = 0; peg < 3; peg++) {
                paint.setColor(0xffddd4c1);
                canvas.drawCircle(151, 47 + peg * 20, 3.5f, paint);
                canvas.drawCircle(169, 47 + peg * 20, 3.5f, paint);
                paint.setColor(0xff6d5039);
                canvas.drawCircle(151, 47 + peg * 20, 1.6f, paint);
                canvas.drawCircle(169, 47 + peg * 20, 1.6f, paint);
            }

            paint.setColor(0xff8d522d); canvas.drawCircle(160, 273, 33, paint);
            paint.setColor(0xfff7dfab); canvas.drawCircle(160, 273, 30.5f, paint);
            paint.setColor(0xff7b492c); canvas.drawCircle(160, 273, 28.5f, paint);
            paint.setColor(0xff302825); canvas.drawCircle(160, 273, 26, paint);
            Path guard = new Path();
            guard.moveTo(188, 268); guard.cubicTo(207, 280, 214, 306, 196, 320);
            guard.quadTo(178, 324, 173, 310); guard.quadTo(195, 298, 188, 268); guard.close();
            paint.setColor(0xff764331); canvas.drawPath(guard, paint);
            paint.setColor(0xff573b2e); canvas.drawRoundRect(130, 343, 190, 358, 4, 4, paint);
            paint.setColor(0xfff8e5c4); canvas.drawRoundRect(138, 346, 182, 349, 1, 1, paint);

            // Six continuous strings run from the bridge to the tuning pegs.
            for (int string = 0; string < 6; string++) {
                float neckX = 151 + string * 3.6f;
                float bridgeX = 142 + string * 7.2f;
                paint.setStrokeWidth(.65f + (5 - string) * .09f); paint.setColor(0xfff8eaca);
                canvas.drawLine(bridgeX, 351, neckX, 103, paint);
                int peg = string < 3 ? 2 - string : string - 3;
                canvas.drawLine(neckX, 103, string < 3 ? 151 : 169, 47 + peg * 20, paint);
                paint.setColor(0xffeee2c7); canvas.drawCircle(bridgeX, 353, 1.7f, paint);
            }
            canvas.restore();
        }
        @Override protected void onDraw(Canvas canvas) {
            float width = getWidth(), height = getHeight();
            if (instrument.equals("Piano")) {
                drawPiano(canvas, width, height);
            } else {
                drawGuitar(canvas, width, height);
            }
        }
    }
}
