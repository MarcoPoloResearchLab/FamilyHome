package com.mprlab.portal;

import android.content.Context;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

final class ScreensaverView extends FrameLayout {
    private static final long MOVE_INTERVAL_MS = 60000;
    private final LinearLayout clock;
    private final Runnable moveClock = new Runnable() {
        @Override public void run() {
            positionClock();
            postDelayed(this, MOVE_INTERVAL_MS);
        }
    };

    ScreensaverView(Context context, ScreensaverSettings.Mode mode, Runnable wake) {
        super(context);
        setBackgroundColor(Color.BLACK);
        setContentDescription(mode == ScreensaverSettings.Mode.BLACK
                ? "Black screensaver. Tap to return" : "Clock screensaver. Tap to return");
        setFocusable(true);
        setOnClickListener(view -> wake.run());
        if (mode == ScreensaverSettings.Mode.CLOCK) {
            clock = new LinearLayout(context);
            clock.setOrientation(LinearLayout.VERTICAL);
            clock.setGravity(Gravity.CENTER);
            TextClock time = new TextClock(context);
            time.setFormat12Hour("h:mm a");
            time.setFormat24Hour("HH:mm");
            time.setTextSize(64);
            time.setTextColor(Color.rgb(190, 198, 210));
            clock.addView(time);
            TextView help = new TextView(context);
            help.setText("Tap anywhere to return");
            help.setTextSize(16);
            help.setTextColor(Color.GRAY);
            clock.addView(help);
            addView(clock, new FrameLayout.LayoutParams(-2, -2));
        } else {
            clock = null;
        }
    }

    private void positionClock() {
        int position = (int) ((SystemClock.uptimeMillis() / MOVE_INTERVAL_MS) % 6);
        float horizontal = (position % 3 + 1) / 4f;
        float vertical = (position / 3 + 1) / 3f;
        clock.setTranslationX(Math.max(0, getWidth() - clock.getWidth()) * horizontal);
        clock.setTranslationY(Math.max(0, getHeight() - clock.getHeight()) * vertical);
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (clock != null) positionClock();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (clock != null) post(moveClock);
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(moveClock);
        super.onDetachedFromWindow();
    }
}
