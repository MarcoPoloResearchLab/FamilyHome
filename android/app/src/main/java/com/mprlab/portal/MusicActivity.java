package com.mprlab.portal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MusicActivity extends PortalActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.addView(label("Music", PortalStyle.TextRole.TITLE), new LinearLayout.LayoutParams(0, -2, 1));
        PortalToolbar.navigation(this, toolbar);
        LinearLayout root = JoinedSurface.column(this);
        root.setBackgroundColor(PortalStyle.PAPER);
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout instruments = JoinedSurface.row(this);
        addInstrument(instruments, "Piano", "Tap the keys. Find your melody.", PortalStyle.BLUE,
                CharacterView.Kind.PIANO, PianoActivity.class);
        addInstrument(instruments, "Guitar", "Pick a note. Strum a chord.", PortalStyle.YELLOW,
                CharacterView.Kind.GUITAR, GuitarActivity.class);
        root.addView(instruments, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void addInstrument(LinearLayout instruments, String title, String subtitle, int color,
            CharacterView.Kind illustration, Class<?> activity) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(dp(24), dp(16), dp(24), dp(24));
        PortalStyle.tile(tile, color);
        tile.addView(label(title, PortalStyle.TextRole.DISPLAY));
        TextView description = label(subtitle, PortalStyle.TextRole.BODY);
        description.setPadding(0, dp(4), 0, dp(8));
        tile.addView(description);
        tile.addView(new CharacterView(this, illustration), new LinearLayout.LayoutParams(-1, 0, 1));
        tile.setContentDescription(title);
        tile.setFocusable(true);
        tile.setOnClickListener(view -> {
            Intent intent = new Intent(this, activity);
            if (getIntent().getExtras() != null) intent.putExtras(getIntent().getExtras());
            startActivity(intent);
        });
        instruments.addView(tile, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private TextView label(String value, PortalStyle.TextRole role) {
        TextView text = new TextView(this);
        text.setText(value);
        PortalStyle.text(text, role);
        text.setTextColor(PortalStyle.INK);
        return text;
    }

    private int dp(int value) { return PortalStyle.dp(this, value); }
}
