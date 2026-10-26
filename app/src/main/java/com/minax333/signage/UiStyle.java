package com.minax333.signage;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Small shared UI styling helper, compatible with Android API 17.
 */
public final class UiStyle {

    private UiStyle() {
    }

    public static void styleButton(Button button) {
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setPadding(18, 10, 18, 10);
        button.setBackground(makeBackground(Color.rgb(37, 99, 235), 14));
        button.setMinHeight(46);
        button.setAllCaps(false);
    }

    public static void styleSecondaryButton(Button button) {
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setPadding(16, 8, 16, 8);
        button.setBackground(makeBackground(Color.rgb(55, 65, 81), 12));
        button.setMinHeight(44);
        button.setAllCaps(false);
    }

    public static void styleOverlay(TextView textView, int textSize) {
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(textSize);
        textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textView.setShadowLayer(3, 1, 1, Color.BLACK);
        textView.setPadding(14, 8, 14, 8);
        textView.setBackground(makeBackground(Color.argb(165, 15, 23, 42), 16));
    }

    public static void stylePanel(View view) {
        view.setBackground(makeBackground(Color.rgb(17, 24, 39), 18));
    }

    public static GradientDrawable makeBackground(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusDp);
        drawable.setStroke(1, Color.argb(55, 255, 255, 255));
        return drawable;
    }
}
