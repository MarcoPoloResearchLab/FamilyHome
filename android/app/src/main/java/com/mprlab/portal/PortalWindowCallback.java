package com.mprlab.portal;

import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.KeyboardShortcutGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

/** Keeps dialog input in the owning activity's inactivity lifecycle. */
final class PortalWindowCallback implements Window.Callback {
    private final PortalActivity activity;
    private final Window window;
    private final Window.Callback delegate;

    PortalWindowCallback(PortalActivity activity, Window window) {
        this.activity = activity;
        this.window = window;
        this.delegate = window.getCallback();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        return activity.portalTouchEvent(event, () -> delegate.dispatchTouchEvent(event));
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        return activity.portalKeyEvent(event, () -> delegate.dispatchKeyEvent(event));
    }
    @Override public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return activity.portalKeyEvent(event, () -> delegate.dispatchKeyShortcutEvent(event));
    }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return activity.portalMotionEvent(() -> delegate.dispatchGenericMotionEvent(event));
    }
    @Override public boolean dispatchTrackballEvent(MotionEvent event) {
        return activity.portalMotionEvent(() -> delegate.dispatchTrackballEvent(event));
    }
    @Override public void onWindowFocusChanged(boolean focused) {
        delegate.onWindowFocusChanged(focused);
        activity.portalWindowFocusChanged(window, focused);
    }
    @Override public void onDetachedFromWindow() {
        delegate.onDetachedFromWindow();
        window.setCallback(delegate);
        activity.portalWindowDetached(window);
    }
    @Override public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return delegate.dispatchPopulateAccessibilityEvent(event);
    }
    @Override public View onCreatePanelView(int feature) { return delegate.onCreatePanelView(feature); }
    @Override public boolean onCreatePanelMenu(int feature, Menu menu) { return delegate.onCreatePanelMenu(feature, menu); }
    @Override public boolean onPreparePanel(int feature, View view, Menu menu) { return delegate.onPreparePanel(feature, view, menu); }
    @Override public boolean onMenuOpened(int feature, Menu menu) { return delegate.onMenuOpened(feature, menu); }
    @Override public boolean onMenuItemSelected(int feature, MenuItem item) { return delegate.onMenuItemSelected(feature, item); }
    @Override public void onWindowAttributesChanged(WindowManager.LayoutParams attributes) { delegate.onWindowAttributesChanged(attributes); }
    @Override public void onContentChanged() { delegate.onContentChanged(); }
    @Override public void onAttachedToWindow() { delegate.onAttachedToWindow(); }
    @Override public void onPanelClosed(int feature, Menu menu) { delegate.onPanelClosed(feature, menu); }
    @Override public boolean onSearchRequested() { return delegate.onSearchRequested(); }
    @Override public boolean onSearchRequested(SearchEvent event) { return delegate.onSearchRequested(event); }
    @Override public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) { return delegate.onWindowStartingActionMode(callback); }
    @Override public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) { return delegate.onWindowStartingActionMode(callback, type); }
    @Override public void onActionModeStarted(ActionMode mode) { delegate.onActionModeStarted(mode); }
    @Override public void onActionModeFinished(ActionMode mode) { delegate.onActionModeFinished(mode); }
    @Override public void onProvideKeyboardShortcuts(List<KeyboardShortcutGroup> groups, Menu menu, int device) {
        delegate.onProvideKeyboardShortcuts(groups, menu, device);
    }
    @Override public void onPointerCaptureChanged(boolean captured) { delegate.onPointerCaptureChanged(captured); }
}
