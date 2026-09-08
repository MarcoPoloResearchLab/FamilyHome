package com.mprlab.portal;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class TimeFormatSettings {
    static final String LABEL = "Time format";
    private static final String PREFERENCES = "time_format";
    private static final String FORMAT_KEY = "format";

    enum Format {
        TWELVE_HOUR("12-hour", "h:mm a"), TWENTY_FOUR_HOUR("24-hour", "HH:mm");
        private final String label;
        final String pattern;

        Format(String label, String pattern) { this.label = label; this.pattern = pattern; }

        String format(Date date) { return new SimpleDateFormat(pattern, Locale.getDefault()).format(date); }

        @Override public String toString() { return label; }
    }

    static Format read(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        try {
            String saved = preferences.getString(FORMAT_KEY, null);
            if (saved != null) return Format.valueOf(saved);
            Format initial = DateFormat.is24HourFormat(context) ? Format.TWENTY_FOUR_HOUR : Format.TWELVE_HOUR;
            if (!save(context, initial)) throw new IllegalStateException("Save initial FamilyHome time format failed");
            return initial;
        } catch (IllegalArgumentException | ClassCastException error) {
            throw new IllegalStateException("Read FamilyHome time format: invalid saved selection", error);
        }
    }

    static boolean save(Context context, Format format) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
                .putString(FORMAT_KEY, format.name()).commit();
    }

    private TimeFormatSettings() { }
}
