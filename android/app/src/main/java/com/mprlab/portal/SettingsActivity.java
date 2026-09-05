package com.mprlab.portal;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public final class SettingsActivity extends PortalActivity {
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
        root.setPadding(dp(34), dp(8), dp(34), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        LinearLayout header = row();
        header.addView(text("Settings", 27, INK, true), new LinearLayout.LayoutParams(0, -2, 1f));

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
        homeColumn.addView(screensaverPanel(), matchWrap());
        LinearLayout weatherPanel = panel();
        weatherPanel.addView(sectionLabel("WEATHER LOCATION"));
        weatherPanel.addView(sectionHelp("Use a ZIP code or city for the home-screen weather card. Leave it blank to hide weather."));
        locationInput = textInput();
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
        Button saveLocation = button("Save weather location", BLUE, Color.WHITE);
        saveLocation.setOnClickListener(view -> saveLocation(true));
        LinearLayout.LayoutParams saveParams = matchWrap();
        saveParams.topMargin = dp(12);
        weatherPanel.addView(saveLocation, saveParams);
        locationStatus = text(locationMessage(), 15, MUTED, false);
        locationStatus.setPadding(0, dp(10), 0, 0);
        weatherPanel.addView(locationStatus, matchWrap());
        LinearLayout.LayoutParams weatherParams = matchWrap();
        weatherParams.topMargin = dp(18);
        homeColumn.addView(weatherPanel, weatherParams);

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

        setContentView(PortalToolbar.screen(this, header, scroll, BG));
    }

    private View screensaverPanel() {
        ScreensaverSettings saved = ScreensaverSettings.read(this);
        LinearLayout panel = panel();
        panel.addView(sectionLabel("SCREENSAVER"));
        panel.addView(sectionHelp("Start after no activity in FamilyHome. Tap anywhere to return."));
        panel.addView(sectionHelp("Black screen hides everything at minimum brightness. The display stays powered."));
        Spinner mode = selection(panel, ScreensaverSettings.MODE_LABEL, ScreensaverSettings.Mode.values(), saved.mode.ordinal());
        Spinner timeout = selection(panel, ScreensaverSettings.TIMEOUT_LABEL, ScreensaverSettings.Timeout.values(), saved.timeout.ordinal());
        Button preview = button(ScreensaverSettings.PREVIEW_LABEL, PURPLE, Color.WHITE);
        preview.setContentDescription(ScreensaverSettings.PREVIEW_LABEL);
        preview.setOnClickListener(view -> showScreensaver());
        LinearLayout.LayoutParams previewParams = matchWrap();
        previewParams.topMargin = dp(12);
        panel.addView(preview, previewParams);
        Runnable save = () -> {
            ScreensaverSettings selected = new ScreensaverSettings((ScreensaverSettings.Mode) mode.getSelectedItem(),
                    (ScreensaverSettings.Timeout) timeout.getSelectedItem());
            boolean enabled = selected.mode != ScreensaverSettings.Mode.DISABLED;
            timeout.setEnabled(enabled);
            preview.setEnabled(enabled);
            preview.setAlpha(enabled ? 1f : .45f);
            ScreensaverSettings current = ScreensaverSettings.read(this);
            if (current.mode == selected.mode && current.timeout == selected.timeout) return;
            if (!selected.save(this)) {
                Toast.makeText(this, "Could not save screensaver settings", Toast.LENGTH_LONG).show();
                return;
            }
            reloadScreensaverSettings();
        };
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { save.run(); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        mode.setOnItemSelectedListener(listener);
        timeout.setOnItemSelectedListener(listener);
        save.run();
        return panel;
    }

    private <T> Spinner selection(LinearLayout panel, String label, T[] choices, int selected) {
        TextView title = text(label, 16, INK, true);
        title.setPadding(0, dp(12), 0, dp(4));
        panel.addView(title, matchWrap());
        Spinner spinner = new PortalSpinner(this);
        spinner.setPrompt(label);
        spinner.setContentDescription(label);
        ArrayAdapter<T> adapter = new ArrayAdapter<>(spinner.getContext(), android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selected);
        spinner.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        panel.addView(spinner, new LinearLayout.LayoutParams(-1, dp(52)));
        return spinner;
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
            store.active = saved;
            store.save();
            dialog.dismiss();
            render();
        }));
        showPortalDialog(dialog);
    }

    private void saveLocation(boolean announce) {
        if (locationInput == null) return;
        store.weatherLocation = locationInput.getText().toString().trim();
        store.save();
        if (locationStatus != null) locationStatus.setText(locationMessage());
        if (announce) Toast.makeText(this, store.weatherLocation.isEmpty() ? "Weather hidden from the home screen" : "Weather location saved", Toast.LENGTH_SHORT).show();
    }

    private String locationMessage() {
        return store.weatherLocation == null || store.weatherLocation.isEmpty()
                ? "Weather stays hidden until a location is saved."
                : "Saved on this Portal: " + store.weatherLocation;
    }

    private String childSummary(ProfileStore.Profile profile) {
        return profile.calendarUrl == null || profile.calendarUrl.trim().isEmpty()
                ? "No calendar yet"
                : "Calendar";
    }

    private String profileInitial(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        return name.trim().substring(0, 1).toUpperCase();
    }

    private EditText field(String hint, String value) {
        EditText field = textInput();
        field.setHint(hint);
        field.setSingleLine(true);
        field.setText(value);
        field.setTextColor(Color.BLACK);
        field.setHintTextColor(Color.DKGRAY);
        return field;
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
