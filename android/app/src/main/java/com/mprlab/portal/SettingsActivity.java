package com.mprlab.portal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
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

import java.util.LinkedHashMap;
import java.util.Map;

public final class SettingsActivity extends Activity {
    private static final int BG = Color.rgb(255, 248, 234);
    private static final int SYSTEM_BAR = Color.rgb(36, 49, 71);
    private static final int SURFACE = Color.WHITE;
    private static final int INK = Color.rgb(36, 49, 71);
    private static final int MUTED = Color.rgb(92, 104, 124);
    private static final int PURPLE = Color.rgb(124, 92, 252);
    private static final int BLUE = Color.rgb(63, 132, 255);
    private static final int TEAL = Color.rgb(0, 166, 153);
    private static final int PALE_PURPLE = Color.rgb(244, 239, 255);
    private static final int PALE_BLUE = Color.rgb(232, 243, 255);
    private static final int PALE_GREEN = Color.rgb(234, 249, 240);

    private ProfileStore store;
    private EditText locationInput;
    private TextView locationStatus;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setStatusBarColor(SYSTEM_BAR);
        window.setNavigationBarColor(SYSTEM_BAR);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        store = new ProfileStore(this);
        render();
    }

    @Override protected void onPause() {
        saveLocation(false);
        super.onPause();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout root = column();
        root.setPadding(dp(34), dp(24), dp(34), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        LinearLayout header = row();
        LinearLayout heading = column();
        heading.addView(text("Settings", 32, INK, true), matchWrap());
        TextView subtitle = text("Make FamilyHome fit your family.", 17, MUTED, false);
        subtitle.setPadding(0, dp(3), 0, 0);
        heading.addView(subtitle, matchWrap());
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1f));
        Button done = button("Done", PURPLE, Color.WHITE);
        done.setOnClickListener(view -> {
            saveLocation(false);
            finish();
        });
        header.addView(done, new LinearLayout.LayoutParams(dp(126), dp(58)));
        root.addView(header, matchWrap());

        LinearLayout content = row();
        content.setGravity(Gravity.TOP);

        LinearLayout childrenPanel = panel();
        childrenPanel.addView(sectionLabel("CHILDREN"));
        childrenPanel.addView(sectionHelp("Add a child, choose their calendar and games, or switch the current child."));
        if (store.profiles.isEmpty()) {
            TextView empty = text("No child spaces yet", 19, MUTED, true);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(24), 0, dp(24));
            childrenPanel.addView(empty, matchWrap());
        } else {
            for (ProfileStore.Profile profile : store.profiles) {
                childrenPanel.addView(childRow(profile), childRowParams());
            }
        }
        Button addChild = button("＋  Add a child", TEAL, Color.WHITE);
        addChild.setOnClickListener(view -> editChild(null));
        LinearLayout.LayoutParams addParams = matchWrap();
        addParams.topMargin = dp(14);
        childrenPanel.addView(addChild, addParams);

        LinearLayout homeColumn = column();
        LinearLayout weatherPanel = panel();
        weatherPanel.addView(sectionLabel("WEATHER LOCATION"));
        weatherPanel.addView(sectionHelp("Use a ZIP code or city so FamilyHome can show weather for the right place."));
        locationInput = new EditText(this);
        locationInput.setSingleLine(true);
        locationInput.setHint("ZIP code or city");
        locationInput.setText(store.weatherLocation);
        locationInput.setTextSize(19);
        locationInput.setTextColor(Color.BLACK);
        locationInput.setHintTextColor(Color.DKGRAY);
        locationInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        locationInput.setPadding(dp(14), 0, dp(14), 0);
        locationInput.setBackground(rounded(PALE_BLUE, 14));
        LinearLayout.LayoutParams locationParams = matchWrap();
        locationParams.height = dp(58);
        locationParams.topMargin = dp(18);
        weatherPanel.addView(locationInput, locationParams);
        Button saveLocation = button("Save location", BLUE, Color.WHITE);
        saveLocation.setOnClickListener(view -> saveLocation(true));
        LinearLayout.LayoutParams saveParams = matchWrap();
        saveParams.topMargin = dp(12);
        weatherPanel.addView(saveLocation, saveParams);
        locationStatus = text(locationMessage(), 15, MUTED, false);
        locationStatus.setPadding(0, dp(10), 0, 0);
        weatherPanel.addView(locationStatus, matchWrap());
        homeColumn.addView(weatherPanel, matchWrap());

        LinearLayout devicePanel = panel();
        devicePanel.addView(sectionLabel("THIS PORTAL"));
        devicePanel.addView(sectionHelp("Child spaces and this location stay saved on this Portal. Each child keeps their own timer, drawings, calendar, and game choices."));
        LinearLayout.LayoutParams deviceParams = matchWrap();
        deviceParams.topMargin = dp(18);
        homeColumn.addView(devicePanel, deviceParams);

        LinearLayout.LayoutParams childrenParams = new LinearLayout.LayoutParams(0, -2, 1.18f);
        childrenParams.topMargin = dp(24);
        childrenParams.rightMargin = dp(12);
        content.addView(childrenPanel, childrenParams);
        LinearLayout.LayoutParams homeParams = new LinearLayout.LayoutParams(0, -2, 0.82f);
        homeParams.topMargin = dp(24);
        homeParams.leftMargin = dp(12);
        content.addView(homeColumn, homeParams);
        root.addView(content, matchWrap());

        setContentView(scroll);
    }

    private View childRow(ProfileStore.Profile profile) {
        boolean current = store.active != null && store.active.id.equals(profile.id);
        LinearLayout row = row();
        row.setPadding(dp(14), dp(10), dp(12), dp(10));
        row.setBackground(rounded(current ? PALE_GREEN : PALE_PURPLE, 16));
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription(profile.name + (current ? ", current child" : ", switch to this child"));
        row.setOnClickListener(view -> {
            saveLocation(false);
            store.active = profile;
            store.save();
            render();
        });

        TextView initial = text(profileInitial(profile.name), 22, Color.WHITE, true);
        initial.setGravity(Gravity.CENTER);
        GradientDrawable initialBackground = new GradientDrawable();
        initialBackground.setShape(GradientDrawable.OVAL);
        initialBackground.setColor(current ? TEAL : PURPLE);
        initial.setBackground(initialBackground);
        row.addView(initial, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout details = column();
        TextView name = text(profile.name, 20, INK, true);
        details.addView(name, matchWrap());
        TextView summary = text(childSummary(profile), 14, MUTED, false);
        summary.setPadding(0, dp(2), 0, 0);
        details.addView(summary, matchWrap());
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(0, -2, 1f);
        detailsParams.leftMargin = dp(12);
        row.addView(details, detailsParams);

        if (current) {
            TextView currentLabel = text("CURRENT", 12, TEAL, true);
            currentLabel.setGravity(Gravity.CENTER);
            currentLabel.setBackground(rounded(Color.WHITE, 12));
            LinearLayout.LayoutParams currentParams = new LinearLayout.LayoutParams(dp(84), dp(36));
            currentParams.rightMargin = dp(8);
            row.addView(currentLabel, currentParams);
        }

        Button edit = button("Edit", Color.WHITE, INK);
        edit.setOnClickListener(view -> editChild(profile));
        row.addView(edit, new LinearLayout.LayoutParams(dp(82), dp(44)));
        return row;
    }

    private void editChild(final ProfileStore.Profile profile) {
        LinearLayout form = column();
        form.setPadding(dp(28), dp(8), dp(28), dp(18));
        form.setBackgroundColor(Color.WHITE);
        TextView title = text(profile == null ? "Add a child" : "Edit " + profile.name, 26, INK, true);
        title.setPadding(0, dp(8), 0, dp(12));
        form.addView(title, matchWrap());

        EditText name = field("Child's name", profile == null ? "" : profile.name);
        EditText calendar = field("Private iCalendar / ICS link (optional)", profile == null ? "" : profile.calendarUrl);
        form.addView(name, matchWrap());
        LinearLayout.LayoutParams calendarParams = matchWrap();
        calendarParams.topMargin = dp(12);
        form.addView(calendar, calendarParams);

        TextView gameHeading = text("CHOOSE GAMES", 14, PURPLE, true);
        gameHeading.setLetterSpacing(.08f);
        LinearLayout.LayoutParams headingParams = matchWrap();
        headingParams.topMargin = dp(18);
        form.addView(gameHeading, headingParams);
        TextView gameHelp = text("These games appear only in this child’s game library.", 15, MUTED, false);
        gameHelp.setPadding(0, dp(4), 0, dp(8));
        form.addView(gameHelp, matchWrap());
        Map<String, CheckBox> gameChecks = new LinkedHashMap<>();
        for (GameCatalog.Game game : GameCatalog.all()) {
            CheckBox choice = gameChoice(game, profile != null && profile.isGameEnabled(game.id));
            gameChecks.put(game.id, choice);
            LinearLayout.LayoutParams choiceParams = matchWrap();
            choiceParams.topMargin = dp(7);
            form.addView(choice, choiceParams);
        }

        ScrollView formScroll = new ScrollView(this);
        formScroll.setFillViewport(true);
        formScroll.addView(form, new ScrollView.LayoutParams(-1, -2));

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
                .setView(formScroll)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String value = name.getText().toString().trim();
            if (value.isEmpty()) {
                name.setError("Enter a name");
                return;
            }
            saveLocation(false);
            ProfileStore.Profile saved = profile == null ? store.add(value) : profile;
            saved.name = value;
            saved.calendarUrl = calendar.getText().toString().trim();
            for (Map.Entry<String, CheckBox> entry : gameChecks.entrySet()) {
                saved.setGameEnabled(entry.getKey(), entry.getValue().isChecked());
            }
            store.active = saved;
            store.save();
            dialog.dismiss();
            render();
        }));
        dialog.show();
    }

    private void saveLocation(boolean announce) {
        if (locationInput == null) return;
        store.weatherLocation = locationInput.getText().toString().trim();
        store.save();
        if (locationStatus != null) locationStatus.setText(locationMessage());
        if (announce) Toast.makeText(this, store.weatherLocation.isEmpty() ? "Weather location cleared" : "Weather location saved", Toast.LENGTH_SHORT).show();
    }

    private String locationMessage() {
        return store.weatherLocation == null || store.weatherLocation.isEmpty()
                ? "Add a location whenever you want weather on the home screen."
                : "Saved on this Portal: " + store.weatherLocation;
    }

    private String childSummary(ProfileStore.Profile profile) {
        StringBuilder summary = new StringBuilder();
        if (profile.calendarUrl != null && !profile.calendarUrl.trim().isEmpty()) summary.append("Calendar");
        int gameCount = GameCatalog.enabledCount(profile);
        if (gameCount > 0) appendSummary(summary, gameCount + (gameCount == 1 ? " game" : " games"));
        return summary.length() == 0 ? "No calendar or games yet" : summary.toString();
    }

    private void appendSummary(StringBuilder summary, String value) {
        if (summary.length() > 0) summary.append("  •  ");
        summary.append(value);
    }

    private String profileInitial(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        return name.trim().substring(0, 1).toUpperCase();
    }

    private EditText field(String hint, String value) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setSingleLine(true);
        field.setText(value);
        field.setTextColor(Color.BLACK);
        field.setHintTextColor(Color.DKGRAY);
        return field;
    }

    private CheckBox gameChoice(GameCatalog.Game game, boolean checked) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(game.icon + "   " + game.name + "\n      " + game.description);
        checkBox.setTextSize(17);
        checkBox.setTextColor(INK);
        checkBox.setChecked(checked);
        checkBox.setButtonTintList(ColorStateList.valueOf(game.color));
        checkBox.setPadding(dp(14), dp(7), dp(14), dp(7));
        checkBox.setBackground(rounded(Color.rgb(248, 247, 252), 14));
        checkBox.setContentDescription(game.name + ". " + game.description);
        return checkBox;
    }

    private LinearLayout panel() {
        LinearLayout panel = column();
        panel.setPadding(dp(22), dp(20), dp(22), dp(20));
        panel.setBackground(rounded(SURFACE, 18));
        panel.setElevation(dp(2));
        return panel;
    }

    private TextView sectionLabel(String value) {
        TextView label = text(value, 14, PURPLE, true);
        label.setLetterSpacing(.08f);
        return label;
    }

    private TextView sectionHelp(String value) {
        TextView help = text(value, 16, MUTED, false);
        help.setPadding(0, dp(6), 0, 0);
        return help;
    }

    private LinearLayout.LayoutParams childRowParams() {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(10);
        return params;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private LinearLayout column() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        return column;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return text;
    }

    private Button button(String value, int color, int textColor) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(17);
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        button.setBackground(rounded(color, 16));
        button.setPadding(dp(14), dp(6), dp(14), dp(6));
        return button;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
