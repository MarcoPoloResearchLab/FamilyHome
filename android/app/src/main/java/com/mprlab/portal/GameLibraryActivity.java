package com.mprlab.portal;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public final class GameLibraryActivity extends PortalActivity {
    private static final int BG = Color.rgb(255, 248, 234);
    private static final int SYSTEM_BAR = Color.rgb(36, 49, 71);
    private static final int INK = Color.rgb(36, 49, 71);
    private static final int MUTED = Color.rgb(92, 104, 124);
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
        root.setPadding(dp(38), dp(4), dp(38), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        LinearLayout header = row();
        header.addView(text("Games", 27, INK, true), new LinearLayout.LayoutParams(0, -2, 1f));

        List<GameCatalog.Game> games = GameCatalog.all();
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

        setContentView(PortalToolbar.screen(this, header, scroll, BG));
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
