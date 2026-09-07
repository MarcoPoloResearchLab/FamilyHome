package com.mprlab.portal;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.List;

public final class GameLibraryActivity extends PortalActivity {
    private static final int SYSTEM_BAR = PortalStyle.INK;
    private static final int INK = PortalStyle.INK;
    private static final int MUTED = PortalStyle.SECONDARY;

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
        LinearLayout header = row();
        header.addView(text("Games", PortalStyle.TextRole.TITLE, INK), new LinearLayout.LayoutParams(0, -2, 1f));
        PortalToolbar.navigation(this, header);
        LinearLayout root = JoinedSurface.column(this);
        root.setBackgroundColor(PortalStyle.PAPER);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));
        List<GameCatalog.Game> games = GameCatalog.all();
        LinearLayout grid = JoinedSurface.column(this);
        for (int index = 0; index < games.size(); index += 2) {
            LinearLayout cells = JoinedSurface.row(this);
            cells.addView(gameCard(games.get(index)), new LinearLayout.LayoutParams(0, -1, 1f));
            cells.addView(gameCard(games.get(index + 1)), new LinearLayout.LayoutParams(0, -1, 1f));
            grid.addView(cells, new LinearLayout.LayoutParams(-1, 0, 1f));
        }
        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private View gameCard(GameCatalog.Game game) {
        boolean installed = GameLauncher.isInstalled(this, game);
        FrameLayout card = new FrameLayout(this);
        card.setPadding(dp(24), dp(16), dp(24), dp(16));
        PortalStyle.tile(card, game.color);
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription(game.name + ". " + game.description + (installed ? "" : ". Not installed yet"));
        card.setOnClickListener(view -> GameLauncher.open(this, game));

        CharacterView illustration = new CharacterView(this, game.illustration);
        // Reserve the title column; the character uses the whole remaining height.
        FrameLayout.LayoutParams art = new FrameLayout.LayoutParams(-1, -1, Gravity.RIGHT | Gravity.BOTTOM);
        art.leftMargin = dp(170);
        card.addView(illustration, art);
        TextView name = text(game.name, PortalStyle.TextRole.DISPLAY, INK);
        card.addView(name, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.LEFT));
        if (!installed) {
            TextView status = text("Not installed yet", PortalStyle.TextRole.BODY, MUTED);
            card.addView(status, new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.LEFT));
        }
        return card;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private TextView text(String value, PortalStyle.TextRole role, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        PortalStyle.text(text, role);
        text.setTextColor(color);
        return text;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
