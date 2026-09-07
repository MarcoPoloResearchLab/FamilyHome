package com.mprlab.portal;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.Choreographer;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import java.util.Locale;

final class ScreensaverView extends FrameLayout {
    private static final float HORIZONTAL_DP_PER_SECOND = 72;
    private static final float VERTICAL_DP_PER_SECOND = 48;
    private final LinearLayout clock;
    private final Axis horizontal;
    private final Axis vertical;
    private long previousFrameNanos;
    private boolean positioned;
    private final Choreographer.FrameCallback moveClock = new Choreographer.FrameCallback() {
        @Override public void doFrame(long frameTimeNanos) {
            if (!ValueAnimator.areAnimatorsEnabled()) return;
            if (previousFrameNanos != 0 && positioned) {
                double seconds = (frameTimeNanos - previousFrameNanos) / 1_000_000_000d;
                horizontal.advance(seconds, Math.max(0, getWidth() - clock.getWidth()));
                vertical.advance(seconds, Math.max(0, getHeight() - clock.getHeight()));
                positionClock();
            }
            previousFrameNanos = frameTimeNanos;
            Choreographer.getInstance().postFrameCallback(this);
        }
    };

    ScreensaverView(Context context, ScreensaverSettings.Mode mode, Runnable wake) {
        super(context);
        float density = getResources().getDisplayMetrics().density;
        horizontal = new Axis(HORIZONTAL_DP_PER_SECOND * density);
        vertical = new Axis(VERTICAL_DP_PER_SECOND * density);
        setBackgroundColor(Color.BLACK);
        setContentDescription(mode == ScreensaverSettings.Mode.BLACK
                ? "Black screensaver. Tap to return" : "Clock screensaver. Tap to return");
        setFocusable(true);
        setOnClickListener(view -> wake.run());
        if (mode == ScreensaverSettings.Mode.CLOCK) {
            clock = new LinearLayout(context);
            clock.setOrientation(LinearLayout.VERTICAL);
            clock.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams faceLayout = new LinearLayout.LayoutParams(-2, -2);
            faceLayout.bottomMargin = Math.round(12 * density);
            clock.addView(new ScreensaverClockView(context), faceLayout);
            TextClock time = new TextClock(context);
            String timePattern = TimeFormatSettings.read(context).pattern;
            time.setFormat12Hour(timePattern);
            time.setFormat24Hour(timePattern);
            PortalStyle.text(time, PortalStyle.TextRole.COUNTDOWN);
            time.setTextColor(Color.rgb(190, 198, 210));
            time.setGravity(Gravity.CENTER);
            clock.addView(time, new LinearLayout.LayoutParams(-2, -2));
            TextClock date = new TextClock(context);
            String datePattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEE MMMM d yyyy");
            date.setFormat12Hour(datePattern);
            date.setFormat24Hour(datePattern);
            PortalStyle.text(date, PortalStyle.TextRole.PRIMARY);
            date.setTextColor(Color.rgb(190, 198, 210));
            date.setGravity(Gravity.CENTER);
            clock.addView(date, new LinearLayout.LayoutParams(-2, -2));
            TextView help = new TextView(context);
            help.setText("Tap anywhere to return");
            PortalStyle.text(help, PortalStyle.TextRole.BODY);
            help.setTextColor(Color.GRAY);
            help.setGravity(Gravity.CENTER);
            clock.addView(help, new LinearLayout.LayoutParams(-2, -2));
            addView(clock, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.LEFT));
        } else {
            clock = null;
        }
    }

    private void positionClock() {
        clock.setTranslationX((float) horizontal.position);
        clock.setTranslationY((float) vertical.position);
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (clock == null) return;
        int width = Math.max(0, getWidth() - clock.getWidth());
        int height = Math.max(0, getHeight() - clock.getHeight());
        horizontal.position = positioned ? Math.min(horizontal.position, width) : width / 2d;
        vertical.position = positioned ? Math.min(vertical.position, height) : height / 2d;
        positioned = true;
        positionClock();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        previousFrameNanos = 0;
        if (clock != null && ValueAnimator.areAnimatorsEnabled())
            Choreographer.getInstance().postFrameCallback(moveClock);
    }

    @Override protected void onDetachedFromWindow() {
        Choreographer.getInstance().removeFrameCallback(moveClock);
        previousFrameNanos = 0;
        super.onDetachedFromWindow();
    }

    private static final class Axis {
        double position;
        private double velocity;

        Axis(double velocity) { this.velocity = velocity; }

        void advance(double seconds, int limit) {
            if (limit == 0) { position = 0; return; }
            double period = 2d * limit;
            double phase = ((position + velocity * seconds) % period + period) % period;
            position = phase <= limit ? phase : period - phase;
            if (phase > limit) velocity = -velocity;
        }
    }
}
