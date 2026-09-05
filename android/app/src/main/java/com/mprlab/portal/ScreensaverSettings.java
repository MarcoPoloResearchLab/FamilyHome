package com.mprlab.portal;

import android.content.Context;
import android.content.SharedPreferences;

final class ScreensaverSettings {
    static final String MODE_LABEL = "Screensaver mode";
    static final String TIMEOUT_LABEL = "Screensaver timeout";
    static final String PREVIEW_LABEL = "Preview screensaver";
    private static final String PREFERENCES = "screensaver";
    private static final String MODE_KEY = "mode";
    private static final String TIMEOUT_KEY = "timeout";

    enum Mode {
        DISABLED("Disabled"), CLOCK("Clock"), BLACK("Black screen");
        private final String label;
        Mode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    enum Timeout {
        THIRTY_SECONDS("30 seconds", 30), ONE_MINUTE("1 minute", 60), TWO_MINUTES("2 minutes", 120),
        FIVE_MINUTES("5 minutes", 300), TEN_MINUTES("10 minutes", 600),
        FIFTEEN_MINUTES("15 minutes", 900), THIRTY_MINUTES("30 minutes", 1800);
        private final String label;
        final long milliseconds;
        Timeout(String label, int seconds) { this.label = label; this.milliseconds = seconds * 1000L; }
        @Override public String toString() { return label; }
    }

    final Mode mode;
    final Timeout timeout;

    ScreensaverSettings(Mode mode, Timeout timeout) { this.mode = mode; this.timeout = timeout; }

    static ScreensaverSettings read(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        try {
            return new ScreensaverSettings(Mode.valueOf(preferences.getString(MODE_KEY, Mode.BLACK.name())),
                    Timeout.valueOf(preferences.getString(TIMEOUT_KEY, Timeout.FIVE_MINUTES.name())));
        } catch (IllegalArgumentException | ClassCastException error) {
            throw new IllegalStateException("Read screensaver settings: invalid saved selection", error);
        }
    }

    boolean save(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
                .putString(MODE_KEY, mode.name()).putString(TIMEOUT_KEY, timeout.name()).commit();
    }
}
