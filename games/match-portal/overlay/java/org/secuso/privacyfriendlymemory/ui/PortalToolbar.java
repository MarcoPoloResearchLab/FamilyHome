package org.secuso.privacyfriendlymemory.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.widget.Button;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import org.secuso.privacyfriendlymemory.R;

public final class PortalToolbar {
    public static void install(Activity activity, Toolbar toolbar, DrawerLayout drawer) {
        LinearLayout row = row(activity);
        View status = toolbar.getChildCount() == 0 ? title(activity, "Match") : toolbar.getChildAt(0);
        toolbar.removeAllViews();
        styleContent(status);
        navigation(activity, row);
        ImageButton menu = button(activity, "Game menu", R.drawable.portal_menu,
                view -> drawer.openDrawer(GravityCompat.START));
        row.addView(menu);
        row.addView(status, new LinearLayout.LayoutParams(0, -2, 1f));
        toolbar.setBackgroundColor(PortalStyle.PAPER);
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.addView(row, new Toolbar.LayoutParams(-1, -1));
        fullscreen(activity);
    }

    public static void install(Activity activity, ActionBar bar) {
        bar.setBackgroundDrawable(new ColorDrawable(PortalStyle.PAPER));
        LinearLayout row = row(activity);
        navigation(activity, row);
        row.addView(title(activity, bar.getTitle()), new LinearLayout.LayoutParams(0, -2, 1f));
        bar.setDisplayHomeAsUpEnabled(false);
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayShowCustomEnabled(true);
        bar.setCustomView(row, new ActionBar.LayoutParams(-1, -1));
        fullscreen(activity);
    }

    public static void fullscreen(Activity activity) {
        activity.getWindow().getDecorView().post(() -> styleContent(activity.getWindow().getDecorView()));
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private static void styleContent(View view) {
        if ("Screen toolbar".equals(view.getContentDescription())) return;
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            text.setTextColor(PortalStyle.INK);
            PortalStyle.text(text, PortalStyle.TextRole.SECTION);
        }
        if (view instanceof Button) {
            PortalStyle.primary((Button) view);
            view.setBackground(PortalStyle.surface(view.getContext(), PortalStyle.MINT, 16));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) styleContent(group.getChildAt(i));
        }
    }

    private static void navigation(Activity activity, LinearLayout row) {
        row.addView(button(activity, "Back", R.drawable.portal_back, view -> activity.onBackPressed()));
        row.addView(button(activity, "Home", R.drawable.portal_home, view -> {
            Intent home = new Intent(Intent.ACTION_MAIN)
                    .setClassName("com.mprlab.portal", "com.mprlab.portal.MainActivity");
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(home);
        }));
    }

    private static LinearLayout row(Activity activity) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(PortalStyle.PAPER);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setContentDescription("Screen toolbar");
        row.setPadding(dp(activity, 8), 0, dp(activity, 8), 0);
        return row;
    }

    private static TextView title(Activity activity, CharSequence text) {
        TextView title = new TextView(activity);
        title.setText(text);
        PortalStyle.text(title, PortalStyle.TextRole.TITLE);
        title.setTextColor(PortalStyle.INK);
        title.setSingleLine(true);
        title.setPadding(dp(activity, 12), 0, 0, 0);
        return title;
    }

    private static ImageButton button(Activity activity, String label, int icon, View.OnClickListener action) {
        ImageButton button = new ImageButton(activity);
        button.setContentDescription(label);
        button.setImageResource(icon);
        button.setColorFilter(PortalStyle.INK);
        button.setBackground(PortalStyle.surface(activity, PortalStyle.YELLOW, 16));
        button.setPadding(dp(activity, 14), dp(activity, 14), dp(activity, 14), dp(activity, 14));
        button.setOnClickListener(action);
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, PortalStyle.CONTROL_HEIGHT), dp(activity, PortalStyle.CONTROL_HEIGHT)));
        return button;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private PortalToolbar() { }
}
