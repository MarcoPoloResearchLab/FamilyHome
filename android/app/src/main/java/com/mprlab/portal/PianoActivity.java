package com.mprlab.portal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public final class PianoActivity extends PortalActivity {
    private static final String TAG = "PianoActivity";
    private static final String AUDIO_ERROR = "Sound unavailable";
    private static final int BG = Color.rgb(255, 248, 234);
    private static final int SYSTEM_BAR = Color.rgb(36, 49, 71);
    private static final int INK = Color.rgb(36, 49, 71);
    private static final int MUTED = Color.rgb(92, 104, 124);
    private static final int PURPLE = Color.rgb(124, 92, 252);
    private static final int PALE_PURPLE = Color.rgb(241, 237, 255);
    private static final int BLUE = Color.rgb(67, 114, 235);

    private TextView noteReadout;
    private PianoView pianoView;
    private NotePlayer notePlayer;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Window window = getWindow();
        window.setStatusBarColor(SYSTEM_BAR);
        window.setNavigationBarColor(SYSTEM_BAR);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        notePlayer = new NotePlayer();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(22), dp(12), dp(20), dp(12));
        toolbar.setBackgroundColor(Color.WHITE);

        toolbar.addView(text("Piano", 27, INK, true), new LinearLayout.LayoutParams(0, -2, 1f));
        PortalToolbar.navigation(this, toolbar);

        noteReadout = text("Tap a key", 18, PURPLE, true);
        noteReadout.setGravity(Gravity.CENTER);
        noteReadout.setBackground(rounded(PALE_PURPLE, 18));
        noteReadout.setPadding(dp(22), dp(10), dp(22), dp(10));
        LinearLayout.LayoutParams readoutParams = new LinearLayout.LayoutParams(dp(180), dp(50));
        readoutParams.rightMargin = dp(14);
        toolbar.addView(noteReadout, readoutParams);

        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(PortalToolbar.HEIGHT_DP)));

        FrameLayout keyboardSurface = new FrameLayout(this);
        keyboardSurface.setPadding(dp(18), dp(18), dp(18), dp(18));
        pianoView = new PianoView();
        pianoView.setContentDescription("Two octave piano keyboard");
        keyboardSurface.addView(pianoView, new FrameLayout.LayoutParams(-1, -1));
        root.addView(keyboardSurface, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    @Override protected void onPause() {
        if (pianoView != null) pianoView.clearTouches();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (notePlayer != null) notePlayer.close();
        super.onDestroy();
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class PianoView extends View {
        private static final int KEY_COUNT = 24;
        private final int[] whiteNotes = {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23};
        private final int[] blackNotes = {1, 3, 6, 8, 10, 13, 15, 18, 20, 22};
        private final int[] blackBoundaries = {1, 2, 4, 5, 6, 8, 9, 11, 12, 13};
        private final String[] noteNames = {"C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B"};
        private final int[] noteColors = {
                Color.rgb(255, 105, 105), Color.rgb(255, 145, 92), Color.rgb(255, 183, 77),
                Color.rgb(230, 190, 55), Color.rgb(103, 190, 104), Color.rgb(0, 166, 153),
                Color.rgb(45, 169, 220), Color.rgb(67, 114, 235), Color.rgb(124, 92, 252),
                Color.rgb(165, 92, 232), Color.rgb(232, 84, 145), Color.rgb(235, 96, 126)
        };
        private final RectF[] whiteRects = new RectF[whiteNotes.length];
        private final RectF[] blackRects = new RectF[blackNotes.length];
        private final int[] pressedCounts = new int[KEY_COUNT];
        private final SparseIntArray pointerKeys = new SparseIntArray();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float keyboardTop;
        private float keyboardBottom;

        PianoView() {
            super(PianoActivity.this);
            setFocusable(true);
            setClickable(true);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            float left = dp(4);
            float right = width - dp(4);
            keyboardTop = dp(4);
            keyboardBottom = height - dp(4);
            float whiteWidth = (right - left) / whiteNotes.length;
            for (int index = 0; index < whiteNotes.length; index++) {
                whiteRects[index] = new RectF(left + index * whiteWidth, keyboardTop,
                        left + (index + 1) * whiteWidth, keyboardBottom);
            }
            float blackWidth = whiteWidth * 0.62f;
            float blackBottom = keyboardTop + (keyboardBottom - keyboardTop) * 0.61f;
            for (int index = 0; index < blackNotes.length; index++) {
                float center = left + blackBoundaries[index] * whiteWidth;
                blackRects[index] = new RectF(center - blackWidth / 2f, keyboardTop,
                        center + blackWidth / 2f, blackBottom);
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setStrokeWidth(dp(2));
            paint.setTextAlign(Paint.Align.CENTER);
            for (int index = 0; index < whiteNotes.length; index++) {
                int note = whiteNotes[index];
                RectF key = whiteRects[index];
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(pressedCounts[note] > 0 ? pale(noteColors[note % 12]) : Color.WHITE);
                canvas.drawRoundRect(key, dp(10), dp(10), paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.rgb(216, 218, 226));
                canvas.drawRoundRect(key, dp(10), dp(10), paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(noteColors[note % 12]);
                canvas.drawCircle(key.centerX(), key.bottom - dp(48), dp(7), paint);
                paint.setTextSize(dp(20));
                paint.setColor(INK);
                canvas.drawText(noteNames[note % 12], key.centerX(), key.bottom - dp(17), paint);
            }

            for (int index = 0; index < blackNotes.length; index++) {
                int note = blackNotes[index];
                RectF key = blackRects[index];
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(pressedCounts[note] > 0 ? PURPLE : INK);
                canvas.drawRoundRect(key, 0, 0, paint);
                RectF foot = new RectF(key.left, key.bottom - dp(18), key.right, key.bottom);
                canvas.drawRoundRect(foot, dp(8), dp(8), paint);
                paint.setTextSize(dp(12));
                paint.setColor(Color.WHITE);
                canvas.drawText(noteNames[note % 12], key.centerX(), key.bottom - dp(10), paint);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            int actionIndex = event.getActionIndex();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                updatePointer(event.getPointerId(actionIndex), event.getX(actionIndex), event.getY(actionIndex));
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                for (int index = 0; index < event.getPointerCount(); index++) {
                    updatePointer(event.getPointerId(index), event.getX(index), event.getY(index));
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                releasePointer(event.getPointerId(actionIndex));
                if (action == MotionEvent.ACTION_UP) performClick();
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                clearTouches();
                return true;
            }
            return super.onTouchEvent(event);
        }

        @Override public boolean performClick() {
            super.performClick();
            return true;
        }

        void clearTouches() {
            for (int index = 0; index < pointerKeys.size(); index++) {
                int note = pointerKeys.valueAt(index);
                if (note >= 0) pressedCounts[note] = 0;
            }
            pointerKeys.clear();
            invalidate();
        }

        private void updatePointer(int pointerID, float x, float y) {
            int oldNote = pointerKeys.get(pointerID, -1);
            int newNote = hitTest(x, y);
            if (oldNote == newNote) return;
            if (oldNote >= 0) pressedCounts[oldNote] = Math.max(0, pressedCounts[oldNote] - 1);
            if (newNote >= 0) {
                pressedCounts[newNote]++;
                try {
                    notePlayer.play(newNote);
                    int octave = 4 + newNote / 12;
                    noteReadout.setText(String.format(Locale.US, "%s%d", noteNames[newNote % 12], octave));
                } catch (IllegalStateException error) {
                    Log.e(TAG, "Cannot play piano semitone " + newNote, error);
                    noteReadout.setText(AUDIO_ERROR);
                }
            }
            pointerKeys.put(pointerID, newNote);
            invalidate();
        }

        private void releasePointer(int pointerID) {
            int note = pointerKeys.get(pointerID, -1);
            if (note >= 0) pressedCounts[note] = Math.max(0, pressedCounts[note] - 1);
            pointerKeys.delete(pointerID);
            invalidate();
        }

        private int hitTest(float x, float y) {
            for (int index = 0; index < blackRects.length; index++) {
                if (blackRects[index] != null && blackRects[index].contains(x, y)) return blackNotes[index];
            }
            for (int index = 0; index < whiteRects.length; index++) {
                if (whiteRects[index] != null && whiteRects[index].contains(x, y)) return whiteNotes[index];
            }
            return -1;
        }

        private int pale(int color) {
            int red = (Color.red(color) + 255 * 3) / 4;
            int green = (Color.green(color) + 255 * 3) / 4;
            int blue = (Color.blue(color) + 255 * 3) / 4;
            return Color.rgb(red, green, blue);
        }
    }

    private static final class NotePlayer {
        private static final int SAMPLE_RATE = 22050;
        private static final double C4 = 261.625565;
        private final SparseArray<AudioTrack> tracks = new SparseArray<>();

        void play(int semitone) {
            AudioTrack track = tracks.get(semitone);
            if (track == null) {
                track = createTrack(semitone);
                tracks.put(semitone, track);
            }
            if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) track.stop();
            int result = track.setPlaybackHeadPosition(0);
            if (result != AudioTrack.SUCCESS) {
                throw new IllegalStateException("Cannot reset piano semitone " + semitone + ": " + result);
            }
            track.play();
        }

        void close() {
            for (int index = 0; index < tracks.size(); index++) {
                AudioTrack track = tracks.valueAt(index);
                track.release();
            }
            tracks.clear();
        }

        private AudioTrack createTrack(int semitone) {
            double frequency = C4 * Math.pow(2.0, semitone / 12.0);
            int sampleCount = (int) (SAMPLE_RATE * 1.35);
            short[] samples = new short[sampleCount];
            for (int index = 0; index < sampleCount; index++) {
                double time = index / (double) SAMPLE_RATE;
                double attack = Math.min(1.0, time / 0.012);
                double decay = Math.exp(-3.1 * time / 1.35);
                double phase = 2.0 * Math.PI * frequency * time;
                double wave = Math.sin(phase) + 0.34 * Math.sin(phase * 2.0) + 0.12 * Math.sin(phase * 3.0);
                samples[index] = (short) (Short.MAX_VALUE * 0.24 * attack * decay * wave / 1.46);
            }
            AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(samples.length * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();
            try {
                // MODE_STATIC becomes initialized only after samples are loaded.
                int written = track.write(samples, 0, samples.length);
                if (written != samples.length || track.getState() != AudioTrack.STATE_INITIALIZED) {
                    throw new IllegalStateException("Cannot load piano semitone " + semitone
                            + ": wrote " + written + " of " + samples.length + " samples");
                }
                return track;
            } catch (RuntimeException error) {
                track.release();
                throw error;
            }
        }
    }
}
