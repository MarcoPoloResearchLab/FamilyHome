package com.mprlab.portal;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;

/** Adjoining surfaces share a single black divider. */
final class JoinedSurface {
    private JoinedSurface() {}

    static LinearLayout row(Context context) { return cells(context, LinearLayout.HORIZONTAL); }
    static LinearLayout column(Context context) { return cells(context, LinearLayout.VERTICAL); }

    private static LinearLayout cells(Context context, int orientation) {
        LinearLayout cells = new LinearLayout(context);
        cells.setOrientation(orientation);
        GradientDrawable divider = new GradientDrawable();
        divider.setColor(PortalStyle.INK);
        int width = PortalStyle.dp(context, PortalStyle.OUTLINE_DP);
        divider.setSize(width, width);
        cells.setDividerDrawable(divider);
        cells.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        return cells;
    }
}
