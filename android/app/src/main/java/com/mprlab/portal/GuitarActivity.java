package com.mprlab.portal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Arrays;

public final class GuitarActivity extends PortalActivity {
    private static final String TAG = "GuitarActivity";
    private static final String FRETS_STATE = "guitar_frets";
    private static final String CHORD_STATE = "guitar_chord";
    private static final int INK = Color.rgb(36, 49, 71);
    private static final int PURPLE = Color.rgb(124, 92, 252);
    private static final int BACKGROUND = Color.rgb(255, 248, 234);
    private static final int MUTED_STRING = -1;
    private static final float NUT = .14f, NECK_END = .68f, FRET_WIDTH = .108f, STRUM_START = .76f, BRIDGE = .96f;
    private static final String[] NOTE_NAMES = {"C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B"};
    private static final int[] COLORS = {0xffe95a73, 0xffbd73de, 0xff6489ef, 0xff30aca1, 0xffd49b32, 0xffcf7546};
    private enum Chord {
        C("C", new int[]{0, 1, 0, 2, 3, -1}),
        G("G", new int[]{3, 0, 0, 0, 2, 3}),
        AM("Am", new int[]{0, 1, 2, 2, 0, -1}),
        F("F", new int[]{1, 1, 2, 3, 3, 1});
        final String label;
        final int[] frets;
        Chord(String label, int[] frets) { this.label = label; this.frets = frets; }
    }
    private final int[] selected = new int[GuitarPlayer.STRING_COUNT];
    private final Button[] chordButtons = new Button[Chord.values().length];
    private Chord chord;
    private GuitarPlayer player;
    private Fretboard board;
    private TextView readout;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (state != null) {
            int[] frets = state.getIntArray(FRETS_STATE);
            if (frets != null) System.arraycopy(frets, 0, selected, 0, selected.length);
            String name = state.getString(CHORD_STATE);
            if (name != null) chord = Chord.valueOf(name);
        }
        player = new GuitarPlayer();
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.addView(text("Guitar", 27, INK), new LinearLayout.LayoutParams(dp(112), -2));
        readout = text("Tap a string to play", 18, PURPLE);
        readout.setPadding(dp(16), 0, dp(12), 0);
        readout.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        readout.setSingleLine(true);
        readout.setEllipsize(android.text.TextUtils.TruncateAt.END);

        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(12), dp(22), dp(14));
        for (Chord preset : Chord.values()) {
            Button button = button(preset.label);
            button.setOnClickListener(view -> selectChord(preset));
            chordButtons[preset.ordinal()] = button;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(64), dp(56));
            params.rightMargin = dp(10); toolbar.addView(button, params);
        }
        Button open = button("Open strings");
        open.setOnClickListener(view -> {
            player.close(); Arrays.fill(selected, 0); chord = null;
            board.clearTouches(); updateChordButtons(); readout.setText("Open strings · Swipe to strum"); board.invalidate();
        });
        toolbar.addView(open, new LinearLayout.LayoutParams(dp(148), dp(56)));
        toolbar.addView(readout, new LinearLayout.LayoutParams(0, -2, 1));
        TextView hint = text("Tap a fret for a higher note. Swipe the strings on the right to strum.", 17, INK);
        hint.setPadding(0, dp(12), 0, dp(6)); content.addView(hint);
        content.addView(new FretLabels(), new LinearLayout.LayoutParams(-1, dp(32)));
        board = new Fretboard();
        content.addView(board, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView footer = text("Shorter string = higher note     •     Dots show where to put your fingers", 16, INK);
        footer.setGravity(Gravity.CENTER); footer.setPadding(0, dp(10), 0, 0); content.addView(footer);
        updateChordButtons();
        if (chord != null) readout.setText(chord.label + " chord · Swipe to strum");
        setContentView(PortalToolbar.screen(this, toolbar, content, BACKGROUND));
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putIntArray(FRETS_STATE, selected);
        if (chord != null) state.putString(CHORD_STATE, chord.name());
        super.onSaveInstanceState(state);
    }
    @Override protected void onPause() {
        board.clearTouches(); player.close(); super.onPause();
    }
    @Override protected void onDestroy() { player.close(); super.onDestroy(); }
    private void selectChord(Chord preset) {
        player.close(); chord = preset;
        System.arraycopy(preset.frets, 0, selected, 0, selected.length);
        board.clearTouches(); updateChordButtons(); readout.setText(preset.label + " chord · Swipe to strum"); board.invalidate();
    }
    private void updateChordButtons() {
        for (Chord preset : Chord.values()) {
            Button button = chordButtons[preset.ordinal()]; boolean active = chord == preset;
            button.setSelected(active); button.setTextColor(active ? Color.WHITE : INK);
            button.setBackground(rounded(active ? PURPLE : Color.WHITE, 16));
        }
    }
    private void selectNote(int string, int fret) {
        selected[string] = fret; chord = null; updateChordButtons();
        readout.setText("String " + (string + 1) + " · " + (fret == 0 ? "Open" : "Fret " + fret) + " · " + noteName(string));
        pluck(string);
    }
    private String noteName(int string) {
        int midi = GuitarPlayer.OPEN_MIDI[string] + selected[string];
        return NOTE_NAMES[midi % 12] + (midi / 12 - 1);
    }
    private void pluck(int string) {
        if (selected[string] == MUTED_STRING) return;
        try {
            player.play(string, selected[string]);
            board.pluckedAt[string] = SystemClock.uptimeMillis(); board.invalidate();
        } catch (IllegalStateException error) {
            Log.e(TAG, "Cannot play guitar string " + (string + 1), error);
            readout.setText("Sound unavailable");
        }
    }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return view;
    }
    private Button button(String label) {
        Button view = new Button(this); view.setText(label); view.setAllCaps(false); view.setTextSize(20);
        view.setContentDescription(label); view.setTextColor(INK); view.setBackground(rounded(Color.WHITE, 16)); return view;
    }
    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(color); shape.setCornerRadius(dp(radius)); return shape;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private final class FretLabels extends View {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        FretLabels() { super(GuitarActivity.this); }
        @Override protected void onDraw(Canvas canvas) {
            paint.setColor(INK); paint.setTextSize(dp(14)); paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText("OPEN", getWidth() * .07f, dp(22), paint);
            for (int fret = 1; fret <= GuitarPlayer.FRET_COUNT; fret++) {
                canvas.drawText(Integer.toString(fret), getWidth() * (NUT + (fret - .5f) * FRET_WIDTH), dp(22), paint);
            }
            canvas.drawText("STRUM ↕", getWidth() * .86f, dp(22), paint);
        }
    }
    private static final class Finger {
        final int string, fret;
        final boolean strumming;
        Finger(int string, int fret, boolean strumming) { this.string = string; this.fret = fret; this.strumming = strumming; }
    }
    private final class Fretboard extends View {
        private static final int NOTE_ACTION = 1000;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final SparseArray<Finger> fingers = new SparseArray<>();
        private final long[] pluckedAt = new long[GuitarPlayer.STRING_COUNT];
        Fretboard() { super(GuitarActivity.this); setContentDescription("Guitar fretboard"); setFocusable(true); setClickable(true); }
        @Override protected void onDraw(Canvas canvas) {
            float width = getWidth(), height = getHeight(), row = height / GuitarPlayer.STRING_COUNT;
            paint.setStyle(Paint.Style.FILL); paint.setColor(0xfff2e3ca);
            canvas.drawRoundRect(width * NUT, 0, width * NECK_END, height, dp(14), dp(14), paint);
            paint.setColor(0xffeee6fc);
            canvas.drawRoundRect(width * STRUM_START, 0, width, height, dp(22), dp(22), paint);
            paint.setColor(0xffaa916b); paint.setStrokeWidth(dp(2));
            for (int fret = 0; fret <= GuitarPlayer.FRET_COUNT; fret++) {
                float x = width * (NUT + fret * FRET_WIDTH); canvas.drawLine(x, dp(8), x, height - dp(8), paint);
            }
            paint.setColor(INK); paint.setStrokeWidth(dp(5));
            canvas.drawLine(width * BRIDGE, dp(16), width * BRIDGE, height - dp(16), paint);
            boolean animating = false;
            long now = SystemClock.uptimeMillis();
            for (int string = 0; string < GuitarPlayer.STRING_COUNT; string++) {
                float y = row * (string + .5f);
                int fret = selected[string]; boolean muted = fret == MUTED_STRING;
                float start = width * (NUT + Math.max(0, fret) * FRET_WIDTH);
                float end = width * BRIDGE;
                paint.setStrokeWidth(dp(2) + string * .45f); paint.setColor(0xffc3b9a9);
                canvas.drawLine(width * NUT, y, end, y, paint);
                long age = now - pluckedAt[string];
                if (!muted) {
                    paint.setColor(COLORS[string]); paint.setStyle(Paint.Style.STROKE);
                    path.reset(); path.moveTo(start, y);
                    float amplitude = age < 850 ? dp(5) * (1 - age / 850f) : 0;
                    if (amplitude > 0) animating = true;
                    for (int step = 1; step <= 48; step++) {
                        float ratio = step / 48f;
                        float offset = amplitude * (float) (Math.sin(Math.PI * ratio) * Math.sin(ratio * Math.PI * 8 + age * .06));
                        path.lineTo(start + (end - start) * ratio, y + offset);
                    }
                    canvas.drawPath(path, paint); paint.setStyle(Paint.Style.FILL);
                }
                float marker = width * (fret <= 0 ? .07f : NUT + (fret - .5f) * FRET_WIDTH);
                paint.setColor(muted ? 0xffaaa59e : COLORS[string]);
                canvas.drawCircle(marker, y, Math.min(dp(21), row * .29f), paint);
                paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setColor(Color.WHITE); paint.setTextSize(dp(17));
                canvas.drawText(muted ? "×" : Integer.toString(fret), marker, y + dp(6), paint);
                if (fret > 0) {
                    paint.setColor(INK); paint.setTextSize(dp(17)); canvas.drawText(noteName(string), width * .07f, y + dp(6), paint);
                } else if (!muted) {
                    paint.setColor(INK); paint.setTextSize(dp(12)); canvas.drawText(noteName(string), width * .07f, y + Math.min(dp(36), row * .46f), paint);
                }
            }
            if (animating) postInvalidateOnAnimation();
        }
        void clearTouches() { fingers.clear(); Arrays.fill(pluckedAt, 0); invalidate(); }
        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked(), index = event.getActionIndex();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                update(event.getPointerId(index), event.getX(index), event.getY(index)); return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                for (int history = 0; history < event.getHistorySize(); history++) {
                    for (int pointer = 0; pointer < event.getPointerCount(); pointer++) update(event.getPointerId(pointer), event.getHistoricalX(pointer, history), event.getHistoricalY(pointer, history));
                }
                for (int pointer = 0; pointer < event.getPointerCount(); pointer++) update(event.getPointerId(pointer), event.getX(pointer), event.getY(pointer));
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                update(event.getPointerId(index), event.getX(index), event.getY(index));
                fingers.remove(event.getPointerId(index)); if (action == MotionEvent.ACTION_UP) performClick(); return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) { fingers.clear(); return true; }
            return super.onTouchEvent(event);
        }
        private void update(int pointer, float x, float y) {
            Finger previous = fingers.get(pointer);
            if (x >= getWidth() * STRUM_START && x < getWidth() && previous != null && previous.strumming) {
                y = Math.max(0, Math.min(getHeight() - 1, y));
            }
            if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) { fingers.remove(pointer); return; }
            float position = x / getWidth();
            boolean strumming = position >= STRUM_START;
            if (position > NECK_END && !strumming) { fingers.remove(pointer); return; }
            int string = (int) (y / getHeight() * GuitarPlayer.STRING_COUNT);
            int fret = position < NUT ? 0 : Math.min(GuitarPlayer.FRET_COUNT, 1 + (int) ((position - NUT) / FRET_WIDTH));
            if (strumming) {
                if (previous != null && previous.strumming) {
                    int direction = Integer.compare(string, previous.string);
                    for (int crossed = previous.string + direction; direction != 0 && crossed != string + direction; crossed += direction) pluck(crossed);
                } else pluck(string);
            } else if (previous == null || previous.strumming || previous.string != string || previous.fret != fret) selectNote(string, fret);
            fingers.put(pointer, new Finger(string, fret, strumming));
            invalidate();
        }
        @Override public boolean performClick() { super.performClick(); return true; }
        @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            StringBuilder state = new StringBuilder();
            for (int string = 0; string < GuitarPlayer.STRING_COUNT; string++) {
                if (string > 0) state.append("; ");
                state.append("String ").append(string + 1).append(": ");
                if (selected[string] == MUTED_STRING) state.append("muted");
                else state.append(noteName(string)).append(", fret ").append(selected[string]);
            }
            info.setText(state);
            for (int string = 0; string < GuitarPlayer.STRING_COUNT; string++) {
                for (int fret = 0; fret <= GuitarPlayer.FRET_COUNT; fret++) {
                    int action = NOTE_ACTION + string * (GuitarPlayer.FRET_COUNT + 1) + fret;
                    info.addAction(new AccessibilityNodeInfo.AccessibilityAction(action, "Play string " + (string + 1) + ", fret " + fret));
                }
            }
        }
        @Override public boolean performAccessibilityAction(int action, Bundle args) {
            int note = action - NOTE_ACTION;
            if (note >= 0 && note < GuitarPlayer.STRING_COUNT * (GuitarPlayer.FRET_COUNT + 1)) {
                selectNote(note / (GuitarPlayer.FRET_COUNT + 1), note % (GuitarPlayer.FRET_COUNT + 1)); return true;
            }
            return super.performAccessibilityAction(action, args);
        }
    }
}
