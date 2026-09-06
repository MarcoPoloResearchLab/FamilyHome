package com.mprlab.portal;

import android.content.Context;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ScrollView;

final class PortalStyle {
    static final int PAPER = 0xfffffbef;
    static final int INK = Color.BLACK;
    static final int SECONDARY = 0xff424242;
    static final int BLUE = 0xff8dd4ff;
    static final int PURPLE = 0xffc4aaff;
    static final int CORAL = 0xffff8597;
    static final int MINT = 0xff77ddb5;
    static final int YELLOW = 0xffffd65c;
    static final int WHITE = Color.WHITE;
    enum TextRole {
        TITLE(32, true), PRIMARY(24, true), CONTROL(22, true), SECTION(22, true), BODY(20, false), DISPLAY(48, true), COUNTDOWN(76, true);
        final int sp;
        final boolean rounded;
        TextRole(int sp, boolean rounded) { this.sp = sp; this.rounded = rounded; }
    }
    static final int CONTROL_HEIGHT = 60;
    static final int PRIMARY_HEIGHT = 64;
    static void text(TextView view, TextRole role) {
        view.setTag(R.id.portal_text_role, role);
        view.setTextSize(role.sp);
        view.setAccessibilityHeading(role == TextRole.TITLE || role == TextRole.SECTION);
        view.setTypeface(role.rounded ? heading(view.getContext()) : Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }
    static void primary(Button view) {
        button(view);
        text(view, TextRole.PRIMARY);
        view.setMinHeight(dp(view.getContext(), PRIMARY_HEIGHT));
        view.setMinimumHeight(dp(view.getContext(), PRIMARY_HEIGHT));
    }
    private static Typeface heading;

    static Typeface heading(Context context) {
        if (heading == null) heading = Typeface.create(context.getResources().getFont(R.font.fredoka), 700, false);
        return heading;
    }

    static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static ScrollView scroll(Context context) {
        ScrollView scroll = new ScrollView(context);
        scroll.setDefaultFocusHighlightEnabled(false);
        return scroll;
    }

    static Drawable surface(Context context, int color, int radius) {
        return new OutlinedSurface(context, color, radius);
    }

    static void button(TextView view) {
        text(view, TextRole.CONTROL);
        view.setMinHeight(dp(view.getContext(), CONTROL_HEIGHT));
        view.setTextColor(INK);
        view.setStateListAnimator(null);
        view.setElevation(0);
    }

    static void toolbar(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            text.setTextColor(INK);
        }
        if (view instanceof Button) button((Button) view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) toolbar(group.getChildAt(i));
        }
    }

    static void dialog(AlertDialog dialog) {
        dialog.getWindow().setBackgroundDrawable(surface(dialog.getContext(), PAPER, 24));
        dialogText(dialog.getWindow().getDecorView());
    }

    private static void dialogText(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            text.setTextColor(INK);
            int titleId = view.getResources().getIdentifier("alertTitle", "id", "android");
            TextRole role = (TextRole) text.getTag(R.id.portal_text_role);
            text(text, role == null ? (text.getId() == titleId ? TextRole.TITLE : TextRole.BODY) : role);
        }
        if (view instanceof Button) {
            Button button = (Button) view;
            TextRole role = (TextRole) button.getTag(R.id.portal_text_role);
            if (role == TextRole.PRIMARY) primary(button); else button(button);
            button.setAllCaps(false);
            button.setBackground(surface(view.getContext(), YELLOW, 16));
            button.setPadding(dp(view.getContext(), 16), dp(view.getContext(), 8), dp(view.getContext(), 16), dp(view.getContext(), 8));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) dialogText(group.getChildAt(i));
        }
    }

    private static final class OutlinedSurface extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF face = new RectF();
        private final float stroke;
        private final float shadow;
        private final float radius;
        private final int color;
        private boolean pressed;
        private boolean focused;
        private boolean selected;
        private boolean enabled = true;
        private int opacity = 255;

        OutlinedSurface(Context context, int color, int radius) {
            this.color = color;
            this.radius = dp(context, radius);
            stroke = dp(context, 3);
            shadow = dp(context, 4);
        }

        @Override public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            int layer = opacity == 255 ? canvas.save()
                : canvas.saveLayerAlpha(bounds.left, bounds.top, bounds.right, bounds.bottom, opacity);
            float inset = stroke / 2;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(INK);
            face.set(bounds.left + inset + shadow, bounds.top + inset + shadow,
                    bounds.right - inset, bounds.bottom - inset);
            canvas.drawRoundRect(face, radius, radius, paint);
            float offset = pressed ? shadow : 0;
            face.set(bounds.left + inset + offset, bounds.top + inset + offset,
                    bounds.right - shadow - inset + offset, bounds.bottom - shadow - inset + offset);
            paint.setColor(enabled ? color : 0xffdeded8);
            canvas.drawRoundRect(face, radius, radius, paint);
            paint.setColor(INK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            canvas.drawRoundRect(face, radius, radius, paint);
            if (focused || selected) {
                face.inset(stroke * 2, stroke * 2);
                paint.setStrokeWidth(stroke / 2);
                canvas.drawRoundRect(face, radius * .7f, radius * .7f, paint);
            }
            canvas.restoreToCount(layer);
        }

        @Override public boolean isStateful() { return true; }
        @Override public boolean hasFocusStateSpecified() { return true; }
        @Override protected boolean onStateChange(int[] states) {
            boolean nextPressed = false, nextFocused = false, nextEnabled = false, nextSelected = false;
            for (int state : states) {
                if (state == android.R.attr.state_pressed) nextPressed = true;
                if (state == android.R.attr.state_focused) nextFocused = true;
                if (state == android.R.attr.state_enabled) nextEnabled = true;
                if (state == android.R.attr.state_selected) nextSelected = true;
            }
            if (pressed == nextPressed && focused == nextFocused && enabled == nextEnabled && selected == nextSelected) return false;
            pressed = nextPressed; focused = nextFocused; enabled = nextEnabled; selected = nextSelected;
            invalidateSelf();
            return true;
        }
        @Override public void setAlpha(int alpha) { opacity = alpha; invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    private PortalStyle() { }
}
