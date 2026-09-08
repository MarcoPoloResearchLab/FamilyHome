package com.mprlab.portal;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.net.Uri;
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
    private static final int SYSTEM_BAR = PortalStyle.INK;
    private static final int INK = PortalStyle.INK;
    private static final int MUTED = PortalStyle.SECONDARY;
    private static final int BLUE = PortalStyle.BLUE;
    private static final int PURPLE = PortalStyle.PURPLE;
    private static final int CORAL = PortalStyle.CORAL;
    private static final int TEAL = PortalStyle.MINT;
    private static final int MUSIC_BLUE = PortalStyle.BLUE;
    private static final int PALE_BLUE = PortalStyle.BLUE;
    private static final int PALE_GREEN = PortalStyle.MINT;
    private static final int PALE_YELLOW = PortalStyle.YELLOW;
    private static final long READING_MS = 20L * 60L * 1000L;
    private static final long BRUSH_TEETH_MS = (2L * 60L + 15L) * 1000L;
    private static final long QUICK_TIMER_MS = 5L * 60L * 1000L;
    private static final long WEATHER_CACHE_MS = 15L * 60L * 1000L;
    private static final long WEATHER_RETRY_MS = 30L * 1000L;
    private static final String WEATHER_SOURCE = "Open-Meteo";
    private static final String WEATHER_SOURCE_URL = "https://open-meteo.com/en/licence";
    private final Handler handler = new Handler();
    private ProfileStore store;
    private TimeFormatSettings.Format timeFormat;
    private TextView clock, eventTitle, eventTime, timerText, timerStatus;
    private TextView countdownDisplay;
    private Button countdownPause;
    private TextView weatherTemperature, weatherCondition, weatherDetails, weatherPlace, weatherFeelsLike;
    private TextView weatherTopLabel, weatherShoesLabel, weatherDayOutlook, weatherPrecipitation;
    private ImageView weatherTopIcon, weatherShoesIcon;
    private LinearLayout weatherOutfit, weatherDayAdvice;
    private WeatherReport weatherReport;
    private LinearLayout weatherCard;
    private WeatherIconView weatherIcon;
    private boolean setupPrompted;
    private boolean weatherActive, weatherRequestPending;
    private int weatherGeneration;
    private long nextWeatherAttemptAt;
    private String weatherRequestLocation = "";

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (clock != null) {
                Date now = new Date();
                clock.setText(timeFormat.format(now) + new SimpleDateFormat("  •  EEEE, MMMM d", Locale.getDefault()).format(now));
            }
            updateTimer();
            refreshWeather();
            handler.postDelayed(this, 500L);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setStatusBarColor(SYSTEM_BAR);
        window.setNavigationBarColor(SYSTEM_BAR);
        store = new ProfileStore(this);
        timeFormat = TimeFormatSettings.read(this);
        render();
        if (store.profiles.isEmpty()) handler.postDelayed(() -> {
            if (!setupPrompted) { setupPrompted = true; openSettings(); }
        }, 300L);
    }

    @Override protected void onResume() {
        super.onResume();
        store.load();
        timeFormat = TimeFormatSettings.read(this);
        weatherActive = true;
        nextWeatherAttemptAt = 0L;
        render();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        if (store.active != null && store.active.timerRunning
                && store.active.timerEndEpochMs > System.currentTimeMillis()) {
            scheduleTimerAlarm(store.active);
        }
    }

    @Override protected void onPause() {
        weatherActive = false;
        weatherGeneration++;
        weatherRequestPending = false;
        handler.removeCallbacks(ticker);
        store.save();
        super.onPause();
    }

    private void render() {
        LinearLayout root = JoinedSurface.column(this);
        root.setBackgroundColor(PortalStyle.PAPER);
        LinearLayout header = row();
        header.setPadding(dp(12), dp(8), dp(12), dp(8));
        Button profile = button(store.active == null ? "Set up FamilyHome" : "Hi, " + store.active.name + "!  ▾", PURPLE);
        profile.setOnClickListener(v -> {
            if (store.profiles.isEmpty()) openSettings();
            else showProfiles();
        });
        PortalStyle.text(profile, PortalStyle.TextRole.TITLE);
        header.addView(profile, weighted(1f, 72));
        timerText = text("", PortalStyle.TextRole.CONTROL, INK);
        timerText.setGravity(Gravity.CENTER);
        timerText.setPadding(dp(12), 0, dp(12), 0);
        timerText.setBackground(rounded(PALE_YELLOW, 16));
        timerText.setClickable(true);
        timerText.setFocusable(true);
        timerText.setVisibility(View.GONE);
        timerText.setOnClickListener(v -> showCountdown());
        LinearLayout.LayoutParams activeTimer = new LinearLayout.LayoutParams(-2, dp(64));
        activeTimer.leftMargin = dp(10);
        header.addView(timerText, activeTimer);
        clock = text("", PortalStyle.TextRole.SECTION, INK);
        clock.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        header.addView(clock, weighted(1.35f, 72));
        ImageButton settings = new ImageButton(this);
        settings.setImageResource(R.drawable.ic_settings);
        settings.setColorFilter(INK);
        settings.setContentDescription("Open settings");
        settings.setPadding(dp(15), dp(15), dp(15), dp(15));
        settings.setBackground(rounded(PALE_YELLOW, 20));
        settings.setOnClickListener(view -> openSettings());
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(dp(72), dp(72));
        settingsParams.leftMargin = dp(14);
        header.addView(settings, settingsParams);
        root.addView(header, matchWrap());

        LinearLayout cards = JoinedSurface.row(this);
        LinearLayout calendarCard = column();
        calendarCard.setBackgroundColor(PALE_BLUE);
        calendarCard.setPadding(dp(20), dp(16), dp(20), dp(16));
        calendarCard.addView(text("Coming up", PortalStyle.TextRole.SECTION, INK), matchWrap());
        CharacterView calendar = new CharacterView(this, CharacterView.Kind.CALENDAR);
        calendarCard.addView(calendar, new LinearLayout.LayoutParams(-1, 0, 1f));
        eventTitle = text("What’s next?", PortalStyle.TextRole.PRIMARY, INK);
        calendarCard.addView(eventTitle, matchWrap());
        eventTime = text("Add a calendar to see the next adventure.", PortalStyle.TextRole.BODY, MUTED);
        calendarCard.addView(eventTime, matchWrap());
        Button connect = button("Add or change calendar", BLUE);
        connect.setOnClickListener(v -> openSettings());
        LinearLayout.LayoutParams connectParams = matchWrap();
        connectParams.topMargin = dp(12);
        calendarCard.addView(connect, connectParams);

        LinearLayout timerGrid = JoinedSurface.column(this);
        LinearLayout firstPresetRow = JoinedSurface.row(this);
        firstPresetRow.addView(timerPreset("Reading", "20 min", PALE_BLUE, CharacterView.Kind.BOOK,
                () -> startTimer(READING_MS, "Reading")), timerParams());
        firstPresetRow.addView(timerPreset("Brush teeth", "2 min 15 sec", PALE_GREEN, CharacterView.Kind.TOOTH,
                () -> startTimer(BRUSH_TEETH_MS, "Brush teeth")), timerParams());
        timerGrid.addView(firstPresetRow, new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout secondPresetRow = JoinedSurface.row(this);
        secondPresetRow.addView(timerPreset("Quick timer", "5 min", PALE_YELLOW, CharacterView.Kind.HOURGLASS,
                () -> startTimer(QUICK_TIMER_MS, "Quick")), timerParams());
        secondPresetRow.addView(timerPreset("Custom", "Choose time", CORAL, CharacterView.Kind.CLOCK,
                this::showCustomTimer), timerParams());
        timerGrid.addView(secondPresetRow, new LinearLayout.LayoutParams(-1, 0, 1f));

        boolean showWeather = WeatherVisibility.isConfigured(store.weatherLocation);
        cards.addView(calendarCard, new LinearLayout.LayoutParams(0, -1, 1f));
        cards.addView(timerGrid, new LinearLayout.LayoutParams(0, -1, 1.25f));
        if (showWeather) {
            weatherCard = createWeatherCard();
            cards.addView(weatherCard, new LinearLayout.LayoutParams(0, -1, 1f));
        } else {
            weatherCard = null;
            weatherIcon = null;
            weatherTemperature = null;
            weatherCondition = null;
            weatherDetails = null;
            weatherPlace = null;
        }
        root.addView(cards, new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout actions = JoinedSurface.row(this);
        ActivityTile draw = actionTile("Draw", "Make a picture", CORAL, CharacterView.Kind.PENCIL); draw.setOnClickListener(v -> launch(DrawingActivity.class));
        ActivityTile ask = actionTile("Ask", "Learn something", PURPLE, CharacterView.Kind.QUESTION); ask.setOnClickListener(v -> launch(AskActivity.class));
        ActivityTile music = actionTile("Music", "Choose an instrument", MUSIC_BLUE, CharacterView.Kind.MUSIC); music.setOnClickListener(v -> launch(MusicActivity.class));
        ActivityTile games = actionTile("Games", "Choose and play", TEAL, CharacterView.Kind.GAME); games.setOnClickListener(v -> launch(GameLibraryActivity.class));
        actions.addView(draw, actionParams());
        actions.addView(ask, actionParams());
        actions.addView(music, actionParams());
        actions.addView(games, actionParams());
        ActivityTile photo = actionTile("Photo Booth", "Take a picture", PALE_YELLOW, CharacterView.Kind.CAMERA);
        photo.setOnClickListener(v -> launch(PhotoBoothActivity.class));
        actions.addView(photo, actionParams());
        root.addView(actions, new LinearLayout.LayoutParams(-1, dp(200)));
        setContentView(root);
        updateTimer();
        refreshCalendar();
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
        showPortalDialog(new ProfilePickerDialog(this, store.profiles, store.active, profile -> {
            store.active = profile;
            store.save();
            render();
        }));
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
        countdownDisplay = text("", PortalStyle.TextRole.COUNTDOWN, INK);
        countdownDisplay.setGravity(Gravity.CENTER);
        content.addView(countdownDisplay, matchWrap());
        timerStatus = text("", PortalStyle.TextRole.BODY, MUTED);
        timerStatus.setGravity(Gravity.CENTER);
        content.addView(timerStatus, matchWrap());
        countdownPause = button("Pause", TEAL);
        countdownPause.setOnClickListener(view -> toggleTimer());
        LinearLayout.LayoutParams pauseParams = new LinearLayout.LayoutParams(-1, dp(PortalStyle.CONTROL_HEIGHT));
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
        showPortalDialog(dialog);
    }

    private void showCustomTimer() {
        if (store.active == null) { openSettings(); return; }
        LinearLayout duration = column();
        duration.setPadding(dp(24), dp(8), dp(24), dp(8));
        long[] customSeconds = {5L * 60L};
        Button[] startButton = {null};
        TextView customValue = text("05:00", PortalStyle.TextRole.DISPLAY, INK);
        customValue.setGravity(Gravity.CENTER);
        customValue.setContentDescription("Custom timer duration, 5 minutes");
        duration.addView(customValue, matchWrap());

        TextView customHelp = text("Use the buttons to change the timer.", PortalStyle.TextRole.BODY, MUTED);
        customHelp.setGravity(Gravity.CENTER);
        customHelp.setPadding(0, 0, 0, dp(12));
        duration.addView(customHelp, matchWrap());

        Runnable updateDuration = () -> {
            customValue.setText(String.format(Locale.US, "%02d:%02d", customSeconds[0] / 60L, customSeconds[0] % 60L));
            customValue.setContentDescription("Custom timer duration, " + customSeconds[0] + " seconds");
            if (startButton[0] != null) startButton[0].setEnabled(customSeconds[0] > 0L);
        };
        LinearLayout minuteControls = row();
        Button removeMinute = customTimerButton("− 1 minute", PALE_BLUE);
        removeMinute.setOnClickListener(view -> {
            customSeconds[0] = Math.max(0L, customSeconds[0] - 60L);
            updateDuration.run();
        });
        Button addMinute = customTimerButton("+ 1 minute", BLUE);
        addMinute.setOnClickListener(view -> {
            customSeconds[0] = Math.min(60L * 60L, customSeconds[0] + 60L);
            updateDuration.run();
        });
        minuteControls.addView(removeMinute, spacedWeighted(1f, 52, false));
        minuteControls.addView(addMinute, spacedWeighted(1f, 52, true));
        duration.addView(minuteControls, matchWrap());

        LinearLayout secondControls = row();
        Button removeSeconds = customTimerButton("− 15 seconds", PALE_YELLOW);
        removeSeconds.setOnClickListener(view -> {
            customSeconds[0] = Math.max(0L, customSeconds[0] - 15L);
            updateDuration.run();
        });
        Button addSeconds = customTimerButton("+ 15 seconds", CORAL);
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
        showPortalDialog(dialog);
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
        boolean hasTimer = store.active != null && store.active.timerEndEpochMs > 0 && remaining > 0;
        timerText.setVisibility(hasTimer ? View.VISIBLE : View.GONE);
        timerText.setText((running ? "Timer " : "Paused ") + countdown);
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
        LinearLayout weather = column();
        LinearLayout shell = column();
        shell.setBackgroundColor(PALE_YELLOW);
        weatherCard = shell;
        weatherReport = null;
        weather.setPadding(dp(20), dp(16), dp(24), dp(18));
        weatherPlace = text(store.weatherLocation, PortalStyle.TextRole.SECTION, INK);
        weather.addView(weatherPlace, matchWrap());

        LinearLayout now = column();
        weatherCondition = text("Checking the sky…", PortalStyle.TextRole.BODY, INK);
        now.addView(weatherHeading("Now", weatherCondition), matchWrap());
        LinearLayout current = row();
        current.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout readings = column();
        weatherTemperature = text("—", PortalStyle.TextRole.DISPLAY, INK);
        weatherTemperature.setGravity(Gravity.CENTER_VERTICAL);
        readings.addView(weatherTemperature, matchWrap());
        weatherFeelsLike = text("", PortalStyle.TextRole.BODY, MUTED);
        readings.addView(weatherFeelsLike, matchWrap());
        current.addView(readings, new LinearLayout.LayoutParams(0, -2, 1f));
        weatherIcon = new WeatherIconView();
        weatherIcon.setVisibility(View.INVISIBLE);
        current.addView(weatherIcon, new LinearLayout.LayoutParams(dp(72), dp(72)));
        now.addView(current, matchWrap());

        weatherOutfit = row();
        weatherOutfit.setGravity(Gravity.CENTER);
        weatherTopIcon = new ImageView(this);
        weatherTopLabel = text("", PortalStyle.TextRole.SECTION, INK);
        weatherOutfit.addView(outfitItem(weatherTopIcon, weatherTopLabel), new LinearLayout.LayoutParams(0, -1, 1f));
        ImageView trousers = new ImageView(this);
        trousers.setImageResource(R.drawable.ic_weather_pants);
        weatherOutfit.addView(outfitItem(trousers, text("Pants", PortalStyle.TextRole.SECTION, INK)), new LinearLayout.LayoutParams(0, -1, 1f));
        weatherShoesIcon = new ImageView(this);
        weatherShoesLabel = text("", PortalStyle.TextRole.SECTION, INK);
        weatherOutfit.addView(outfitItem(weatherShoesIcon, weatherShoesLabel), new LinearLayout.LayoutParams(0, -1, 1f));
        weatherOutfit.setVisibility(View.INVISIBLE);
        now.addView(weatherOutfit, new LinearLayout.LayoutParams(-1, -2));
        weather.addView(now, matchWrap());

        View divider = new View(this);
        divider.setBackgroundColor(INK);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(PortalStyle.OUTLINE_DP));
        dividerParams.topMargin = dp(8);
        dividerParams.bottomMargin = dp(8);
        weather.addView(divider, dividerParams);

        LinearLayout today = column();
        weatherDayOutlook = text("", PortalStyle.TextRole.BODY, INK);
        today.addView(weatherHeading("Today", weatherDayOutlook), matchWrap());
        weatherDetails = text("Checking today's forecast…", PortalStyle.TextRole.BODY, MUTED);
        today.addView(weatherDetails, matchWrap());
        weatherPrecipitation = text("", PortalStyle.TextRole.BODY, MUTED);
        today.addView(weatherPrecipitation, matchWrap());
        weatherDayAdvice = column();
        weatherDayAdvice.setPadding(0, dp(6), 0, 0);
        today.addView(weatherDayAdvice, matchWrap());
        weather.addView(today, matchWrap());
        TextView weatherSource = text(WEATHER_SOURCE, PortalStyle.TextRole.ATTRIBUTION, MUTED);
        weatherSource.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        weatherSource.setMinHeight(dp(PortalStyle.ATTRIBUTION_HEIGHT));
        weatherSource.setPadding(dp(8), 0, dp(8), 0);
        weatherSource.setPaintFlags(weatherSource.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        weatherSource.setContentDescription("Weather data by Open-Meteo. Open source and license.");
        weatherSource.setFocusable(true);
        PortalStyle.tile(weatherSource, PALE_YELLOW);
        weatherSource.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(WEATHER_SOURCE_URL)));
            } catch (ActivityNotFoundException error) {
                Toast.makeText(this, "No browser is available to open the weather source.", Toast.LENGTH_LONG).show();
            }
        });
        weather.addView(weatherSource, matchWrap());

        if (store.hasWeatherCacheFor(store.weatherLocation) && weatherCacheIsFresh()) {
            try {
                applyWeather(WeatherReport.parse(store.weatherCacheJson));
            } catch (org.json.JSONException error) {
                android.util.Log.w("FamilyHomeWeather", "Cannot read saved weather report", error);
            }
        }
        ScrollView detailsScroll = PortalStyle.scroll(this);
        detailsScroll.setFillViewport(true);
        detailsScroll.addView(weather);
        shell.addView(detailsScroll, new LinearLayout.LayoutParams(-1, -1));
        return shell;
    }

    private LinearLayout outfitItem(ImageView icon, TextView title) {
        LinearLayout item = column();
        PortalStyle.text(title, PortalStyle.TextRole.BODY);
        item.setGravity(Gravity.CENTER);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        item.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52)));
        title.setGravity(Gravity.CENTER);
        item.addView(title, matchWrap());
        return item;
    }

    private LinearLayout weatherHeading(String label, TextView summary) {
        LinearLayout heading = row();
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(label, PortalStyle.TextRole.SECTION, INK);
        title.setPadding(0, dp(4), dp(12), dp(4));
        heading.addView(title, new LinearLayout.LayoutParams(-2, -2));
        summary.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        heading.addView(summary, new LinearLayout.LayoutParams(0, -2, 1f));
        return heading;
    }

    private boolean weatherCacheIsFresh() {
        long age = System.currentTimeMillis() - store.weatherCacheUpdatedAt;
        return age >= 0L && age < WEATHER_CACHE_MS;
    }

    private void refreshWeather() {
        if (!weatherActive || weatherCard == null || !WeatherVisibility.isConfigured(store.weatherLocation)) return;
        String requestedLocation = store.weatherLocation.trim();
        if (!requestedLocation.equals(weatherRequestLocation)) {
            weatherGeneration++;
            weatherRequestLocation = requestedLocation;
            weatherRequestPending = false;
            nextWeatherAttemptAt = 0L;
        }
        if (weatherReport != null && weatherCacheIsFresh()) return;
        if (weatherReport != null) showWeatherUnavailable();
        if (weatherRequestPending || android.os.SystemClock.elapsedRealtime() < nextWeatherAttemptAt) return;
        weatherRequestPending = true;
        int generation = weatherGeneration;
        new Thread(() -> {
            try {
                String endpoint = PortalConfig.serviceURL("/v1/weather?location=" + URLEncoder.encode(requestedLocation, "UTF-8"));
                HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
                String raw;
                try {
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(12000);
                    PortalConfig.authorize(connection);
                    raw = read(connection);
                } finally {
                    connection.disconnect();
                }
                WeatherReport weather = WeatherReport.parse(raw);
                runOnUiThread(() -> {
                    if (!acceptWeatherResult(generation, requestedLocation)) return;
                    weatherRequestPending = false;
                    nextWeatherAttemptAt = 0L;
                    store.cacheWeather(requestedLocation, raw);
                    applyWeather(weather);
                });
            } catch (Exception error) {
                android.util.Log.w("FamilyHomeWeather", "Cannot refresh weather report", error);
                runOnUiThread(() -> {
                    if (!acceptWeatherResult(generation, requestedLocation)) return;
                    weatherRequestPending = false;
                    nextWeatherAttemptAt = android.os.SystemClock.elapsedRealtime() + WEATHER_RETRY_MS;
                    showWeatherUnavailable();
                });
            }
        }).start();
    }

    private boolean acceptWeatherResult(int generation, String location) {
        return weatherActive && generation == weatherGeneration && weatherCard != null
                && location.equals(store.weatherLocation.trim());
    }

    private void showWeatherUnavailable() {
        weatherReport = null;
        weatherTemperature.setText("—");
        weatherCondition.setText("Weather unavailable");
        weatherFeelsLike.setText("");
        weatherDayOutlook.setText("");
        weatherDetails.setText("Forecast unavailable");
        weatherPrecipitation.setText("");
        weatherDayAdvice.removeAllViews();
        weatherDayAdvice.addView(text("Trying again automatically.", PortalStyle.TextRole.BODY, MUTED), matchWrap());
        weatherTopLabel.setText("");
        weatherShoesLabel.setText("");
        weatherOutfit.setVisibility(View.INVISIBLE);
        weatherIcon.setVisibility(View.INVISIBLE);
        weatherCard.setContentDescription("Weather unavailable for " + store.weatherLocation + ". Trying again automatically.");
    }

    private void applyWeather(WeatherReport weather) {
        weatherReport = weather;
        weatherTemperature.setText(weather.temperature + "°");
        weatherCondition.setText(weather.condition);
        weatherFeelsLike.setText("Feels like " + weather.feelsLike + "°");
        String details = "High " + weather.high + "°  •  Low " + weather.low + "°";
        weatherDetails.setText(details);
        weatherPlace.setText(weather.place);
        weatherIcon.setCondition(weather.icon);
        weatherIcon.setVisibility(View.VISIBLE);
        WeatherReport.Outfit outfit = weather.outfit();
        weatherTopIcon.setImageResource(outfit.topIcon);
        weatherTopLabel.setText(outfit.top);
        weatherShoesIcon.setImageResource(outfit.shoesIcon);
        weatherShoesLabel.setText(outfit.shoes);
        weatherOutfit.setVisibility(View.VISIBLE);
        WeatherReport.DayPlan day = weather.dayPlan();
        weatherDayOutlook.setText(day.outlook);
        weatherPrecipitation.setText(day.precipitation);
        weatherDayAdvice.removeAllViews();
        for (String advice : day.advice) {
            weatherDayAdvice.addView(text(advice, PortalStyle.TextRole.BODY, INK), matchWrap());
        }
        weatherCard.setContentDescription("Weather for " + weather.place + ". Now: " + weather.temperature
                + " degrees and " + weather.condition + ". Feels like " + weather.feelsLike
                + ". Wear " + outfit.top + ", pants, " + outfit.shoes + ". Today: " + day.outlook + ". "
                + details + ". " + day.precipitation + ". " + android.text.TextUtils.join(" ", day.advice));
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

    private String formatTime(String value) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(value);
            return new SimpleDateFormat("EEEE, MMMM d 'at' ", Locale.getDefault()).format(date) + timeFormat.format(date);
        } catch (Exception ignored) { return value; }
    }

    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private TextView text(String value, PortalStyle.TextRole role, int color) { TextView v = new TextView(this); v.setText(value); PortalStyle.text(v, role); v.setTextColor(color); return v; }
    private Button button(String value, int color) { Button v = new Button(this); v.setText(value); v.setAllCaps(false); PortalStyle.button(v); v.setBackground(rounded(color, 20)); v.setPadding(dp(18), dp(8), dp(18), dp(8)); return v; }
    private Button customTimerButton(String value, int color) { Button v = button(value, color); PortalStyle.text(v, PortalStyle.TextRole.CONTROL); return v; }
    private ActivityTile actionTile(String title, String subtitle, int color, CharacterView.Kind icon) { return new ActivityTile(title, subtitle, color, icon); }
    private TimerPresetTile timerPreset(String title, String duration, int color, CharacterView.Kind icon, Runnable command) { return new TimerPresetTile(title, duration, color, icon, command); }
    private Drawable rounded(int color, int radius) { return PortalStyle.surface(this, color, radius); }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams weighted(float weight, int height) { return new LinearLayout.LayoutParams(0, dp(height), weight); }
    private LinearLayout.LayoutParams spacedWeighted(float weight, int height, boolean left) { LinearLayout.LayoutParams p = weighted(weight, height); if (left) p.leftMargin = dp(8); else p.rightMargin = dp(8); return p; }
    private LinearLayout.LayoutParams timerParams() { return new LinearLayout.LayoutParams(0, -1, 1f); }
    private LinearLayout.LayoutParams actionParams() { return new LinearLayout.LayoutParams(0, -1, 1f); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private final class TimerPresetTile extends LinearLayout {
        TimerPresetTile(String titleValue, String durationValue, int color, CharacterView.Kind iconKind, Runnable command) {
            super(MainActivity.this);
            setOrientation(VERTICAL);
            setPadding(dp(16), dp(12), dp(16), dp(10));
            PortalStyle.tile(this, color);
            setClickable(true);
            setFocusable(true);
            setOnClickListener(view -> command.run());
            TextView title = text(titleValue, PortalStyle.TextRole.CONTROL, INK);
            TextView duration = text(durationValue, PortalStyle.TextRole.BODY, INK);
            addView(title, matchWrap());
            addView(duration, matchWrap());
            CharacterView illustration = new CharacterView(MainActivity.this, iconKind);
            addView(illustration, new LinearLayout.LayoutParams(-1, 0, 1f));
            setContentDescription(titleValue + " timer, " + durationValue);
        }
    }


    private final class ActivityTile extends LinearLayout {
        ActivityTile(String titleValue, String subtitleValue, int color, CharacterView.Kind iconKind) {
            super(MainActivity.this);
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            setPadding(dp(12), dp(10), dp(12), dp(12));
            setClickable(true);
            setFocusable(true);
            PortalStyle.tile(this, color);
            addView(new CharacterView(MainActivity.this, iconKind), new LinearLayout.LayoutParams(-1, 0, 1));
            TextView title = text(titleValue, PortalStyle.TextRole.PRIMARY, INK);
            title.setGravity(Gravity.CENTER);
            addView(title, matchWrap());
            setContentDescription(titleValue + ". " + subtitleValue);
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
            weatherPaint.setColor(INK);
            weatherPaint.setStyle(Paint.Style.STROKE);
            weatherPaint.setStrokeWidth(3f);
            canvas.drawCircle(centerX, centerY, radius, weatherPaint);
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
            Path cloud = new Path();
            cloud.moveTo(20, 58);
            cloud.cubicTo(4, 53, 13, 28, 27, 31);
            cloud.cubicTo(31, 10, 59, 17, 59, 32);
            cloud.cubicTo(77, 31, 81, 57, 64, 58);
            cloud.close();
            weatherPaint.setStyle(Paint.Style.FILL);
            weatherPaint.setColor(condition.equals("cloudy") || condition.equals("fog") ? PortalStyle.BLUE : Color.WHITE);
            canvas.drawPath(cloud, weatherPaint);
            weatherPaint.setStyle(Paint.Style.STROKE);
            weatherPaint.setColor(INK); weatherPaint.setStrokeWidth(3f);
            canvas.drawPath(cloud, weatherPaint);
            weatherPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(34, 40, 2.5f, weatherPaint);
            canvas.drawCircle(52, 40, 2.5f, weatherPaint);
            weatherPaint.setStyle(Paint.Style.STROKE);
            canvas.drawArc(37, 42, 50, 50, 0, 180, false, weatherPaint);
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
