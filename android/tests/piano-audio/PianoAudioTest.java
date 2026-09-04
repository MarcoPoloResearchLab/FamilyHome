package com.mprlab.portal.pianotest;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public final class PianoAudioTest extends Instrumentation {
    private AudioManager audio;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        Activity activity = null;
        try {
            audio = (AudioManager) getTargetContext().getSystemService(Context.AUDIO_SERVICE);
            activity = startActivitySync(new Intent().setClassName("com.mprlab.portal", "com.mprlab.portal.PianoActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            View keyboard = findKeyboard(activity.getWindow().getDecorView());
            if (keyboard == null) throw new AssertionError("Missing piano keyboard");
            String[] names = {"C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B"};
            int[] whiteNotes = {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23};
            int[] blackNotes = {1, 3, 6, 8, 10, 13, 15, 18, 20, 22};
            int[] boundaries = {1, 2, 4, 5, 6, 8, 9, 11, 12, 13};
            float inset = 4 * activity.getResources().getDisplayMetrics().density;
            float width = (keyboard.getWidth() - 2 * inset) / 14;
            for (int index = 0; index < whiteNotes.length; index++) {
                int note = whiteNotes[index];
                tapAndVerify(keyboard, inset + (index + 0.5f) * width, keyboard.getHeight() * 0.85f,
                        names[note % 12] + (4 + note / 12));
            }
            for (int index = 0; index < blackNotes.length; index++) {
                int note = blackNotes[index];
                tapAndVerify(keyboard, inset + boundaries[index] * width, keyboard.getHeight() * 0.25f,
                        names[note % 12] + (4 + note / 12));
            }
            // Replay a cached note while it is playing, then after it has finished.
            float x = inset + width / 2;
            float y = keyboard.getHeight() * 0.85f;
            tap(keyboard, x, y);
            awaitAudio(true);
            SystemClock.sleep(700);
            tap(keyboard, x, y);
            SystemClock.sleep(800);
            if (!audio.isMusicActive()) throw new AssertionError("Repeated note did not restart");
            awaitAudio(false);
            tapAndVerify(keyboard, x, y, "C4");
            result.putString("stream", "Piano audio passed: 24 keys and repeated-note playback.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "Piano audio failed: " + error + "\n");
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            if (activity != null) {
                Activity current = activity;
                runOnMainSync(current::finish);
            }
        }
    }

    private void tapAndVerify(View keyboard, float x, float y, String name) {
        awaitAudio(false);
        tap(keyboard, x, y);
        waitForIdleSync();
        if (!containsText(keyboard.getRootView(), name)) throw new AssertionError("Missing note " + name);
        awaitAudio(true);
        awaitAudio(false);
    }

    private void tap(View keyboard, float x, float y) {
        runOnMainSync(() -> {
            long now = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
            MotionEvent up = MotionEvent.obtain(now, now + 20, MotionEvent.ACTION_UP, x, y, 0);
            try {
                keyboard.dispatchTouchEvent(down);
                keyboard.dispatchTouchEvent(up);
            } finally {
                down.recycle();
                up.recycle();
            }
        });
    }

    private void awaitAudio(boolean playing) {
        long deadline = SystemClock.uptimeMillis() + 3000;
        while (audio.isMusicActive() != playing && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(20);
        if (audio.isMusicActive() != playing) throw new AssertionError("Expected music active=" + playing);
    }

    private View findKeyboard(View view) {
        if ("Two octave piano keyboard".contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                View found = findKeyboard(group.getChildAt(index));
                if (found != null) return found;
            }
        }
        return null;
    }

    private boolean containsText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                if (containsText(group.getChildAt(index), text)) return true;
            }
        }
        return false;
    }
}
