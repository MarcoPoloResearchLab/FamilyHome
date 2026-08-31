package com.mprlab.portal;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class GameLibraryActivity extends Activity {
    private static final int BG = Color.rgb(255, 248, 234);
    private static final int SYSTEM_BAR = Color.rgb(36, 49, 71);
    private static final int INK = Color.rgb(36, 49, 71);
    private static final int MUTED = Color.rgb(92, 104, 124);
    private static final int PURPLE = Color.rgb(124, 92, 252);
    private static final int DISABLED = Color.rgb(217, 220, 228);

    private ProfileStore store;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setStatusBarColor(SYSTEM_BAR);
        window.setNavigationBarColor(SYSTEM_BAR);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        store = new ProfileStore(this);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        store.load();
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout root = column();
        root.setPadding(dp(38), dp(26), dp(38), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        LinearLayout header = row();
        LinearLayout heading = column();
        String childName = store.active == null ? "Your" : store.active.name + "’s";
        heading.addView(text(childName + " games", 34, INK, true), matchWrap());
        TextView help = text("Pick one and have fun.", 18, MUTED, false);
        help.setPadding(0, dp(3), 0, 0);
        heading.addView(help, matchWrap());
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1f));
        Button done = button("Done", PURPLE, Color.WHITE);
        done.setOnClickListener(view -> finish());
        header.addView(done, new LinearLayout.LayoutParams(dp(126), dp(58)));
        root.addView(header, matchWrap());

        List<GameCatalog.Game> games = enabledGames();
        if (games.isEmpty()) {
            LinearLayout empty = column();
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(30), dp(60), dp(30), dp(60));
            empty.setBackground(rounded(Color.WHITE, 24));
            empty.setElevation(dp(3));
            TextView icon = text("★", 58, PURPLE, true);
            icon.setGravity(Gravity.CENTER);
            empty.addView(icon, matchWrap());
            TextView title = text("Choose some games", 28, INK, true);
            title.setGravity(Gravity.CENTER);
            empty.addView(title, matchWrap());
            TextView message = text("A grown-up can choose games for this child in Settings.", 18, MUTED, false);
            message.setGravity(Gravity.CENTER);
            message.setPadding(0, dp(8), 0, dp(22));
            empty.addView(message, matchWrap());
            Button settings = button("Open Settings", PURPLE, Color.WHITE);
            settings.setOnClickListener(view -> startActivity(new Intent(this, SettingsActivity.class)));
            empty.addView(settings, new LinearLayout.LayoutParams(dp(230), dp(60)));
            LinearLayout.LayoutParams emptyParams = matchWrap();
            emptyParams.topMargin = dp(34);
            root.addView(empty, emptyParams);
        } else {
            LinearLayout grid = column();
            LinearLayout currentRow = null;
            for (int index = 0; index < games.size(); index++) {
                if (index % 3 == 0) {
                    currentRow = row();
                    LinearLayout.LayoutParams rowParams = matchWrap();
                    rowParams.topMargin = dp(22);
                    grid.addView(currentRow, rowParams);
                }
                currentRow.addView(gameCard(games.get(index)), gameParams());
            }
            int remainder = games.size() % 3;
            if (remainder > 0 && currentRow != null) {
                for (int index = remainder; index < 3; index++) {
                    currentRow.addView(new View(this), gameParams());
                }
            }
            root.addView(grid, matchWrap());
        }

        setContentView(scroll);
    }

    private List<GameCatalog.Game> enabledGames() {
        ArrayList<GameCatalog.Game> games = new ArrayList<>();
        if (store.active == null) return games;
        for (GameCatalog.Game game : GameCatalog.all()) {
            if (store.active.isGameEnabled(game.id)) games.add(game);
        }
        return games;
    }

    private View gameCard(GameCatalog.Game game) {
        boolean installed = GameLauncher.isInstalled(this, game);
        LinearLayout card = column();
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(18), dp(20), dp(18), dp(18));
        card.setBackground(rounded(installed ? game.color : DISABLED, 24));
        card.setElevation(dp(4));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription(game.name + ". " + game.description + (installed ? "" : ". Not installed yet"));
        card.setOnClickListener(view -> GameLauncher.open(this, store.active, game));

        TextView icon = text(game.icon, 40, installed ? Color.WHITE : MUTED, true);
        icon.setGravity(Gravity.CENTER);
        card.addView(icon, matchWrap());
        TextView name = text(game.name, 25, installed ? Color.WHITE : INK, true);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, dp(7), 0, 0);
        card.addView(name, matchWrap());
        TextView description = text(game.description, 16, installed ? Color.WHITE : MUTED, true);
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, dp(3), 0, 0);
        card.addView(description, matchWrap());
        if (!installed) {
            TextView status = text("Not installed yet", 13, MUTED, true);
            status.setGravity(Gravity.CENTER);
            status.setPadding(0, dp(8), 0, 0);
            card.addView(status, matchWrap());
        }
        return card;
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

    private LinearLayout.LayoutParams gameParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(205), 1f);
        params.leftMargin = dp(9);
        params.rightMargin = dp(9);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
