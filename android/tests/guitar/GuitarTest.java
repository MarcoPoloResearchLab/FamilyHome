package com.mprlab.portal.guitartest;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.AudioAttributes;
import android.media.AudioPlaybackConfiguration;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;

public final class GuitarTest extends Instrumentation {
    private static final String PACKAGE = "com.mprlab.portal";
    private AudioManager audio;
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            audio = (AudioManager) getTargetContext().getSystemService(Context.AUDIO_SERVICE);
            boolean saved = getTargetContext().getSharedPreferences("children_portal", Context.MODE_PRIVATE).edit()
                    .putString("profiles_json", "[{\"id\":\"music-test\",\"name\":\"Music Test\"}]")
                    .putString("active_profile_id", "music-test").commit();
            if (!saved) throw new AssertionError("Cannot save test profile");
            Activity home = startActivitySync(new Intent().setClassName(PACKAGE, PACKAGE + ".MainActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            Activity music = navigate(home, "Music. Choose an instrument", "MusicActivity");
            Activity piano = navigate(music, "Piano", "PianoActivity");
            runOnMainSync(piano::finish); waitForIdleSync();
            Activity guitar = navigate(music, "Guitar", "GuitarActivity");
            waitForIdleSync();
            View board = required(guitar, "Guitar fretboard");
            bounds(guitar.getWindow().getDecorView());
            if (guitar.getVolumeControlStream() != AudioManager.STREAM_MUSIC) throw new AssertionError("Wrong volume stream");
            String[] names = {"E4", "B3", "G3", "D3", "A2", "E2"};
            for (int string = 0; string < 6; string++) {
                tap(board, .07f, (.5f + string) / 6);
                text(guitar, "String " + (string + 1) + " · Open · " + names[string]);
                active(true);
            }
            String[] highNotes = {"F4", "F♯4", "G4", "G♯4", "A4"};
            for (int fret = 1; fret <= 5; fret++) {
                tap(board, .14f + (fret - .5f) * .108f, .5f / 6);
                text(guitar, "String 1 · Fret " + fret + " · " + highNotes[fret - 1]);
            }
            click(guitar, "C");
            text(guitar, "C chord · Swipe to strum");
            chordState(board, "String 1: E4, fret 0; String 2: C4, fret 1; String 3: G3, fret 0; String 4: E3, fret 2; String 5: C3, fret 3; String 6: muted");
            // A single fast move must cross every string, including a muted low E.
            swipe(board, .86f, .99f, .01f);
            voices(5);
            for (String chord : new String[]{"G", "Am", "F"}) {
                click(guitar, chord);
                text(guitar, chord + " chord · Swipe to strum");
                if (chord.equals("G")) chordState(board, "String 1: G4, fret 3; String 2: B3, fret 0; String 3: G3, fret 0; String 4: D3, fret 0; String 5: B2, fret 2; String 6: G2, fret 3");
                if (chord.equals("Am")) chordState(board, "String 1: E4, fret 0; String 2: C4, fret 1; String 3: A3, fret 2; String 4: E3, fret 2; String 5: A2, fret 0; String 6: muted");
                if (chord.equals("F")) chordState(board, "String 1: F4, fret 1; String 2: C4, fret 1; String 3: A3, fret 2; String 4: F3, fret 3; String 5: C3, fret 3; String 6: F2, fret 1");
                swipe(board, .86f, .01f, 1.1f);
                voices(chord.equals("Am") ? 5 : 6);
            }
            click(guitar, "Open strings");
            tap(board, .07f, .5f / 6);
            SystemClock.sleep(1200);
            tap(board, .07f, .5f / 6);
            SystemClock.sleep(1000);
            active(true);
            // Two independent fingers select different strings during one gesture.
            runOnMainSync(() -> multiTouch(board));
            active(true);
            click(guitar, "C");
            text(guitar, "C chord · Swipe to strum");
            SystemClock.sleep(200);
            File screenshot = new File(getTargetContext().getExternalFilesDir(null), "guitar.png");
            try (FileOutputStream output = new FileOutputStream(screenshot)) {
                getUiAutomation().takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, output);
            }
            tap(board, .86f, 2.5f / 6);
            runOnMainSync(guitar::finish);
            waitForIdleSync();
            active(false);
            Activity reopened = navigate(music, "Guitar", "GuitarActivity");
            tap(required(reopened, "Guitar fretboard"), .07f, .5f / 6);
            active(true);
            click(reopened, "Home");
            waitForIdleSync(); active(false);
            result.putString("stream", "Guitar passed: Music navigation, tuning, five frets, chords, strums, multitouch, replay, and exit cleanup.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "Guitar failed: " + error + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private Activity navigate(Activity source, String label, String target) {
        ActivityMonitor monitor = addMonitor(PACKAGE + "." + target, null, false);
        click(source, label);
        Activity result = waitForMonitorWithTimeout(monitor, 4000);
        removeMonitor(monitor);
        if (result == null) throw new AssertionError("Navigation missing: " + target);
        waitForIdleSync(); return result;
    }
    private void click(Activity activity, String label) {
        View view = required(activity, label);
        runOnMainSync(() -> { if (!view.performClick()) throw new AssertionError("Cannot click " + label); });
        waitForIdleSync();
    }
    private View required(Activity activity, String label) {
        View result = find(activity.getWindow().getDecorView(), label);
        if (result == null) throw new AssertionError("Missing control: " + label);
        return result;
    }
    private View find(View view, String label) {
        if (label.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                View match = find(group.getChildAt(index), label); if (match != null) return match;
            }
        }
        return null;
    }
    private void text(Activity activity, String expected) {
        final boolean[] found = {false};
        runOnMainSync(() -> found[0] = containsText(activity.getWindow().getDecorView(), expected));
        if (!found[0]) throw new AssertionError("Missing text: " + expected);
    }
    private boolean containsText(View view, String value) {
        if (view instanceof TextView && value.contentEquals(((TextView) view).getText())) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) if (containsText(group.getChildAt(index), value)) return true;
        }
        return false;
    }
    private void chordState(View board, String expected) {
        final String[] actual = {null};
        runOnMainSync(() -> actual[0] = board.createAccessibilityNodeInfo().getText().toString());
        if (!expected.equals(actual[0])) throw new AssertionError("Wrong chord positions: " + actual[0]);
    }
    private void voices(int expected) {
        long deadline = SystemClock.uptimeMillis() + 1000;
        int count;
        do {
            count = 0;
            for (AudioPlaybackConfiguration config : audio.getActivePlaybackConfigurations()) {
                if (config.getAudioAttributes().getUsage() == AudioAttributes.USAGE_GAME) count++;
            }
            if (count == expected) return;
            SystemClock.sleep(20);
        } while (SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Expected " + expected + " guitar strings, got " + count);
    }
    private void active(boolean expected) {
        long end = SystemClock.uptimeMillis() + 3000;
        while (audio.isMusicActive() != expected && SystemClock.uptimeMillis() < end) SystemClock.sleep(20);
        if (audio.isMusicActive() != expected) throw new AssertionError("Music active must be " + expected);
    }
    private void tap(View view, float x, float y) { swipe(view, x, y, y); }
    private void swipe(View view, float x, float from, float to) {
        runOnMainSync(() -> {
            long now = SystemClock.uptimeMillis();
            event(view, now, MotionEvent.ACTION_DOWN, x, from);
            event(view, now, MotionEvent.ACTION_MOVE, x, to);
            event(view, now, MotionEvent.ACTION_UP, x, to);
        });
        waitForIdleSync();
    }
    private void event(View view, long time, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(time, time, action, x * view.getWidth(), y * view.getHeight(), 0);
        try { view.dispatchTouchEvent(event); } finally { event.recycle(); }
    }
    private void multiTouch(View view) {
        long now = SystemClock.uptimeMillis();
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[2];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[2];
        for (int i = 0; i < 2; i++) {
            properties[i] = new MotionEvent.PointerProperties(); properties[i].id = i;
            properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coords[i] = new MotionEvent.PointerCoords(); coords[i].x = .3f * view.getWidth();
            coords[i].y = (.5f + i * 4) / 6 * view.getHeight(); coords[i].pressure = 1; coords[i].size = 1;
        }
        event(view, now, MotionEvent.ACTION_DOWN, .3f, .5f / 6);
        MotionEvent second = MotionEvent.obtain(now, now, MotionEvent.ACTION_POINTER_DOWN | (1 << 8), 2,
                properties, coords, 0, 0, 1, 1, 0, 0, 0, 0);
        try { view.dispatchTouchEvent(second); } finally { second.recycle(); }
        event(view, now, MotionEvent.ACTION_CANCEL, .3f, .5f / 6);
    }
    private void bounds(View view) {
        if (view.getVisibility() != View.VISIBLE) return;
        Rect rect = new Rect();
        if (!view.getGlobalVisibleRect(rect)) throw new AssertionError("Invisible view: " + view);
        if (view instanceof TextView && (rect.width() < view.getWidth() || rect.height() < view.getHeight())) throw new AssertionError("Clipped control");
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) bounds(group.getChildAt(index));
        }
    }
}
