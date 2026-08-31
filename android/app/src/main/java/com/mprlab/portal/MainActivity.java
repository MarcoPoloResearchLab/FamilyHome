package com.mprlab.portal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(255, 248, 234);
    private static final int SYSTEM_BAR = Color.rgb(36, 49, 71);
    private static final int SURFACE = Color.WHITE;
    private static final int INK = Color.rgb(36, 49, 71);
    private static final int MUTED = Color.rgb(92, 104, 124);
    private static final int BLUE = Color.rgb(63, 132, 255);
    private static final int PURPLE = Color.rgb(124, 92, 252);
    private static final int CORAL = Color.rgb(255, 105, 105);
    private static final int TEAL = Color.rgb(0, 166, 153);
    private static final int PINK = Color.rgb(232, 84, 145);
    private static final int MUSIC_BLUE = Color.rgb(67, 114, 235);
    private static final int PALE_BLUE = Color.rgb(232, 243, 255);
    private static final int PALE_GREEN = Color.rgb(234, 249, 240);
    private static final int ICON_DRAW = 1;
    private static final int ICON_ASK = 2;
    private static final int ICON_PLAY = 3;
    private static final int ICON_RACE = 4;
    private static final int ICON_MUSIC = 5;
    private static final long READING_MS = 20L * 60L * 1000L;
    private final Handler handler = new Handler();
    private ProfileStore store;
    private TextView clock, eventTitle, eventTime, timerText;
    private Button timerButton;
    private ActivityTile gameButton, kartButton;
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        store = new ProfileStore(this);
        render();
        if (store.profiles.isEmpty()) handler.postDelayed(() -> {
            if (!setupPrompted) { setupPrompted = true; editProfile(null, true); }
        }, 300L);
    }

    @Override protected void onResume() {
        super.onResume();
        store.load();
        render();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
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
        root.setPadding(dp(34), dp(28), dp(34), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        LinearLayout header = row();
        Button profile = button(store.active == null ? "Create a child space" : "Hi, " + store.active.name + "!  ▾", PURPLE);
        profile.setOnClickListener(v -> showProfiles());
        header.addView(profile, weighted(1f, 62));
        clock = text("", 22, INK, true);
        clock.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        header.addView(clock, weighted(1.35f, 62));
        root.addView(header, matchWrap());

        LinearLayout cards = row();
        LinearLayout calendarCard = card(PALE_BLUE);
        calendarCard.addView(label("COMING UP"));
        eventTitle = text("What’s next?", 29, INK, true);
        eventTitle.setPadding(0, dp(22), 0, dp(8));
        calendarCard.addView(eventTitle);
        eventTime = text("Add a calendar to see the next adventure.", 18, MUTED, false);
        calendarCard.addView(eventTime);
        Button connect = button("Add or change calendar", BLUE);
        connect.setOnClickListener(v -> editProfile(store.active, store.active == null));
        LinearLayout.LayoutParams connectParams = matchWrap();
        connectParams.topMargin = dp(24);
        calendarCard.addView(connect, connectParams);

        LinearLayout timerCard = card(PALE_GREEN);
        timerCard.addView(label("READING TIME"));
        timerText = text("20:00", 48, INK, true);
        timerText.setPadding(0, dp(18), 0, dp(18));
        timerCard.addView(timerText);
        LinearLayout timerActions = row();
        timerButton = button("Start", BLUE);
        timerButton.setOnClickListener(v -> toggleTimer());
        Button reset = button("Start over", SURFACE, INK);
        reset.setOnClickListener(v -> {
            if (store.active != null) {
                store.active.timerRunning = false;
                store.active.remainingMs = READING_MS;
                store.save();
                updateTimer();
            }
        });
        timerActions.addView(timerButton, spacedWeighted(1f, 58, false));
        timerActions.addView(reset, spacedWeighted(1f, 58, true));
        timerCard.addView(timerActions, matchWrap());

        LinearLayout.LayoutParams left = weighted(1f, 300); left.topMargin = dp(28); left.rightMargin = dp(12);
        LinearLayout.LayoutParams right = weighted(1f, 300); right.topMargin = dp(28); right.leftMargin = dp(12);
        cards.addView(calendarCard, left);
        cards.addView(timerCard, right);
        root.addView(cards, matchWrap());

        View activitySpacer = new View(this);
        root.addView(activitySpacer, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout actions = row();
        ActivityTile draw = actionTile("Draw", "Make a picture", CORAL, ICON_DRAW); draw.setOnClickListener(v -> launch(DrawingActivity.class));
        ActivityTile ask = actionTile("Ask", "Learn something", PURPLE, ICON_ASK); ask.setOnClickListener(v -> launch(AskActivity.class));
        ActivityTile music = actionTile("Music", "Play piano", MUSIC_BLUE, ICON_MUSIC); music.setOnClickListener(v -> launch(PianoActivity.class));
        gameButton = actionTile("Play", "Adventure game", TEAL, ICON_PLAY); gameButton.setOnClickListener(v -> launchFreedoom());
        kartButton = actionTile("Race", "Kart Adventure", PINK, ICON_RACE); kartButton.setOnClickListener(v -> launchKart());
        actions.addView(draw, actionParams());
        actions.addView(ask, actionParams());
        actions.addView(music, actionParams());
        actions.addView(gameButton, actionParams());
        actions.addView(kartButton, actionParams());
        LinearLayout.LayoutParams actionRow = matchWrap(); actionRow.topMargin = dp(20);
        root.addView(actions, actionRow);

        setContentView(scroll);
        updateTimer();
        updateGameButton();
        updateKartButton();
        refreshCalendar();
    }

    private void launch(Class<?> type) {
        if (store.active == null) { editProfile(null, true); return; }
        Intent intent = new Intent(this, type);
        intent.putExtra("profile_id", store.active.id);
        intent.putExtra("profile_name", store.active.name);
        startActivity(intent);
    }

    private void launchFreedoom() {
        if (store.active == null || !store.active.freedoomEnabled) {
            Toast.makeText(this, "Add the adventure game to this child’s space.", Toast.LENGTH_LONG).show();
            if (store.active != null) editProfile(store.active, false);
            return;
        }
        try {
            getPackageManager().getApplicationInfo(PortalConfig.FREEDOOM_PACKAGE, 0);
        } catch (PackageManager.NameNotFoundException error) {
            Toast.makeText(this, "The adventure game is not installed yet.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent launch = new Intent(Intent.ACTION_MAIN);
        launch.setClassName(PortalConfig.FREEDOOM_PACKAGE, "net.nullsum.freedoom.PortalGameActivity");
        launch.putExtra("portal_profile_id", store.active.id);
        try {
            startActivity(launch);
        } catch (SecurityException error) {
            Toast.makeText(this, "The Portal edition of the adventure game is required.", Toast.LENGTH_LONG).show();
        }
    }

    private void launchKart() {
        if (store.active == null || !store.active.kartEnabled) {
            Toast.makeText(this, "Add Kart Adventure to this child’s space.", Toast.LENGTH_LONG).show();
            if (store.active != null) editProfile(store.active, false);
            return;
        }
        try {
            getPackageManager().getApplicationInfo(PortalConfig.KART_PACKAGE, 0);
        } catch (PackageManager.NameNotFoundException error) {
            Toast.makeText(this, "Kart Adventure is not installed yet.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent launch = new Intent(Intent.ACTION_MAIN);
        launch.setClassName(PortalConfig.KART_PACKAGE, PortalConfig.KART_ACTIVITY);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.putExtra("portal_profile_id", store.active.id);
        launch.putExtra("portal_profile_name", store.active.name);
        try {
            startActivity(launch);
        } catch (RuntimeException error) {
            Toast.makeText(this, "Kart Adventure could not open.", Toast.LENGTH_LONG).show();
        }
    }

    private void showProfiles() {
        String[] names = new String[store.profiles.size() + 1];
        for (int i = 0; i < store.profiles.size(); i++) names[i] = store.profiles.get(i).name;
        names[names.length - 1] = "+ Add another child";
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert).setTitle("Whose turn is it?")
                .setItems(names, (dialog, which) -> {
                    if (which == store.profiles.size()) editProfile(null, false);
                    else { store.active = store.profiles.get(which); store.save(); render(); }
                }).setNegativeButton("Close", null);
        if (store.active != null) builder.setNeutralButton("Edit " + store.active.name, (dialog, which) -> editProfile(store.active, false));
        builder.show();
    }

    private void editProfile(final ProfileStore.Profile profile, final boolean required) {
        LinearLayout form = column();
        form.setPadding(dp(28), dp(8), dp(28), 0);
        form.setBackgroundColor(Color.WHITE);
        TextView formTitle = text(profile == null ? "Create a child space" : "Change this child space", 26, INK, true);
        formTitle.setPadding(0, dp(8), 0, dp(12));
        form.addView(formTitle, matchWrap());
        EditText name = new EditText(this); name.setHint("Child's name"); name.setSingleLine(); name.setText(profile == null ? "" : profile.name); name.setTextColor(Color.BLACK); name.setHintTextColor(Color.DKGRAY);
        EditText calendar = new EditText(this); calendar.setHint("Private iCalendar / ICS link (optional)"); calendar.setSingleLine(); calendar.setText(profile == null ? "" : profile.calendarUrl); calendar.setTextColor(Color.BLACK); calendar.setHintTextColor(Color.DKGRAY);
        CheckBox game = new CheckBox(this); game.setText("Add Adventure Game"); game.setTextSize(18); game.setTextColor(INK); game.setChecked(profile != null && profile.freedoomEnabled);
        CheckBox kart = new CheckBox(this); kart.setText("Add Kart Adventure"); kart.setTextSize(18); kart.setTextColor(INK); kart.setChecked(profile != null && profile.kartEnabled);
        form.addView(name, matchWrap());
        LinearLayout.LayoutParams cp = matchWrap(); cp.topMargin = dp(12); form.addView(calendar, cp);
        LinearLayout.LayoutParams gp = matchWrap(); gp.topMargin = dp(14); form.addView(game, gp);
        LinearLayout.LayoutParams kp = matchWrap(); kp.topMargin = dp(8); form.addView(kart, kp);
        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert).setView(form)
                .setPositiveButton("Save", null).setNegativeButton(required ? null : "Cancel", null).create();
        dialog.setCanceledOnTouchOutside(!required);
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = name.getText().toString().trim();
            if (value.isEmpty()) { name.setError("Enter a name"); return; }
            ProfileStore.Profile saved = profile == null ? store.add(value) : profile;
            saved.name = value;
            saved.calendarUrl = calendar.getText().toString().trim();
            saved.freedoomEnabled = game.isChecked();
            saved.kartEnabled = kart.isChecked();
            store.active = saved;
            store.save();
            dialog.dismiss();
            render();
        }));
        dialog.show();
    }

    private void toggleTimer() {
        if (store.active == null) { editProfile(null, true); return; }
        long now = System.currentTimeMillis();
        if (store.active.timerRunning) {
            store.active.remainingMs = Math.max(0L, store.active.timerEndEpochMs - now);
            store.active.timerRunning = false;
        } else {
            if (store.active.remainingMs <= 0) store.active.remainingMs = READING_MS;
            store.active.timerEndEpochMs = now + store.active.remainingMs;
            store.active.timerRunning = true;
        }
        store.save(); updateTimer();
    }

    private void updateTimer() {
        if (timerText == null || timerButton == null) return;
        long remaining = READING_MS; boolean running = false;
        if (store.active != null) {
            running = store.active.timerRunning;
            remaining = running ? Math.max(0, store.active.timerEndEpochMs - System.currentTimeMillis()) : store.active.remainingMs;
            if (running && remaining == 0) {
                store.active.timerRunning = false; store.active.remainingMs = 0; store.save(); running = false;
                Toast.makeText(this, "Great reading! Time’s up.", Toast.LENGTH_LONG).show();
            }
        }
        long seconds = (remaining + 999) / 1000;
        timerText.setText(String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60));
        timerButton.setText(running ? "Take a break" : "Let’s read");
    }

    private void updateGameButton() {
        if (gameButton == null) return;
        boolean enabled = store.active != null && store.active.freedoomEnabled;
        gameButton.setContent(enabled ? "Play" : "Add a game", enabled ? "Adventure game" : "Choose in profile",
                enabled ? TEAL : Color.rgb(217, 220, 228), enabled ? Color.WHITE : MUTED);
    }

    private void updateKartButton() {
        if (kartButton == null) return;
        boolean enabled = store.active != null && store.active.kartEnabled;
        kartButton.setContent(enabled ? "Race" : "Add kart racing", enabled ? "Kart Adventure" : "Choose in profile",
                enabled ? PINK : Color.rgb(217, 220, 228), enabled ? Color.WHITE : MUTED);
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
    private ActivityTile actionTile(String title, String subtitle, int color, int icon) { return new ActivityTile(title, subtitle, color, icon); }
    private GradientDrawable rounded(int color, int radius) { GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams weighted(float weight, int height) { return new LinearLayout.LayoutParams(0, dp(height), weight); }
    private LinearLayout.LayoutParams spacedWeighted(float weight, int height, boolean left) { LinearLayout.LayoutParams p = weighted(weight, height); if (left) p.leftMargin = dp(8); else p.rightMargin = dp(8); return p; }
    private LinearLayout.LayoutParams actionParams() { LinearLayout.LayoutParams p = weighted(1f, 92); p.leftMargin = dp(6); p.rightMargin = dp(6); return p; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

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
}
