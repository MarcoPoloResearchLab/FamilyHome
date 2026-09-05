package com.mprlab.portal;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

public abstract class PortalActivity extends Activity {
    private final Handler idleHandler = new Handler(Looper.getMainLooper());
    private final Runnable enterScreensaver = this::showScreensaver;
    private ScreensaverSettings screensaverSettings;
    private ScreensaverView screensaver;
    private boolean resumed;
    private boolean touching;
    private boolean wakingTouch;
    private int wakingKey = KeyEvent.KEYCODE_UNKNOWN;
    private float previousBrightness;
    private View content;
    private int previousAccessibility;

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
        wakingTouch = false;
        wakingKey = KeyEvent.KEYCODE_UNKNOWN;
        super.onPause();
    }

    final void reloadScreensaverSettings() {
        screensaverSettings = ScreensaverSettings.read(this);
        resetScreensaverTimeout();
    }

    private void resetScreensaverTimeout() {
        idleHandler.removeCallbacks(enterScreensaver);
        if (resumed && hasWindowFocus() && !touching && screensaver == null
                && screensaverSettings.mode != ScreensaverSettings.Mode.DISABLED) {
            idleHandler.postDelayed(enterScreensaver, screensaverSettings.timeout.milliseconds);
        }
    }

    final void showScreensaver() {
        if (!resumed || !hasWindowFocus() || touching || screensaver != null
                || screensaverSettings.mode == ScreensaverSettings.Mode.DISABLED) return;
        idleHandler.removeCallbacks(enterScreensaver);
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        content = findViewById(android.R.id.content);
        previousAccessibility = content.getImportantForAccessibility();
        content.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        previousBrightness = attributes.screenBrightness;
        attributes.screenBrightness = screensaverSettings.mode == ScreensaverSettings.Mode.BLACK ? 0f : .1f;
        getWindow().setAttributes(attributes);
        screensaver = new ScreensaverView(this, screensaverSettings.mode, this::wakeScreensaver);
        ((ViewGroup) getWindow().getDecorView()).addView(screensaver, new ViewGroup.LayoutParams(-1, -1));
        screensaver.requestFocus();
    }

    private void hideScreensaver() {
        if (screensaver == null) return;
        ((ViewGroup) screensaver.getParent()).removeView(screensaver);
        screensaver = null;
        content.setImportantForAccessibility(previousAccessibility);
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.screenBrightness = previousBrightness;
        getWindow().setAttributes(attributes);
    }

    private void wakeScreensaver() {
        hideScreensaver();
        resetScreensaverTimeout();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            touching = true;
            idleHandler.removeCallbacks(enterScreensaver);
            wakingTouch = screensaver != null;
            if (wakingTouch) hideScreensaver();
        }
        boolean consume = wakingTouch;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            touching = false;
            wakingTouch = false;
            resetScreensaverTimeout();
        }
        return consume || super.dispatchTouchEvent(event);
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (screensaver != null) {
            wakingKey = event.getKeyCode();
            wakeScreensaver();
        }
        boolean consume = wakingKey == event.getKeyCode();
        resetScreensaverTimeout();
        if (event.getAction() == KeyEvent.ACTION_UP) wakingKey = KeyEvent.KEYCODE_UNKNOWN;
        return consume || super.dispatchKeyEvent(event);
    }

    @Override public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (screensaver != null) { wakeScreensaver(); return true; }
        resetScreensaverTimeout();
        return super.dispatchGenericMotionEvent(event);
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        if (resumed) resetScreensaverTimeout();
    }

    protected void beforeHome() { }

    final void openHome() {
        beforeHome();
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }
}
