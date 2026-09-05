package com.mprlab.portal;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends PortalActivity {
    private static final int BG = Color.rgb(255, 248, 234);
    private static final int SYSTEM_BAR = Color.rgb(36, 49, 71);
    private static final int SURFACE = Color.WHITE;
    private static final int INK = Color.rgb(36, 49, 71);
    private static final int MUTED = Color.rgb(92, 104, 124);
    private static final int BLUE = Color.rgb(63, 132, 255);
    private static final int PURPLE = Color.rgb(124, 92, 252);
    private static final int CORAL = Color.rgb(255, 105, 105);
    private static final int TEAL = Color.rgb(0, 166, 153);
    private static final int MUSIC_BLUE = Color.rgb(67, 114, 235);
    private static final int PALE_BLUE = Color.rgb(232, 243, 255);
    private static final int PALE_GREEN = Color.rgb(234, 249, 240);
    private static final int PALE_YELLOW = Color.rgb(255, 244, 204);
    private static final int ICON_DRAW = 1;
    private static final int ICON_ASK = 2;
    private static final int ICON_PLAY = 3;
    private static final int ICON_MUSIC = 4;
    private static final int TIMER_ICON_BOOK = R.drawable.ic_timer_reading;
    private static final int TIMER_ICON_TOOTHBRUSH = R.drawable.ic_timer_teeth;
    private static final int TIMER_ICON_HOURGLASS = R.drawable.ic_timer_quick;
    private static final int TIMER_ICON_CLOCK = R.drawable.ic_timer_custom;
    private static final long READING_MS = 20L * 60L * 1000L;
    private static final long BRUSH_TEETH_MS = (2L * 60L + 15L) * 1000L;
    private static final long QUICK_TIMER_MS = 5L * 60L * 1000L;
    private static final long WEATHER_CACHE_MS = 15L * 60L * 1000L;
    private final Handler handler = new Handler();
    private ProfileStore store;
    private TextView clock, eventTitle, eventTime, timerText, timerStatus;
    private TextView countdownDisplay;
    private Button countdownPause;
    private TextView weatherTemperature, weatherCondition, weatherDetails, weatherPlace, weatherFeelsLike;
    private TextView weatherAdvice, weatherTopLabel, weatherShoesLabel, weatherSource;
    private ImageView weatherTopIcon, weatherShoesIcon;
    private LinearLayout weatherOutfit;
    private WeatherReport weatherReport;
    private LinearLayout weatherCard;
    private WeatherIconView weatherIcon;
    private boolean setupPrompted;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (clock != null) clock.setText(new SimpleDateFormat("h:mm  •  EEEE, MMMM d", Locale.getDefault()).format(new Date()));
            updateTimer();
            handler.postDelayed(this, 500L);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setStatusBarColor(SYSTEM_BAR);
        window.setNavigationBarColor(SYSTEM_BAR);
        store = new ProfileStore(this);
        render();
        if (store.profiles.isEmpty()) handler.postDelayed(() -> {
            if (!setupPrompted) { setupPrompted = true; openSettings(); }
        }, 300L);
    }

    @Override protected void onResume() {
        super.onResume();
        store.load();
        render();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        if (store.active != null && store.active.timerRunning
                && store.active.timerEndEpochMs > System.currentTimeMillis()) {
            scheduleTimerAlarm(store.active);
        }
    }

    @Override protected void onPause() {
        handler.removeCallbacks(ticker);
        store.save();
        super.onPause();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout root = column();
        root.setPadding(dp(34), dp(8), dp(34), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        LinearLayout header = row();
        Button profile = button(store.active == null ? "Set up FamilyHome" : "Hi, " + store.active.name + "!  ▾", PURPLE);
        profile.setOnClickListener(v -> {
            if (store.profiles.isEmpty()) openSettings();
            else showProfiles();
        });
        header.addView(profile, weighted(1f, 62));
        clock = text("", 22, INK, true);
        clock.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        header.addView(clock, weighted(1.35f, 62));
        ImageButton settings = new ImageButton(this);
        settings.setImageResource(R.drawable.ic_settings);
        settings.setContentDescription("Open settings");
        settings.setPadding(dp(15), dp(15), dp(15), dp(15));
        settings.setBackground(rounded(PALE_BLUE, 20));
        settings.setOnClickListener(view -> openSettings());
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(dp(62), dp(62));
        settingsParams.leftMargin = dp(14);
        header.addView(settings, settingsParams);
        root.addView(header, matchWrap());

        LinearLayout cards = row();
        cards.setPadding(dp(8), 0, dp(8), dp(12));
        cards.setClipChildren(false);
        cards.setClipToPadding(false);
        LinearLayout calendarCard = card(PALE_BLUE);
        calendarCard.addView(label("COMING UP"));
        eventTitle = text("What’s next?", 29, INK, true);
        eventTitle.setPadding(0, dp(22), 0, dp(8));
        calendarCard.addView(eventTitle);
        eventTime = text("Add a calendar to see the next adventure.", 18, MUTED, false);
        calendarCard.addView(eventTime);
        Button connect = button("Add or change calendar", BLUE);
        connect.setOnClickListener(v -> openSettings());
        LinearLayout.LayoutParams connectParams = matchWrap();
        connectParams.topMargin = dp(24);
        calendarCard.addView(connect, connectParams);

        LinearLayout timerCard = card(SURFACE);
        timerCard.setPadding(0, 0, 0, 0);
        timerCard.setClipToOutline(true);
        LinearLayout timerSummary = row();
        timerSummary.setPadding(dp(18), 0, dp(18), 0);
        timerSummary.addView(label("TIMER"), new LinearLayout.LayoutParams(0, -2, 1f));
        timerText = text("Choose your time", 15, MUTED, true);
        timerText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        timerText.setClickable(true);
        timerText.setFocusable(true);
        timerText.setOnClickListener(v -> showCountdown());
        timerSummary.addView(timerText, new LinearLayout.LayoutParams(-2, -1));
        timerCard.addView(timerSummary, new LinearLayout.LayoutParams(-1, dp(42)));

        LinearLayout timerGrid = column();
        LinearLayout firstPresetRow = row();
        firstPresetRow.addView(timerPreset("Reading", "20 min", PALE_BLUE, TIMER_ICON_BOOK,
                () -> startTimer(READING_MS, "Reading")), weighted(1f, 129));
        firstPresetRow.addView(timerPreset("Brush teeth", "2 min 15 sec", PALE_GREEN, TIMER_ICON_TOOTHBRUSH,
                () -> startTimer(BRUSH_TEETH_MS, "Brush teeth")), weighted(1f, 129));
        timerGrid.addView(firstPresetRow, new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout secondPresetRow = row();
        secondPresetRow.addView(timerPreset("Quick timer", "5 min", PALE_YELLOW, TIMER_ICON_HOURGLASS,
                () -> startTimer(QUICK_TIMER_MS, "Quick")), weighted(1f, 129));
        secondPresetRow.addView(timerPreset("Custom", "Choose time", Color.rgb(255, 231, 224), TIMER_ICON_CLOCK,
                this::showCustomTimer), weighted(1f, 129));
        timerGrid.addView(secondPresetRow, new LinearLayout.LayoutParams(-1, 0, 1f));
        timerCard.addView(timerGrid, new LinearLayout.LayoutParams(-1, 0, 1f));

        boolean showWeather = WeatherVisibility.isConfigured(store.weatherLocation);
        cards.addView(calendarCard, cardParams(0, showWeather ? 3 : 2));
        cards.addView(timerCard, cardParams(1, showWeather ? 3 : 2));
        if (showWeather) {
            weatherCard = createWeatherCard();
            cards.addView(weatherCard, cardParams(2, 3));
        } else {
            weatherCard = null;
            weatherIcon = null;
            weatherTemperature = null;
            weatherCondition = null;
            weatherDetails = null;
            weatherPlace = null;
        }
        root.addView(cards, matchWrap());

        View activitySpacer = new View(this);
        root.addView(activitySpacer, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout actions = row();
        ActivityTile draw = actionTile("Draw", "Make a picture", CORAL, ICON_DRAW); draw.setOnClickListener(v -> launch(DrawingActivity.class));
        ActivityTile ask = actionTile("Ask", "Learn something", PURPLE, ICON_ASK); ask.setOnClickListener(v -> launch(AskActivity.class));
        ActivityTile music = actionTile("Music", "Choose an instrument", MUSIC_BLUE, ICON_MUSIC); music.setOnClickListener(v -> launch(MusicActivity.class));
        ActivityTile games = actionTile("Games", "Choose and play", TEAL, ICON_PLAY); games.setOnClickListener(v -> launch(GameLibraryActivity.class));
        actions.addView(draw, actionParams());
        actions.addView(ask, actionParams());
        actions.addView(music, actionParams());
        actions.addView(games, actionParams());
        LinearLayout.LayoutParams actionRow = matchWrap(); actionRow.topMargin = dp(20);
        root.addView(actions, actionRow);

        setContentView(scroll);
        updateTimer();
        refreshCalendar();
        refreshWeather();
    }

    private void launch(Class<?> type) {
        if (store.active == null) { openSettings(); return; }
        Intent intent = new Intent(this, type);
        intent.putExtra("profile_id", store.active.id);
        intent.putExtra("profile_name", store.active.name);
        startActivity(intent);
    }

    private void showProfiles() {
        if (store.profiles.isEmpty()) {
            openSettings();
            return;
        }
        String[] names = new String[store.profiles.size()];
        for (int i = 0; i < store.profiles.size(); i++) names[i] = store.profiles.get(i).name;
        new android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert).setTitle("Whose turn is it?")
                .setItems(names, (dialog, which) -> {
                    store.active = store.profiles.get(which);
                    store.save();
                    render();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void toggleTimer() {
        if (store.active == null) { openSettings(); return; }
        long now = System.currentTimeMillis();
        if (store.active.timerRunning) {
            store.active.remainingMs = Math.max(0L, store.active.timerEndEpochMs - now);
            store.active.timerRunning = false;
            cancelTimerAlarm(store.active);
        } else {
            if (store.active.remainingMs <= 0) {
                showCustomTimer();
                return;
            }
            store.active.timerEndEpochMs = now + store.active.remainingMs;
            store.active.timerRunning = true;
            scheduleTimerAlarm(store.active);
        }
        store.save(); updateTimer();
    }

    private void startTimer(long durationMs, String timerName) {
        if (store.active == null) { openSettings(); return; }
        if (durationMs <= 0L) throw new IllegalArgumentException("Timer duration must be positive");
        cancelTimerAlarm(store.active);
        store.active.remainingMs = durationMs;
        store.active.timerEndEpochMs = System.currentTimeMillis() + durationMs;
        store.active.timerRunning = true;
        store.save();
        scheduleTimerAlarm(store.active);
        updateTimer();
        showCountdown();
    }

    private void showCountdown() {
        if (store.active == null || store.active.remainingMs <= 0L) return;
        LinearLayout content = column();
        content.setPadding(dp(32), dp(20), dp(32), dp(24));
        content.setGravity(Gravity.CENTER);
        countdownDisplay = text("", 76, INK, true);
        countdownDisplay.setGravity(Gravity.CENTER);
        content.addView(countdownDisplay, matchWrap());
        timerStatus = text("", 20, MUTED, false);
        timerStatus.setGravity(Gravity.CENTER);
        content.addView(timerStatus, matchWrap());
        countdownPause = button("Pause", TEAL);
        countdownPause.setOnClickListener(view -> toggleTimer());
        LinearLayout.LayoutParams pauseParams = new LinearLayout.LayoutParams(-1, dp(58));
        pauseParams.topMargin = dp(24);
        content.addView(countdownPause, pauseParams);
        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
                .setTitle("Your timer")
                .setView(content)
                .setPositiveButton("Keep playing", null)
                .setNegativeButton("Finish timer", (ignored, which) -> {
                    cancelTimerAlarm(store.active);
                    store.active.timerRunning = false;
                    store.active.remainingMs = 0L;
                    store.save();
                    updateTimer();
                }).create();
        dialog.setOnDismissListener(ignored -> {
            countdownDisplay = null;
            countdownPause = null;
            timerStatus = null;
        });
        updateTimer();
        dialog.show();
    }

    private void showCustomTimer() {
        if (store.active == null) { openSettings(); return; }
        LinearLayout duration = column();
        duration.setPadding(dp(24), dp(8), dp(24), dp(8));
        long[] customSeconds = {5L * 60L};
        Button[] startButton = {null};
        TextView customValue = text("05:00", 48, INK, true);
        customValue.setGravity(Gravity.CENTER);
        customValue.setContentDescription("Custom timer duration, 5 minutes");
        duration.addView(customValue, matchWrap());

        TextView customHelp = text("Use the buttons to change the timer.", 16, MUTED, false);
        customHelp.setGravity(Gravity.CENTER);
        customHelp.setPadding(0, 0, 0, dp(12));
        duration.addView(customHelp, matchWrap());

        Runnable updateDuration = () -> {
            customValue.setText(String.format(Locale.US, "%02d:%02d", customSeconds[0] / 60L, customSeconds[0] % 60L));
            customValue.setContentDescription("Custom timer duration, " + customSeconds[0] + " seconds");
            if (startButton[0] != null) startButton[0].setEnabled(customSeconds[0] > 0L);
        };
        LinearLayout minuteControls = row();
        Button removeMinute = customTimerButton("− 1 minute", PALE_BLUE, INK);
        removeMinute.setOnClickListener(view -> {
            customSeconds[0] = Math.max(0L, customSeconds[0] - 60L);
            updateDuration.run();
        });
        Button addMinute = customTimerButton("+ 1 minute", BLUE, Color.WHITE);
        addMinute.setOnClickListener(view -> {
            customSeconds[0] = Math.min(60L * 60L, customSeconds[0] + 60L);
            updateDuration.run();
        });
        minuteControls.addView(removeMinute, spacedWeighted(1f, 52, false));
        minuteControls.addView(addMinute, spacedWeighted(1f, 52, true));
        duration.addView(minuteControls, matchWrap());

        LinearLayout secondControls = row();
        Button removeSeconds = customTimerButton("− 15 seconds", PALE_YELLOW, INK);
        removeSeconds.setOnClickListener(view -> {
            customSeconds[0] = Math.max(0L, customSeconds[0] - 15L);
            updateDuration.run();
        });
        Button addSeconds = customTimerButton("+ 15 seconds", CORAL, Color.WHITE);
        addSeconds.setOnClickListener(view -> {
            customSeconds[0] = Math.min(60L * 60L, customSeconds[0] + 15L);
            updateDuration.run();
        });
        secondControls.addView(removeSeconds, spacedWeighted(1f, 52, false));
        secondControls.addView(addSeconds, spacedWeighted(1f, 52, true));
        LinearLayout.LayoutParams secondControlsParams = matchWrap();
        secondControlsParams.topMargin = dp(8);
        duration.addView(secondControls, secondControlsParams);

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
                .setTitle("Custom timer")
                .setView(duration)
                .setPositiveButton("Start", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            startButton[0] = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            startButton[0].setOnClickListener(view -> {
                long durationMs = customSeconds[0] * 1000L;
                if (durationMs <= 0L) return;
                dialog.dismiss();
                startTimer(durationMs, "Custom");
            });
            updateDuration.run();
        });
        dialog.show();
    }

    private void scheduleTimerAlarm(ProfileStore.Profile profile) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, profile.timerEndEpochMs, timerAlarmIntent(profile));
    }

    private void cancelTimerAlarm(ProfileStore.Profile profile) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(timerAlarmIntent(profile));
    }

    private PendingIntent timerAlarmIntent(ProfileStore.Profile profile) {
        Intent intent = new Intent(this, TimerAlarmReceiver.class);
        intent.putExtra("profile_name", profile.name);
        return PendingIntent.getBroadcast(this, profile.id.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void updateTimer() {
        if (timerText == null) return;
        long remaining = READING_MS; boolean running = false;
        if (store.active != null) {
            running = store.active.timerRunning;
            remaining = running ? Math.max(0, store.active.timerEndEpochMs - System.currentTimeMillis()) : store.active.remainingMs;
            if (running && remaining == 0) {
                store.active.timerRunning = false; store.active.remainingMs = 0; store.save(); running = false;
                Toast.makeText(this, "Time is up!", Toast.LENGTH_LONG).show();
            }
        }
        long seconds = (remaining + 999) / 1000;
        String countdown = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
        timerText.setText(running ? "◷ " + countdown : remaining > 0 ? "Paused · " + countdown : "Choose your time");
        timerText.setContentDescription("Open timer " + countdown);
        if (countdownDisplay != null) {
            countdownDisplay.setText(remaining == 0 ? "All done!" : countdown);
            timerStatus.setText(running ? "You have time. Enjoy it!" : remaining > 0 ? "Take a little break" : "Nice job — your time is up.");
            countdownPause.setText(running ? "Pause" : "Resume");
            countdownPause.setEnabled(remaining > 0L);
            countdownPause.setVisibility(remaining > 0L ? View.VISIBLE : View.GONE);
        }
    }

    private void refreshCalendar() {
        if (eventTitle == null || store.active == null || store.active.calendarUrl == null || store.active.calendarUrl.trim().isEmpty()) return;
        ProfileStore.Profile profile = store.active;
        eventTitle.setText("Loading calendar…"); eventTime.setText("Connecting through the Portal service");
        new Thread(() -> {
            try {
                String endpoint = PortalConfig.serviceURL("/v1/calendar/next?url=" + URLEncoder.encode(profile.calendarUrl, "UTF-8"));
                HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setConnectTimeout(5000); connection.setReadTimeout(12000);
                PortalConfig.authorize(connection);
                JSONObject event = new JSONObject(read(connection)).optJSONObject("event");
                runOnUiThread(() -> {
                    if (store.active == null || !store.active.id.equals(profile.id)) return;
                    if (event == null) { eventTitle.setText("No upcoming events"); eventTime.setText("This calendar is connected."); }
                    else { eventTitle.setText(event.optString("title", "Upcoming event")); eventTime.setText(formatTime(event.optString("start"))); }
                });
            } catch (Exception error) {
                runOnUiThread(() -> { eventTitle.setText("Calendar unavailable"); eventTime.setText("Please try again soon."); });
            }
        }).start();
    }

    private LinearLayout createWeatherCard() {
        LinearLayout weather = card(PALE_YELLOW);
        weatherCard = weather;
        weatherReport = null;
        weather.setPadding(dp(20), dp(16), dp(20), dp(12));
        weather.addView(label("TODAY’S WEATHER"));
        weatherPlace = text(store.weatherLocation, 12, MUTED, false);
        weatherPlace.setSingleLine(true);
        weatherPlace.setEllipsize(android.text.TextUtils.TruncateAt.END);
        weather.addView(weatherPlace, matchWrap());

        LinearLayout current = row();
        current.setGravity(Gravity.CENTER_VERTICAL);
        weatherIcon = new WeatherIconView();
        current.addView(weatherIcon, new LinearLayout.LayoutParams(dp(56), dp(56)));
        weatherTemperature = text("—", 38, INK, true);
        weatherTemperature.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams temperatureParams = new LinearLayout.LayoutParams(dp(88), dp(62));
        temperatureParams.leftMargin = dp(6);
        current.addView(weatherTemperature, temperatureParams);
        LinearLayout description = column();
        description.setGravity(Gravity.CENTER_VERTICAL);
        weatherCondition = text("Checking the sky…", 16, INK, true);
        weatherCondition.setMaxLines(2);
        description.addView(weatherCondition, matchWrap());
        weatherFeelsLike = text("", 13, MUTED, false);
        description.addView(weatherFeelsLike, matchWrap());
        current.addView(description, new LinearLayout.LayoutParams(0, -1, 1f));
        LinearLayout.LayoutParams currentParams = matchWrap();
        currentParams.topMargin = dp(3);
        weather.addView(current, currentParams);

        weatherDetails = text("", 13, MUTED, false);
        weather.addView(weatherDetails, matchWrap());

        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(231, 216, 163));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
        dividerParams.topMargin = dp(9);
        dividerParams.bottomMargin = dp(7);
        weather.addView(divider, dividerParams);
        weather.addView(text("READY TO GO?", 12, MUTED, true), matchWrap());

        weatherOutfit = row();
        weatherOutfit.setGravity(Gravity.CENTER);
        weatherTopIcon = new ImageView(this);
        weatherTopLabel = text("", 13, INK, true);
        weatherOutfit.addView(outfitItem(weatherTopIcon, weatherTopLabel), new LinearLayout.LayoutParams(0, -1, 1f));
        ImageView trousers = new ImageView(this);
        trousers.setImageResource(R.drawable.ic_weather_pants);
        weatherOutfit.addView(outfitItem(trousers, text("Pants", 13, INK, true)), new LinearLayout.LayoutParams(0, -1, 1f));
        weatherShoesIcon = new ImageView(this);
        weatherShoesLabel = text("", 13, INK, true);
        weatherOutfit.addView(outfitItem(weatherShoesIcon, weatherShoesLabel), new LinearLayout.LayoutParams(0, -1, 1f));
        weatherOutfit.setVisibility(View.INVISIBLE);
        weather.addView(weatherOutfit, new LinearLayout.LayoutParams(-1, 0, 1f));
        weatherAdvice = text("Checking what to wear…", 13, INK, false);
        weatherAdvice.setGravity(Gravity.CENTER);
        weatherAdvice.setMaxLines(2);
        weather.addView(weatherAdvice, matchWrap());
        weatherSource = text("Weather by Open-Meteo", 10, MUTED, false);
        weatherSource.setGravity(Gravity.CENTER);
        weatherSource.setPadding(0, dp(3), 0, 0);
        weather.addView(weatherSource, matchWrap());

        if (store.hasWeatherCacheFor(store.weatherLocation)) {
            try {
                applyWeather(WeatherReport.parse(store.weatherCacheJson));
            } catch (org.json.JSONException error) {
                android.util.Log.w("FamilyHomeWeather", "Cannot read saved weather report", error);
            }
        }
        return weather;
    }

    private LinearLayout outfitItem(ImageView icon, TextView title) {
        LinearLayout item = column();
        item.setGravity(Gravity.CENTER);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        item.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52)));
        title.setGravity(Gravity.CENTER);
        item.addView(title, matchWrap());
        return item;
    }

    private void refreshWeather() {
        if (weatherCard == null || !WeatherVisibility.isConfigured(store.weatherLocation)) return;
        String requestedLocation = store.weatherLocation.trim();
        boolean hasCache = weatherReport != null;
        if (hasCache && System.currentTimeMillis() - store.weatherCacheUpdatedAt < WEATHER_CACHE_MS) return;
        new Thread(() -> {
            try {
                String endpoint = PortalConfig.serviceURL("/v1/weather?location=" + URLEncoder.encode(requestedLocation, "UTF-8"));
                HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(12000);
                PortalConfig.authorize(connection);
                String raw = read(connection);
                WeatherReport weather = WeatherReport.parse(raw);
                runOnUiThread(() -> {
                    if (weatherCard == null || store.weatherLocation == null || !requestedLocation.equals(store.weatherLocation.trim())) return;
                    store.cacheWeather(requestedLocation, raw);
                    applyWeather(weather);
                });
            } catch (Exception error) {
                android.util.Log.w("FamilyHomeWeather", "Cannot refresh weather report", error);
                runOnUiThread(() -> {
                    if (weatherCard == null || store.weatherLocation == null || !requestedLocation.equals(store.weatherLocation.trim())) return;
                    if (weatherReport != null) {
                        weatherSource.setText("Saved weather • " + new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                .format(new Date(store.weatherCacheUpdatedAt)) + " • Open-Meteo");
                        return;
                    }
                    weatherTemperature.setText("—");
                    weatherCondition.setText("Weather is resting");
                    weatherDetails.setText("Please try again soon.");
                    weatherAdvice.setText("Ask a grown-up what to wear.");
                    weatherIcon.setCondition("cloudy");
                });
            }
        }).start();
    }

    private void applyWeather(WeatherReport weather) {
        weatherReport = weather;
        weatherTemperature.setText(weather.temperature + "°");
        weatherCondition.setText(weather.condition);
        weatherFeelsLike.setText("Feels like " + weather.feelsLike + "°");
        String details = "High " + weather.high + "°  •  Low " + weather.low + "°  •  Rain " + weather.precipitation + "%";
        weatherDetails.setText(details);
        weatherPlace.setText(weather.place);
        weatherIcon.setCondition(weather.icon);
        WeatherReport.Outfit outfit = weather.outfit();
        weatherTopIcon.setImageResource(outfit.topIcon);
        weatherTopLabel.setText(outfit.top);
        weatherShoesIcon.setImageResource(outfit.shoesIcon);
        weatherShoesLabel.setText(outfit.shoes);
        weatherAdvice.setText(outfit.advice);
        weatherOutfit.setVisibility(View.VISIBLE);
        weatherSource.setText(System.currentTimeMillis() - store.weatherCacheUpdatedAt < WEATHER_CACHE_MS
                ? "Weather by Open-Meteo"
                : "Saved weather • " + new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        .format(new Date(store.weatherCacheUpdatedAt)) + " • Open-Meteo");
        weatherCard.setContentDescription("Weather for " + weather.place + ". " + weather.temperature
                + " degrees and " + weather.condition + ". Feels like " + weather.feelsLike + ". " + details
                + ". Ready to go? " + outfit.top + ", pants, " + outfit.shoes + ". " + outfit.advice);
    }

    static String read(HttpURLConnection connection) throws Exception {
        int status = connection.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(status >= 400 ? connection.getErrorStream() : connection.getInputStream(), "UTF-8"));
        StringBuilder body = new StringBuilder(); String line;
        while ((line = reader.readLine()) != null) body.append(line);
        reader.close();
        if (status >= 400) throw new IllegalStateException(body.toString());
        return body.toString();
    }

    private static String formatTime(String value) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(value);
            return new SimpleDateFormat("EEEE, MMMM d 'at' h:mm a", Locale.getDefault()).format(date);
        } catch (Exception ignored) { return value; }
    }

    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout card(int color) { LinearLayout v = column(); v.setPadding(dp(26), dp(24), dp(26), dp(24)); v.setBackground(rounded(color, 24)); v.setElevation(dp(3)); return v; }
    private TextView label(String value) { TextView v = text(value, 14, MUTED, true); v.setLetterSpacing(.08f); return v; }
    private TextView text(String value, int size, int color, boolean bold) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL); return v; }
    private Button button(String value, int color) { return button(value, color, Color.WHITE); }
    private Button button(String value, int color, int textColor) { Button v = new Button(this); v.setText(value); v.setTextSize(18); v.setTextColor(textColor); v.setAllCaps(false); v.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); v.setBackground(rounded(color, 20)); v.setPadding(dp(18), dp(8), dp(18), dp(8)); return v; }
    private Button customTimerButton(String value, int color, int textColor) { Button v = button(value, color, textColor); v.setTextSize(15); return v; }
    private ActivityTile actionTile(String title, String subtitle, int color, int icon) { return new ActivityTile(title, subtitle, color, icon); }
    private TimerPresetTile timerPreset(String title, String duration, int color, int icon, Runnable command) { return new TimerPresetTile(title, duration, color, icon, command); }
    private GradientDrawable rounded(int color, int radius) { GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams weighted(float weight, int height) { return new LinearLayout.LayoutParams(0, dp(height), weight); }
    private LinearLayout.LayoutParams spacedWeighted(float weight, int height, boolean left) { LinearLayout.LayoutParams p = weighted(weight, height); if (left) p.leftMargin = dp(8); else p.rightMargin = dp(8); return p; }
    private LinearLayout.LayoutParams actionParams() { LinearLayout.LayoutParams p = weighted(1f, 92); p.leftMargin = dp(6); p.rightMargin = dp(6); return p; }
    private LinearLayout.LayoutParams cardParams(int position, int count) {
        LinearLayout.LayoutParams params = weighted(1f, 300);
        params.topMargin = dp(28);
        params.bottomMargin = dp(12);
        int gap = count == 3 ? 8 : 12;
        if (position > 0) params.leftMargin = dp(gap);
        if (position < count - 1) params.rightMargin = dp(gap);
        return params;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private final class TimerPresetTile extends LinearLayout {
        TimerPresetTile(String titleValue, String durationValue, int color, int iconKind, Runnable command) {
            super(MainActivity.this);
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            setPadding(dp(8), dp(4), dp(8), dp(4));
            setBackgroundColor(color);
            android.util.TypedValue ripple = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
            setForeground(getDrawable(ripple.resourceId));
            setClickable(true);
            setFocusable(true);
            setOnClickListener(view -> command.run());

            ImageView illustration = new ImageView(MainActivity.this);
            illustration.setImageResource(iconKind);
            illustration.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(illustration, new LinearLayout.LayoutParams(dp(56), dp(56)));
            LinearLayout words = column();
            TextView title = text(titleValue, 19, INK, true);
            title.setGravity(Gravity.CENTER);
            TextView duration = text(durationValue, 16, MUTED, false);
            duration.setGravity(Gravity.CENTER);
            duration.setPadding(0, dp(2), 0, 0);
            words.addView(title, matchWrap());
            words.addView(duration, matchWrap());
            LinearLayout.LayoutParams wordsParams = matchWrap();
            wordsParams.topMargin = dp(2);
            addView(words, wordsParams);
            setContentDescription(titleValue + " timer, " + durationValue);
        }
    }


    private final class ActivityTile extends LinearLayout {
        private final TileIconView icon;
        private final TextView title;
        private final TextView subtitle;

        ActivityTile(String titleValue, String subtitleValue, int color, int iconKind) {
            super(MainActivity.this);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(dp(14), 0, dp(12), 0);
            setElevation(dp(4));
            setClickable(true);
            setFocusable(true);

            icon = new TileIconView(iconKind);
            addView(icon, new LinearLayout.LayoutParams(dp(50), dp(50)));

            LinearLayout words = column();
            words.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            title = text(titleValue, 18, Color.WHITE, true);
            title.setGravity(Gravity.LEFT);
            subtitle = text(subtitleValue, 15, Color.WHITE, true);
            subtitle.setGravity(Gravity.LEFT);
            subtitle.setPadding(0, dp(2), 0, 0);
            words.addView(title, matchWrap());
            words.addView(subtitle, matchWrap());
            LinearLayout.LayoutParams wordsParams = new LinearLayout.LayoutParams(0, -2, 1f);
            wordsParams.leftMargin = dp(12);
            addView(words, wordsParams);

            setContent(titleValue, subtitleValue, color, Color.WHITE);
        }

        void setContent(String titleValue, String subtitleValue, int color, int contentColor) {
            title.setText(titleValue);
            subtitle.setText(subtitleValue);
            title.setTextColor(contentColor);
            subtitle.setTextColor(contentColor);
            icon.setInkColor(contentColor);
            setBackground(rounded(color, 24));
            setContentDescription(titleValue + ". " + subtitleValue);
            setAlpha(1f);
        }
    }

    private final class TileIconView extends View {
        private final int kind;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Drawable standardIcon;
        private int inkColor = Color.WHITE;

        TileIconView(int kind) {
            super(MainActivity.this);
            this.kind = kind;
            standardIcon = kind == ICON_MUSIC ? getDrawable(R.drawable.ic_music_note).mutate() : null;
            if (standardIcon != null) standardIcon.setTint(inkColor);
        }

        void setInkColor(int color) {
            inkColor = color;
            if (standardIcon != null) standardIcon.setTint(color);
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float scaleX = getWidth() / 52f;
            float scaleY = getHeight() / 52f;
            canvas.save();
            canvas.scale(scaleX, scaleY);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(34, 0, 0, 0));
            canvas.drawCircle(26, 26, 24, paint);

            paint.setColor(inkColor);
            paint.setStrokeWidth(3.2f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.STROKE);

            if (standardIcon != null) {
                standardIcon.setBounds(13, 12, 39, 40);
                standardIcon.draw(canvas);
            } else if (kind == ICON_DRAW) drawPencil(canvas);
            else if (kind == ICON_ASK) drawQuestion(canvas);
            else if (kind == ICON_PLAY) drawController(canvas);
            else drawFlag(canvas);

            canvas.restore();
        }

        private void drawPencil(Canvas canvas) {
            canvas.drawLine(16, 36, 35, 17, paint);
            canvas.drawLine(20, 39, 39, 20, paint);
            canvas.drawLine(35, 17, 39, 20, paint);
            Path tip = new Path();
            tip.moveTo(16, 36); tip.lineTo(20, 39); tip.lineTo(13, 42); tip.close();
            canvas.drawPath(tip, paint);
            canvas.drawLine(22, 33, 25, 36, paint);
        }

        private void drawQuestion(Canvas canvas) {
            canvas.drawRoundRect(new RectF(12, 12, 40, 35), 11, 11, paint);
            Path tail = new Path();
            tail.moveTo(20, 34); tail.lineTo(17, 41); tail.lineTo(27, 35);
            canvas.drawPath(tail, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(24);
            canvas.drawText("?", 26, 31, paint);
        }

        private void drawController(Canvas canvas) {
            canvas.drawRoundRect(new RectF(10, 17, 42, 36), 8, 8, paint);
            canvas.drawLine(18, 22, 18, 31, paint);
            canvas.drawLine(14, 26.5f, 22, 26.5f, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(33, 24, 2, paint);
            canvas.drawCircle(37, 29, 2, paint);
        }

        private void drawFlag(Canvas canvas) {
            canvas.drawLine(14, 11, 14, 41, paint);
            canvas.drawRect(new RectF(15, 13, 39, 31), paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(new RectF(16.5f, 14.5f, 27, 22), paint);
            canvas.drawRect(new RectF(27, 22, 37.5f, 29.5f), paint);
        }
    }

    private final class WeatherIconView extends View {
        private final Paint weatherPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private String condition = "partly_cloudy";

        WeatherIconView() {
            super(MainActivity.this);
            setContentDescription("Weather illustration");
        }

        void setCondition(String value) {
            condition = value == null ? "cloudy" : value;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float scaleX = getWidth() / 80f;
            float scaleY = getHeight() / 80f;
            canvas.save();
            canvas.scale(scaleX, scaleY);
            if (condition.equals("clear")) {
                drawSun(canvas, 40, 40, 18);
            } else {
                if (condition.equals("partly_cloudy")) drawSun(canvas, 29, 28, 14);
                drawCloud(canvas);
                if (condition.equals("rain")) drawRain(canvas);
                else if (condition.equals("snow")) drawSnow(canvas);
                else if (condition.equals("storm")) drawStorm(canvas);
                else if (condition.equals("fog")) drawFog(canvas);
            }
            canvas.restore();
        }

        private void drawSun(Canvas canvas, float centerX, float centerY, float radius) {
            weatherPaint.setColor(Color.rgb(255, 181, 45));
            weatherPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(centerX, centerY, radius, weatherPaint);
            weatherPaint.setStrokeWidth(3f);
            weatherPaint.setStrokeCap(Paint.Cap.ROUND);
            for (int angle = 0; angle < 360; angle += 45) {
                double radians = Math.toRadians(angle);
                canvas.drawLine(
                        centerX + (float) Math.cos(radians) * (radius + 5),
                        centerY + (float) Math.sin(radians) * (radius + 5),
                        centerX + (float) Math.cos(radians) * (radius + 10),
                        centerY + (float) Math.sin(radians) * (radius + 10), weatherPaint);
            }
        }

        private void drawCloud(Canvas canvas) {
            weatherPaint.setColor(condition.equals("cloudy") || condition.equals("fog") ? Color.rgb(125, 145, 168) : Color.rgb(85, 151, 220));
            weatherPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(28, 43, 13, weatherPaint);
            canvas.drawCircle(43, 35, 17, weatherPaint);
            canvas.drawCircle(58, 44, 12, weatherPaint);
            canvas.drawRoundRect(new RectF(20, 42, 68, 58), 8, 8, weatherPaint);
        }

        private void drawRain(Canvas canvas) {
            weatherPaint.setColor(Color.rgb(63, 132, 255));
            weatherPaint.setStyle(Paint.Style.STROKE);
            weatherPaint.setStrokeWidth(4f);
            weatherPaint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(29, 63, 25, 71, weatherPaint);
            canvas.drawLine(45, 63, 41, 71, weatherPaint);
            canvas.drawLine(61, 63, 57, 71, weatherPaint);
        }

        private void drawSnow(Canvas canvas) {
            weatherPaint.setColor(Color.WHITE);
            weatherPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(28, 68, 3, weatherPaint);
            canvas.drawCircle(45, 65, 3, weatherPaint);
            canvas.drawCircle(61, 70, 3, weatherPaint);
        }

        private void drawStorm(Canvas canvas) {
            weatherPaint.setColor(Color.rgb(255, 181, 45));
            weatherPaint.setStyle(Paint.Style.FILL);
            Path bolt = new Path();
            bolt.moveTo(43, 58); bolt.lineTo(35, 72); bolt.lineTo(44, 70); bolt.lineTo(40, 79); bolt.lineTo(56, 63); bolt.lineTo(47, 65); bolt.close();
            canvas.drawPath(bolt, weatherPaint);
        }

        private void drawFog(Canvas canvas) {
            weatherPaint.setColor(Color.rgb(125, 145, 168));
            weatherPaint.setStyle(Paint.Style.STROKE);
            weatherPaint.setStrokeWidth(3f);
            weatherPaint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(22, 65, 64, 65, weatherPaint);
            canvas.drawLine(28, 72, 58, 72, weatherPaint);
        }
    }
}
