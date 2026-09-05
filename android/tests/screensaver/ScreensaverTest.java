package com.mprlab.portal.screensavertest;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

public final class ScreensaverTest extends Instrumentation {
    private Activity activity;
    private String phase;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        phase = arguments.getString("phase", "behavior");
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            if (phase.equals("persistence")) {
                activity = open("SettingsActivity");
                check(() -> {
                    equal(selected("Screensaver mode"), "Clock");
                    equal(selected("Screensaver timeout"), "30 seconds");
                    activity.finish();
                });
                result.putString("stream", "Screensaver passed: selections survive process restart.\n");
                finish(Activity.RESULT_OK, result);
                return;
            }
            getTargetContext().getSharedPreferences("children_portal", Context.MODE_PRIVATE).edit()
                    .putString("profiles_json", "[{\"id\":\"saver-test\",\"name\":\"Saver Test\"}]")
                    .putString("active_profile_id", "saver-test").commit();
            getTargetContext().getSharedPreferences("screensaver", Context.MODE_PRIVATE).edit().clear().commit();
            android.accessibilityservice.AccessibilityServiceInfo info = getUiAutomation().getServiceInfo();
            info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            getUiAutomation().setServiceInfo(info);
            activity = open("SettingsActivity");
            if (phase.equals("typing") || phase.equals("dialogs")) {
                choose("Screensaver timeout", "30 seconds");
                if (phase.equals("typing")) typingScenario(); else dialogScenario();
                result.putString("stream", "Screensaver passed: " + phase + ".\n");
                finish(Activity.RESULT_OK, result);
                return;
            }
            check(() -> {
                require("Screensaver mode");
                require("Screensaver timeout");
                equal(selected("Screensaver mode"), "Black screen");
                equal(selected("Screensaver timeout"), "5 minutes");
                android.widget.TextView selected = (android.widget.TextView) ((Spinner) require("Screensaver mode")).getSelectedView();
                if (Color.luminance(selected.getCurrentTextColor()) > .3f)
                    throw new AssertionError("Screensaver selection has insufficient contrast on its light background");
            });
            check(() -> {
                View field = findEditor(activity.getWindow().getDecorView());
                if (field == null) throw new AssertionError("Weather field missing");
                field.requestFocus();
                ((android.view.inputmethod.InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(field, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            });
            awaitKeyboard(true);
            check(() -> require("Preview screensaver").performClick());
            awaitSaver(true, 2000);
            awaitKeyboard(false);
            tap(50, 50);
            choose("Screensaver mode", "Clock");
            choose("Screensaver timeout", "30 seconds");
            check(() -> require("Preview screensaver").requestRectangleOnScreen(
                    new android.graphics.Rect(0, 0, require("Preview screensaver").getWidth(),
                            require("Preview screensaver").getHeight()), true));
            screenshot("settings");
            check(() -> require("Preview screensaver").performClick());
            awaitSaver(true, 2000);
            screenshot("clock");
            if (saverNode("Clock screensaver. Tap to return") == null) throw new AssertionError("Clock preview absent");
            tap(50, 50);
            awaitSaver(false, 2000);
            choose("Screensaver mode", "Black screen");
            check(() -> activity.finish());
            activity = open("SettingsActivity");
            check(() -> {
                equal(selected("Screensaver mode"), "Black screen");
                equal(selected("Screensaver timeout"), "30 seconds");
                activity.finish();
            });
            activity = open("MusicActivity");
            float[] brightness = new float[1];
            check(() -> brightness[0] = activity.getWindow().getAttributes().screenBrightness);
            SystemClock.sleep(17000);
            tap(10, 200);
            SystemClock.sleep(17000);
            awaitSaver(false, 500);
            awaitSaver(true, 15000);
            screenshot("black");
            Bitmap black = getUiAutomation().takeScreenshot();
            for (int x = 0; x < black.getWidth(); x += 8) {
                for (int y = 0; y < black.getHeight(); y += 8) {
                    if (black.getPixel(x, y) != Color.BLACK) throw new AssertionError("Black screen has visible pixels");
                }
            }
            black.recycle();
            assertMinimumBrightness();
            // Wake over Back: the complete gesture must not navigate the underlying screen.
            tap(30, 30);
            awaitSaver(false, 2000);
            check(() -> {
                if (activity.isFinishing()) throw new AssertionError("Wake tap reached Back");
                if (activity.getWindow().getAttributes().screenBrightness != brightness[0])
                    throw new AssertionError("Brightness was not restored");
                activity.finish();
            });
            activity = open("SettingsActivity");
            choose("Screensaver mode", "Disabled");
            check(() -> {
                if (require("Preview screensaver").isEnabled()) throw new AssertionError("Disabled preview enabled");
                activity.finish();
            });
            activity = open("MusicActivity");
            SystemClock.sleep(31000);
            awaitSaver(false, 500);
            check(() -> activity.finish());
            activity = open("SettingsActivity");
            choose("Screensaver mode", "Clock");
            check(() -> activity.finish());
            activity = open("MusicActivity");
            sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_HOME);
            SystemClock.sleep(31000);
            awaitSaver(false, 500);
            runOnMainSync(() -> getTargetContext().startActivity(new Intent()
                    .setClassName("com.mprlab.portal", "com.mprlab.portal.MusicActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));
            long resumeDeadline = SystemClock.uptimeMillis() + 4000;
            boolean[] focused = new boolean[1];
            do {
                check(() -> focused[0] = activity.hasWindowFocus());
                if (focused[0]) break;
                SystemClock.sleep(100);
            } while (SystemClock.uptimeMillis() < resumeDeadline);
            if (!focused[0]) throw new AssertionError("Music did not resume");
            awaitSaver(false, 500);
            awaitSaver(true, 32000);
            sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
            awaitSaver(false, 1000);
            check(() -> {
                if (activity.isFinishing()) throw new AssertionError("Wake key reached Back");
                activity.finish();
            });
            result.putString("stream", "Screensaver passed: settings, persistence, previews, timeout, input reset, wake, disabled, and lifecycle.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "Screensaver failed: " + error + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private Activity open(String name) {
        Activity opened = startActivitySync(new Intent().setClassName("com.mprlab.portal", "com.mprlab.portal." + name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();
        return opened;
    }

    private void typingScenario() {
        android.view.inputmethod.InputConnection[] connection = new android.view.inputmethod.InputConnection[1];
        check(() -> {
            View editor = findEditor(activity.getWindow().getDecorView());
            editor.requestFocus();
            connection[0] = editor.onCreateInputConnection(new android.view.inputmethod.EditorInfo());
            ((android.view.inputmethod.InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showSoftInput(editor, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
        awaitKeyboard(true);
        for (int i = 0; i < 7; i++) {
            SystemClock.sleep(4500);
            check(() -> {
                if (!connection[0].commitText("a", 1)) throw new AssertionError("IME text was rejected");
            });
            awaitSaver(false, 200);
        }
        awaitSaver(true, 32000);
        tap(50, 50);
        awaitSaver(false, 1000);
        check(() -> {
            equal(((android.widget.EditText) findEditor(activity.getWindow().getDecorView())).getText().toString(), "aaaaaaa");
        });
    }

    private void dialogScenario() throws Exception {
        check(() -> require("Screensaver mode").performClick());
        android.view.accessibility.AccessibilityNodeInfo option = awaitText("Disabled");
        android.graphics.Rect optionBounds = new android.graphics.Rect();
        option.getBoundsInScreen(optionBounds);
        SystemClock.sleep(17000);
        android.graphics.Rect titleBounds = new android.graphics.Rect();
        awaitText("Screensaver mode").getBoundsInScreen(titleBounds);
        tap(titleBounds.centerX(), titleBounds.centerY());
        SystemClock.sleep(17000);
        awaitSaver(false, 300);
        awaitSaver(true, 16000);
        assertMinimumBrightness();
        // Wake above an option: the option and dialog must remain unchanged.
        tap(optionBounds.centerX(), optionBounds.centerY());
        awaitSaver(false, 1000);
        awaitText("Disabled");
        check(() -> equal(selected("Screensaver mode"), "Black screen"));
        awaitText("Clock").performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
        SystemClock.sleep(300);
        check(() -> equal(selected("Screensaver mode"), "Clock"));

        awaitText("＋  Add a child").performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
        android.view.accessibility.AccessibilityNodeInfo editor = awaitText("Child's name");
        Bundle text = new Bundle();
        text.putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "Unsaved child");
        if (!editor.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, text))
            throw new AssertionError("Dialog draft input was rejected");
        awaitSaver(true, 32000);
        tap(50, 50);
        awaitSaver(false, 1000);
        awaitText("Unsaved child");
        sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_HOME);
        awaitSaver(false, 1000);
    }

    private android.view.accessibility.AccessibilityNodeInfo awaitText(String text) {
        long deadline = SystemClock.uptimeMillis() + 3000;
        do {
            android.view.accessibility.AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
            if (root != null) {
                android.view.accessibility.AccessibilityNodeInfo found = findTextNode(root, text);
                if (found != null) return found;
            }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Missing visible text: " + text);
    }

    private android.view.accessibility.AccessibilityNodeInfo findTextNode(android.view.accessibility.AccessibilityNodeInfo node, String text) {
        if (node == null) return null;
        boolean matches = text.contentEquals(node.getText() == null ? "" : node.getText())
                || text.contentEquals(node.getHintText() == null ? "" : node.getHintText());
        if (matches && node.isVisibleToUser()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            android.view.accessibility.AccessibilityNodeInfo found = findTextNode(node.getChild(i), text);
            if (found != null) return found;
        }
        return null;
    }

    private android.view.accessibility.AccessibilityNodeInfo saverNode(String label) {
        for (android.view.accessibility.AccessibilityWindowInfo window : getUiAutomation().getWindows()) {
            android.view.accessibility.AccessibilityNodeInfo found = findNode(window.getRoot(), label);
            if (found != null) return found;
        }
        return null;
    }

    private android.view.accessibility.AccessibilityNodeInfo findNode(android.view.accessibility.AccessibilityNodeInfo node, String label) {
        if (node == null) return null;
        if (label.contentEquals(node.getContentDescription() == null ? "" : node.getContentDescription()) && node.isVisibleToUser()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            android.view.accessibility.AccessibilityNodeInfo found = findNode(node.getChild(i), label);
            if (found != null) return found;
        }
        return null;
    }

    private void assertMinimumBrightness() throws Exception {
        try (java.io.InputStream input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(
                getUiAutomation().executeShellCommand("dumpsys window windows"))) {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            for (String window : output.toString("UTF-8").split("Window #")) {
                if (window.contains("FamilyHome screensaver") && window.contains("sbrt=0.0")) return;
            }
            throw new AssertionError("Black screensaver window did not request minimum brightness");
        }
    }

    private void check(Runnable action) {
        Throwable[] failure = new Throwable[1];
        runOnMainSync(() -> { try { action.run(); } catch (Throwable error) { failure[0] = error; } });
        if (failure[0] != null) throw new AssertionError(failure[0]);
        waitForIdleSync();
    }

    private View find(View view, String label) {
        if (label.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = find(group.getChildAt(i), label);
                if (found != null) return found;
            }
        }
        return null;
    }

    private View require(String label) {
        View view = find(activity.getWindow().getDecorView(), label);
        if (view == null) throw new AssertionError("Missing control: " + label);
        return view;
    }

    private View findEditor(View view) {
        if (view instanceof android.widget.EditText) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findEditor(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private void awaitKeyboard(boolean expected) {
        long deadline = SystemClock.uptimeMillis() + 4000;
        do {
            boolean visible = false;
            for (android.view.accessibility.AccessibilityWindowInfo window : getUiAutomation().getWindows()) {
                if (window.getType() == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD) visible = true;
            }
            if (visible == expected) return;
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Keyboard visible, expected=" + expected);
    }

    private String selected(String label) { return ((Spinner) require(label)).getSelectedItem().toString(); }

    private void choose(String label, String value) {
        check(() -> {
            Spinner spinner = (Spinner) require(label);
            for (int i = 0; i < spinner.getCount(); i++) {
                if (value.equals(spinner.getItemAtPosition(i).toString())) { spinner.setSelection(i); return; }
            }
            throw new AssertionError("Missing option: " + value);
        });
    }

    private void equal(String actual, String expected) {
        if (!actual.equals(expected)) throw new AssertionError(actual + " != " + expected);
    }

    private void awaitSaver(boolean expected, long timeout) {
        long deadline = SystemClock.uptimeMillis() + timeout;
        boolean[] visible = new boolean[1];
        do {
            visible[0] = saverNode("Black screensaver. Tap to return") != null
                    || saverNode("Clock screensaver. Tap to return") != null;
            if (visible[0] == expected) return;
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Screensaver visible=" + visible[0] + ", expected=" + expected);
    }

    private void tap(float x, float y) {
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, x, y, 0);
        sendPointerSync(down); sendPointerSync(up);
        down.recycle(); up.recycle(); waitForIdleSync();
    }

    private void screenshot(String name) throws Exception {
        getUiAutomation().waitForIdle(300, 3000);
        try (java.io.FileOutputStream output = new java.io.FileOutputStream(
                new java.io.File(getTargetContext().getFilesDir(), "screensaver-" + name + ".png"))) {
            getUiAutomation().takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, output);
        }
    }
}
