package com.mprlab.portal;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

final class PortalToolbar {
    static final int HEIGHT_DP = 72;

    static void navigation(PortalActivity activity, LinearLayout toolbar) {
        toolbar.setContentDescription("Screen toolbar");
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(activity, 14), dp(activity, 8), dp(activity, 14), dp(activity, 8));
        toolbar.addView(button(activity, "Back", R.drawable.ic_nav_back, view -> activity.onBackPressed()), 0);
        toolbar.addView(button(activity, "Home", R.drawable.ic_nav_home, view -> activity.openHome()), 1);
    }

    static View screen(PortalActivity activity, LinearLayout toolbar, View content, int color) {
        navigation(activity, toolbar);
        LinearLayout screen = new LinearLayout(activity);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(color);
        screen.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(activity, HEIGHT_DP)));
        screen.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        return screen;
    }

    private static ImageButton button(PortalActivity activity, String label, int icon, View.OnClickListener action) {
        ImageButton button = new ImageButton(activity);
        button.setContentDescription(label);
        button.setImageResource(icon);
        button.setColorFilter(Color.rgb(36, 49, 71));
        button.setPadding(dp(activity, 14), dp(activity, 14), dp(activity, 14), dp(activity, 14));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.rgb(241, 237, 255));
        shape.setCornerRadius(dp(activity, 16));
        button.setBackground(shape);
        button.setOnClickListener(action);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(activity, 56), dp(activity, 56));
        params.rightMargin = dp(activity, 10);
        button.setLayoutParams(params);
        return button;
    }

    private static int dp(PortalActivity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private PortalToolbar() { }
}
