package com.minax333.signage;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Dedicated launcher-style home screen for Jelly-Bean Digital Signage.
 * All UI is built in Java to keep compatibility with Android API 17.
 */
public final class Home {

    public interface Listener {
        void onVideoSelected();
        void onMusicSelected();
        void onImagesSelected();
        void onSettingsSelected();
    }

    private final Activity activity;
    private final FrameLayout root;
    private final TextView clockText;
    private final TextView dateText;

    public Home(Activity activity, final Listener listener) {
        this.activity = activity;

        root = new FrameLayout(activity);
        root.setFocusable(true);
        root.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] {
                        Color.rgb(7, 35, 78),
                        Color.rgb(15, 91, 153),
                        Color.rgb(18, 34, 67)
                }
        ));

        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(28), dp(22), dp(28), dp(18));

        FrameLayout.LayoutParams pageParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        root.addView(page, pageParams);

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout brand = new LinearLayout(activity);
        brand.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(activity);
        title.setText("Jelly-Bean Digital Signage");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brand.addView(title, wrapParams());

        TextView subtitle = new TextView(activity);
        subtitle.setText("つながる映像、ひろがる情報");
        subtitle.setTextColor(Color.rgb(213, 231, 255));
        subtitle.setTextSize(13);
        subtitle.setPadding(0, dp(3), 0, 0);
        brand.addView(subtitle, wrapParams());

        header.addView(brand, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout dateBlock = new LinearLayout(activity);
        dateBlock.setOrientation(LinearLayout.VERTICAL);
        dateBlock.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

        clockText = new TextView(activity);
        clockText.setTextColor(Color.WHITE);
        clockText.setTextSize(36);
        clockText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        clockText.setGravity(Gravity.RIGHT);
        dateBlock.addView(clockText, wrapParams());

        dateText = new TextView(activity);
        dateText.setTextColor(Color.rgb(220, 235, 255));
        dateText.setTextSize(15);
        dateText.setGravity(Gravity.RIGHT);
        dateBlock.addView(dateText, wrapParams());

        header.addView(dateBlock, wrapParams());
        page.addView(header, wrapParams());

        TextView sectionTitle = new TextView(activity);
        sectionTitle.setText("ホーム");
        sectionTitle.setTextColor(Color.WHITE);
        sectionTitle.setTextSize(22);
        sectionTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sectionTitle.setPadding(0, dp(22), 0, dp(10));
        page.addView(sectionTitle, wrapParams());

        LinearLayout tiles = new LinearLayout(activity);
        tiles.setOrientation(LinearLayout.HORIZONTAL);
        tiles.setGravity(Gravity.CENTER);
        tiles.setWeightSum(4);

        tiles.addView(createTile(
                "▶", "動画", "動画を再生します",
                Color.rgb(25, 116, 232),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        listener.onVideoSelected();
                    }
                }), tileParams());

        tiles.addView(createTile(
                "♫", "音楽", "音楽ライブラリを開く",
                Color.rgb(145, 49, 190),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        listener.onMusicSelected();
                    }
                }), tileParams());

        tiles.addView(createTile(
                "▧", "画像", "画像を表示します",
                Color.rgb(0, 143, 132),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        listener.onImagesSelected();
                    }
                }), tileParams());

        tiles.addView(createTile(
                "⚙", "設定", "表示や再生を設定",
                Color.rgb(204, 113, 16),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        listener.onSettingsSelected();
                    }
                }), tileParams());

        LinearLayout.LayoutParams tilesParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        page.addView(tiles, tilesParams);

        TextView footer = new TextView(activity);
        footer.setText("Jelly-Bean Signage  •  コンテンツを選択してください");
        footer.setTextColor(Color.rgb(211, 228, 249));
        footer.setTextSize(13);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(dp(14), dp(10), dp(14), dp(10));
        footer.setBackground(UiStyle.makeBackground(Color.argb(115, 4, 17, 39), 14));
        page.addView(footer, wrapParams());
    }

    public View getView() {
        return root;
    }

    public void updateClock(String time, String date) {
        clockText.setText(time);
        dateText.setText(date);
    }

    private View createTile(
            String icon,
            String title,
            String description,
            int color,
            View.OnClickListener listener) {
        LinearLayout tile = new LinearLayout(activity);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(8), dp(12), dp(8), dp(12));
        tile.setBackground(UiStyle.makeBackground(color, 22));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setOnClickListener(listener);

        TextView iconText = new TextView(activity);
        iconText.setText(icon);
        iconText.setTextColor(Color.WHITE);
        iconText.setTextSize(38);
        iconText.setGravity(Gravity.CENTER);
        iconText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tile.addView(iconText, wrapParams());

        TextView titleText = new TextView(activity);
        titleText.setText(title);
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(22);
        titleText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(0, dp(8), 0, dp(4));
        tile.addView(titleText, wrapParams());

        TextView descriptionText = new TextView(activity);
        descriptionText.setText(description);
        descriptionText.setTextColor(Color.rgb(242, 247, 255));
        descriptionText.setTextSize(12);
        descriptionText.setGravity(Gravity.CENTER);
        tile.addView(descriptionText, wrapParams());

        return tile;
    }

    private LinearLayout.LayoutParams tileParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, dp(180), 1);
        params.setMargins(dp(5), 0, dp(5), 0);
        return params;
    }

    private LinearLayout.LayoutParams wrapParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        float density = activity.getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }
}
