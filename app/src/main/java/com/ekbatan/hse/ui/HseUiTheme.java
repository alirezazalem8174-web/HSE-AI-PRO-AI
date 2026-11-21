package com.ekbatan.hse.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.EditText;

/** Central visual language for HSE AI PRO. Keeps visual decisions out of feature logic. */
public final class HseUiTheme {
    private HseUiTheme() {}

    public static final int INK = Color.rgb(25,35,45);
    public static final int MUTED = Color.rgb(91,105,116);
    public static final int BG = Color.rgb(242,246,248);
    public static final int CARD = Color.WHITE;
    public static final int BORDER = Color.rgb(218,226,231);
    public static final int GREEN = Color.rgb(0,128,116);
    public static final int BLUE = Color.rgb(28,86,145);
    public static final int ORANGE = Color.rgb(214,126,0);
    public static final int RED = Color.rgb(184,48,54);
    public static final int TEAL = Color.rgb(13,67,72);
    public static final int NAVY = Color.rgb(16,35,51);
    public static final int CYAN = Color.rgb(0,151,167);
    public static final int GOLD = Color.rgb(214,154,35);

    public static int dp(Activity a, float value) {
        return (int)(value * a.getResources().getDisplayMetrics().density + .5f);
    }

    public static GradientDrawable box(Activity a, int fill, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(a, radiusDp));
        if (stroke != fill) g.setStroke(dp(a, 1), stroke);
        return g;
    }

    public static GradientDrawable gradient(Activity a, int start, int end, int radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        g.setCornerRadius(dp(a, radiusDp));
        return g;
    }

    public static TextView text(Activity a, String value, float sp, int color, float fontScale) {
        TextView t = new TextView(a);
        t.setText(value);
        t.setTextSize(sp * fontScale);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        t.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        t.setPadding(dp(a,4), dp(a,3), dp(a,4), dp(a,3));
        t.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return t;
    }

    public static TextView title(Activity a, String value, float sp, float fontScale) {
        TextView t = text(a, value, sp, INK, fontScale);
        t.setTypeface(Typeface.create("sans", Typeface.BOLD));
        return t;
    }

    public static EditText input(Activity a, String hint, float fontScale) {
        EditText e = new EditText(a);
        e.setHint(hint);
        e.setTextColor(INK);
        e.setHintTextColor(Color.rgb(145,155,163));
        e.setTextSize(15 * fontScale);
        e.setSingleLine(false);
        e.setPadding(dp(a,14), dp(a,8), dp(a,14), dp(a,8));
        e.setGravity(Gravity.RIGHT | Gravity.TOP);
        e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        e.setBackground(box(a, Color.WHITE, BORDER, 10));
        return e;
    }

    public static Button action(Activity a, String label, int background, float fontScale) {
        Button b = new Button(a);
        b.setText(label);
        b.setTextSize(14 * fontScale);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setMinHeight(dp(a,48));
        b.setPadding(dp(a,12),0,dp(a,12),0);
        b.setBackground(box(a, background, background, 12));
        return b;
    }

    public static Button outline(Activity a, String label, float fontScale) {
        Button b = new Button(a);
        b.setText(label);
        b.setTextSize(13 * fontScale);
        b.setTextColor(INK);
        b.setAllCaps(false);
        b.setMinHeight(dp(a,44));
        b.setPadding(dp(a,10),0,dp(a,10),0);
        b.setBackground(box(a, Color.WHITE, BORDER, 10));
        return b;
    }

    public static TextView badge(Activity a, String value, int background, float fontScale) {
        TextView t = text(a, value, 11, Color.WHITE, fontScale);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(dp(a,10),dp(a,5),dp(a,10),dp(a,5));
        t.setBackground(box(a, background, background, 30));
        return t;
    }

    public static LinearLayout card(Activity a) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(a,16),dp(a,15),dp(a,16),dp(a,15));
        c.setBackground(box(a, CARD, BORDER, 18));
        c.setElevation(dp(a,3));
        c.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));
        return c;
    }

    public static void addGap(Activity a, LinearLayout parent, int heightDp) {
        Space s = new Space(a);
        parent.addView(s, new LinearLayout.LayoutParams(1, dp(a,heightDp)));
    }
}
