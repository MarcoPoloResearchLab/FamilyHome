package com.mprlab.portal;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public final class GameLibraryActivity extends PortalActivity {
    private static final int BG = PortalStyle.PAPER;
    private static final int SYSTEM_BAR = PortalStyle.INK;
    private static final int INK = PortalStyle.INK;
    private static final int MUTED = PortalStyle.SECONDARY;
    private static final int DISABLED = Color.rgb(217, 220, 228);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setStatusBarColor(SYSTEM_BAR);
        window.setNavigationBarColor(SYSTEM_BAR);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        ScrollView scroll = PortalStyle.scroll(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout root = column();
        root.setPadding(dp(38), dp(4), dp(38), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        LinearLayout header = row();
        header.addView(text("Games", PortalStyle.TextRole.TITLE, INK), new LinearLayout.LayoutParams(0, -2, 1f));

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
        card.setElevation(0);
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription(game.name + ". " + game.description + (installed ? "" : ". Not installed yet"));
        card.setOnClickListener(view -> GameLauncher.open(this, game));

        CharacterView illustration = new CharacterView(this, game.illustration);
        card.addView(illustration, new LinearLayout.LayoutParams(dp(160), dp(160)));
        TextView name = text(game.name, PortalStyle.TextRole.SECTION, INK);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, dp(7), 0, 0);
        card.addView(name, matchWrap());
        TextView description = text(game.description, PortalStyle.TextRole.BODY, INK);
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, dp(3), 0, 0);
        card.addView(description, matchWrap());
        if (!installed) {
            TextView status = text("Not installed yet", PortalStyle.TextRole.BODY, MUTED);
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

    private TextView text(String value, PortalStyle.TextRole role, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        PortalStyle.text(text, role);
        text.setTextColor(color);
        return text;
    }

    private android.graphics.drawable.Drawable rounded(int color, int radius) { return PortalStyle.surface(this, color, radius); }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams gameParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
        params.leftMargin = dp(9);
        params.rightMargin = dp(9);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
