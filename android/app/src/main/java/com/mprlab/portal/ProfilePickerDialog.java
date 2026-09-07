package com.mprlab.portal;

import android.app.Activity;
import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;
import java.util.function.Consumer;

final class ProfilePickerDialog extends Dialog {
    private static final int[] COLORS = {PortalStyle.PURPLE, PortalStyle.CORAL,
        PortalStyle.BLUE, PortalStyle.MINT, PortalStyle.YELLOW};
    private static final CharacterView.Kind[] CHARACTERS = {CharacterView.Kind.MATCH,
        CharacterView.Kind.PENCIL, CharacterView.Kind.BOOK, CharacterView.Kind.GAME,
        CharacterView.Kind.MUSIC, CharacterView.Kind.CAMERA};

    ProfilePickerDialog(Activity activity, List<ProfileStore.Profile> profiles,
            ProfileStore.Profile active, Consumer<ProfileStore.Profile> select) {
        super(activity, android.R.style.Theme_Material_Light_Dialog_Alert);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        window.setBackgroundDrawable(PortalStyle.surface(activity, PortalStyle.PAPER, 24));
        window.setTitle("Whose turn is it?");

        int width = Math.min(dp(960), activity.getResources().getDisplayMetrics().widthPixels - dp(48));
        int contentWidth = width - dp(40);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView title = label("Whose turn is it?", PortalStyle.TextRole.TITLE);
        title.setPadding(dp(4), 0, 0, dp(16));
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout grid = JoinedSurface.column(activity);
        int columns = contentWidth >= dp(640) && profiles.size() > 1 ? 2 : 1;
        for (int index = 0; index < profiles.size(); index += columns) {
            LinearLayout row = JoinedSurface.row(activity);
            for (int column = 0; column < columns; column++) {
                int position = index + column;
                if (position < profiles.size()) {
                    ProfileStore.Profile profile = profiles.get(position);
                    View choice = choice(profile, active != null && active.id.equals(profile.id), position);
                    choice.setOnClickListener(view -> { dismiss(); select.accept(profile); });
                    row.addView(choice, new LinearLayout.LayoutParams(0, -1, 1));
                } else {
                    View space = new View(activity);
                    space.setBackgroundColor(PortalStyle.PAPER);
                    row.addView(space, new LinearLayout.LayoutParams(0, -1, 1));
                }
            }
            grid.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        ScrollView scroll = PortalStyle.scroll(activity);
        scroll.setBackgroundColor(PortalStyle.INK);
        scroll.setPadding(dp(3), dp(3), dp(3), dp(3));
        scroll.setClipToPadding(true);
        scroll.addView(grid);

        Button close = new Button(activity);
        close.setText("Close");
        close.setAllCaps(false);
        PortalStyle.button(close);
        close.setBackground(PortalStyle.surface(activity, PortalStyle.YELLOW, 16));
        close.setOnClickListener(view -> dismiss());
        int exactWidth = View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY);
        int unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        title.measure(exactWidth, unspecified);
        close.measure(exactWidth, unspecified);
        scroll.measure(exactWidth, unspecified);
        int availableHeight = activity.getResources().getDisplayMetrics().heightPixels
            - dp(100) - title.getMeasuredHeight() - close.getMeasuredHeight();
        root.addView(scroll, new LinearLayout.LayoutParams(-1, Math.min(scroll.getMeasuredHeight(), availableHeight)));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(-1, -2);
        closeParams.topMargin = dp(16);
        root.addView(close, closeParams);
        setContentView(root);
        window.setLayout(width, -2);
    }

    private View choice(ProfileStore.Profile profile, boolean selected, int position) {
        LinearLayout tile = new LinearLayout(getContext());
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setMinimumHeight(dp(176));
        tile.setPadding(dp(16), dp(16), dp(16), dp(16));
        tile.setFocusable(true);
        tile.setContentDescription("Choose " + profile.name);
        tile.setSelected(selected);
        PortalStyle.tile(tile, COLORS[position % COLORS.length]);
        CharacterView character = new CharacterView(getContext(), CHARACTERS[position % CHARACTERS.length]);
        tile.addView(character, new LinearLayout.LayoutParams(dp(112), dp(112)));
        LinearLayout words = new LinearLayout(getContext());
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(12), 0, 0, 0);
        words.addView(label(profile.name, PortalStyle.TextRole.PRIMARY));
        TextView status = label(selected ? "✓ Playing now" : "Let's play!", PortalStyle.TextRole.BODY);
        status.setPadding(0, dp(8), 0, 0);
        words.addView(status);
        tile.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        return tile;
    }

    private TextView label(String value, PortalStyle.TextRole role) {
        TextView text = new TextView(getContext());
        text.setText(value);
        text.setTextColor(PortalStyle.INK);
        PortalStyle.text(text, role);
        return text;
    }

    private int dp(int value) { return PortalStyle.dp(getContext(), value); }
}
