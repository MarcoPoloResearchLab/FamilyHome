package com.mprlab.portal.toolbartest;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

public final class ToolbarTest extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            getTargetContext().getSharedPreferences("children_portal", Context.MODE_PRIVATE).edit()
                .putString("profiles_json", "[{\"id\":\"toolbar-test\",\"name\":\"Toolbar Test\"}]")
                .putString("active_profile_id", "toolbar-test").commit();
            for (String name : new String[]{"GameLibraryActivity", "MusicActivity", "GuitarActivity", "PianoActivity", "DrawingActivity", "AskActivity", "SettingsActivity"}) {
                scenario(name);
            }
            result.putString("stream", "Toolbar passed: seven screens, one row, touch targets, Back, and Home.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "Toolbar failed: " + error + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private Activity open(String name) {
        return startActivitySync(new Intent().setClassName("com.mprlab.portal", "com.mprlab.portal." + name)
            .putExtra("profile_id", "toolbar-test").putExtra("profile_name", "Toolbar Test")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private Activity openFrom(Activity parent, String name) {
        ActivityMonitor monitor = addMonitor("com.mprlab.portal." + name, null, false);
        runOnMainSync(() -> parent.startActivity(new Intent().setClassName("com.mprlab.portal", "com.mprlab.portal." + name)
                .putExtra("profile_id", "toolbar-test").putExtra("profile_name", "Toolbar Test")));
        Activity activity = waitForMonitorWithTimeout(monitor, 4000);
        removeMonitor(monitor);
        if (activity == null) throw new AssertionError(name + " did not open");
        return activity;
    }

    private void scenario(String name) throws Exception {
        Activity root = open("MainActivity");
        Activity parent = name.equals("PianoActivity") || name.equals("GuitarActivity")
            ? openFrom(root, "MusicActivity") : root;
        Activity child = openFrom(parent, name);
        waitForIdleSync();
        getUiAutomation().waitForIdle(500, 5000);
        try (java.io.FileOutputStream output = new java.io.FileOutputStream(
                new java.io.File(getTargetContext().getFilesDir(), "toolbar-" + name + ".png"))) {
            getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output);
        }
        ActivityMonitor backMonitor = addMonitor(parent.getClass().getName(), null, false);
        Throwable[] failure = new Throwable[1];
        runOnMainSync(() -> {
            try {
                View decor = child.getWindow().getDecorView();
                View toolbar = find(decor, "Screen toolbar");
                if (toolbar == null) throw new AssertionError(name + " has no combined toolbar");
                View back = find(toolbar, "Back");
                View home = find(toolbar, "Home");
                if (back == null || home == null) throw new AssertionError(name + " navigation missing");
                float density = child.getResources().getDisplayMetrics().density;
                Rect barRect = new Rect(), backRect = new Rect(), homeRect = new Rect();
                toolbar.getGlobalVisibleRect(barRect); back.getGlobalVisibleRect(backRect); home.getGlobalVisibleRect(homeRect);
                if (barRect.top != 0 || barRect.height() / density > 72) throw new AssertionError(name + " wastes top space: " + barRect);
                if (backRect.top != homeRect.top || backRect.bottom != homeRect.bottom) throw new AssertionError(name + " uses two rows");
                for (Rect rect : new Rect[]{backRect, homeRect}) {
                    if (rect.width() / density < 48 || rect.height() / density < 48) throw new AssertionError(name + " small target");
                }
                checkBounds(toolbar, barRect);
                if (name.equals("GuitarActivity")) {
                    for (String label : new String[]{"C", "G", "Am", "F", "Open strings"}) {
                        View chord = find(toolbar, label);
                        if (chord == null) throw new AssertionError("Guitar control outside the toolbar: " + label);
                    }
                }
                back.performClick();
            } catch (Throwable error) { failure[0] = error; }
        });
        waitForIdleSync();
        if (failure[0] != null) throw new AssertionError(failure[0]);
        Activity backHome = waitForMonitorWithTimeout(backMonitor, 4000);
        removeMonitor(backMonitor);
        if (backHome == null) throw new AssertionError(name + " Back did not return to its parent");
        Activity reopened = openFrom(parent, name);
        waitForIdleSync();
        ActivityMonitor homeMonitor = addMonitor("com.mprlab.portal.MainActivity", null, false);
        runOnMainSync(() -> find(reopened.getWindow().getDecorView(), "Home").performClick());
        Activity returnedHome = waitForMonitorWithTimeout(homeMonitor, 4000);
        removeMonitor(homeMonitor);
        if (returnedHome == null) throw new AssertionError(name + " Home did not open MainActivity");
        runOnMainSync(() -> returnedHome.finish());
        runOnMainSync(() -> parent.finish());
        if (parent != root) runOnMainSync(() -> root.finish());
        waitForIdleSync();
    }

    private View find(View view, String description) {
        if (description.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = find(group.getChildAt(i), description);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void checkBounds(View view, Rect toolbar) {
        if (view.getVisibility() != View.VISIBLE) return;
        Rect actual = new Rect();
        if (view.isClickable() && (!view.getGlobalVisibleRect(actual) || !toolbar.contains(actual)
                || actual.width() < view.getWidth() || actual.height() < view.getHeight())) {
            throw new AssertionError("Clipped toolbar control: " + view.getContentDescription());
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) checkBounds(group.getChildAt(i), toolbar);
        }
    }
}
