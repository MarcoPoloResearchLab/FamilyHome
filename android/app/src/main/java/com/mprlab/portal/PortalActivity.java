package com.mprlab.portal;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public abstract class PortalActivity extends Activity {
    private static final int IMMERSIVE_FLAGS = View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    private final Handler idleHandler = new Handler(Looper.getMainLooper());
    private final Runnable enterScreensaver = this::showScreensaver;
    private final List<Window> dialogWindows = new ArrayList<>();
    private ScreensaverSettings screensaverSettings;
    private Dialog screensaver;
    private boolean resumed;
    private boolean touching;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        reloadScreensaverSettings();
    }

    @Override protected void onPause() {
        resumed = false;
        idleHandler.removeCallbacks(enterScreensaver);
        hideScreensaver();
        touching = false;
        super.onPause();
    }

    protected final EditText textInput() {
        EditText input = new EditText(this);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable text) {
                if (input.hasFocus()) resetScreensaverTimeout();
            }
        });
        return input;
    }

    protected final void showPortalDialog(Dialog dialog) {
        Window window = dialog.getWindow();
        dialogWindows.add(window);
        window.setCallback(new PortalWindowCallback(this, window));
        dialog.show();
        resetScreensaverTimeout();
    }

    final void portalWindowDetached(Window window) {
        dialogWindows.remove(window);
        touching = false;
        resetScreensaverTimeout();
    }

    final void portalWindowFocusChanged(Window window, boolean focused) {
        if (focused) window.getDecorView().setSystemUiVisibility(IMMERSIVE_FLAGS);
        resetScreensaverTimeout();
    }

    private Window focusedPortalWindow() {
        for (int i = dialogWindows.size() - 1; i >= 0; i--) {
            Window window = dialogWindows.get(i);
            if (window.getDecorView().hasWindowFocus()) return window;
        }
        return hasWindowFocus() ? getWindow() : null;
    }

    final void reloadScreensaverSettings() {
        screensaverSettings = ScreensaverSettings.read(this);
        resetScreensaverTimeout();
    }

    private void resetScreensaverTimeout() {
        idleHandler.removeCallbacks(enterScreensaver);
        if (resumed && focusedPortalWindow() != null && !touching && screensaver == null
                && screensaverSettings.mode != ScreensaverSettings.Mode.DISABLED) {
            idleHandler.postDelayed(enterScreensaver, screensaverSettings.timeout.milliseconds);
        }
    }

    final void showScreensaver() {
        Window owner = focusedPortalWindow();
        if (!resumed || owner == null || touching || screensaver != null
                || screensaverSettings.mode == ScreensaverSettings.Mode.DISABLED) return;
        idleHandler.removeCallbacks(enterScreensaver);
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(owner.getDecorView().getWindowToken(), 0);
        screensaver = new Dialog(this, android.R.style.Theme_Material_NoActionBar);
        screensaver.setCancelable(false);
        screensaver.setContentView(new ScreensaverView(this, screensaverSettings.mode, this::wakeScreensaver));
        Window window = screensaver.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.setTitle("FamilyHome screensaver");
        attributes.screenBrightness = screensaverSettings.mode == ScreensaverSettings.Mode.BLACK ? 0f : .1f;
        window.setAttributes(attributes);
        window.getDecorView().setSystemUiVisibility(IMMERSIVE_FLAGS);
        window.setCallback(new PortalWindowCallback(this, window));
        screensaver.show();
    }

    private void hideScreensaver() {
        if (screensaver == null) return;
        Dialog previous = screensaver;
        screensaver = null;
        previous.dismiss();
    }

    private void wakeScreensaver() {
        hideScreensaver();
        resetScreensaverTimeout();
    }

    final boolean portalTouchEvent(MotionEvent event, BooleanSupplier dispatch) {
        int action = event.getActionMasked();
        if (screensaver != null) {
            // Keep the wake gesture in this window until every pointer is released.
            if (action == MotionEvent.ACTION_UP) wakeScreensaver();
            return true;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            touching = true;
            idleHandler.removeCallbacks(enterScreensaver);
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            touching = false;
            resetScreensaverTimeout();
        }
        return dispatch.getAsBoolean();
    }

    final boolean portalKeyEvent(KeyEvent event, BooleanSupplier dispatch) {
        if (screensaver != null) {
            if (event.getAction() == KeyEvent.ACTION_UP) wakeScreensaver();
            return true;
        }
        resetScreensaverTimeout();
        return dispatch.getAsBoolean();
    }

    final boolean portalMotionEvent(BooleanSupplier dispatch) {
        if (screensaver != null) { wakeScreensaver(); return true; }
        resetScreensaverTimeout();
        return dispatch.getAsBoolean();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        return portalTouchEvent(event, () -> super.dispatchTouchEvent(event));
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        return portalKeyEvent(event, () -> super.dispatchKeyEvent(event));
    }

    @Override public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return portalMotionEvent(() -> super.dispatchGenericMotionEvent(event));
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        portalWindowFocusChanged(getWindow(), focused);
    }

    protected void beforeHome() { }

    final void openHome() {
        beforeHome();
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }
}
