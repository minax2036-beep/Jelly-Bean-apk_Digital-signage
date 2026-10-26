package com.minax333.signage;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/** Music library and compact now-playing controls for Android 4.2.2 (API 17). */
public final class MusicPanel {
    public interface Listener {
        void onBack();
        void onSelectTrack(int index);
        void onPlayPause();
        void onPrevious();
        void onNext();
        void onSeekStart();
        void onSeekChanged(int progress, boolean fromUser);
        void onSeekStop(int progress);
    }

    private final Activity activity;
    private final Listener listener;
    private final LinearLayout panel;
    public final LinearLayout listLayout;
    public final TextView nowPlayingText;
    public final TextView emptyText;
    private final TextView artwork;
    private final TextView trackTitle;
    private final TextView playbackState;
    private final TextView positionText;
    private final TextView durationText;
    private final TextView playPauseButton;
    private final SeekBar seekBar;

    public MusicPanel(Activity activity, final Listener listener) {
        this.activity = activity;
        this.listener = listener;
        panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(12), dp(16), dp(12));
        panel.setBackgroundColor(Color.rgb(17, 24, 39));
        panel.setVisibility(View.GONE);

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button backButton = new Button(activity);
        backButton.setText("戻る");
        UiStyle.styleSecondaryButton(backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { MusicPanel.this.listener.onBack(); }
        });
        header.addView(backButton, wrapContentParams());

        TextView title = new TextView(activity);
        title.setText("音楽ライブラリ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        panel.addView(header, matchWrapParams());

        nowPlayingText = new TextView(activity);
        nowPlayingText.setTextColor(Color.LTGRAY);
        nowPlayingText.setTextSize(13);
        nowPlayingText.setPadding(dp(10), dp(7), dp(10), dp(7));
        panel.addView(nowPlayingText, matchWrapParams());

        TextView listTitle = new TextView(activity);
        listTitle.setText("  PLAYLIST");
        listTitle.setTextColor(Color.rgb(120, 190, 255));
        listTitle.setTextSize(12);
        listTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        listTitle.setPadding(dp(4), dp(7), dp(4), dp(5));
        panel.addView(listTitle, matchWrapParams());

        ScrollView scrollView = new ScrollView(activity);
        listLayout = new LinearLayout(activity);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listLayout);
        panel.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        emptyText = new TextView(activity);
        emptyText.setTextColor(Color.LTGRAY);
        emptyText.setTextSize(15);
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setText("音楽ファイルがありません。\n/sdcard/signage/audio/ に音楽を入れてください。");
        emptyText.setPadding(dp(12), dp(20), dp(12), dp(20));
        panel.addView(emptyText, matchWrapParams());

        LinearLayout player = new LinearLayout(activity);
        player.setOrientation(LinearLayout.HORIZONTAL);
        player.setGravity(Gravity.CENTER_VERTICAL);
        player.setPadding(dp(10), dp(9), dp(10), dp(9));
        player.setBackground(background(Color.rgb(31, 41, 55), dp(16)));

        artwork = new TextView(activity);
        artwork.setText("♫");
        artwork.setTextColor(Color.WHITE);
        artwork.setTextSize(34);
        artwork.setGravity(Gravity.CENTER);
        artwork.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        artwork.setBackground(background(Color.rgb(37, 99, 235), dp(14)));
        player.addView(artwork, new LinearLayout.LayoutParams(dp(64), dp(64)));

        LinearLayout info = new LinearLayout(activity);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(10), 0, dp(6), 0);
        trackTitle = new TextView(activity);
        trackTitle.setText("曲を選択してください");
        trackTitle.setTextColor(Color.WHITE);
        trackTitle.setTextSize(15);
        trackTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        trackTitle.setMaxLines(2);
        trackTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(trackTitle, matchWrapParams());

        playbackState = new TextView(activity);
        playbackState.setText("停止中");
        playbackState.setTextColor(Color.rgb(150, 220, 255));
        playbackState.setTextSize(12);
        info.addView(playbackState, matchWrapParams());

        LinearLayout timeline = new LinearLayout(activity);
        timeline.setGravity(Gravity.CENTER_VERTICAL);
        positionText = new TextView(activity);
        positionText.setText("00:00");
        positionText.setTextColor(Color.LTGRAY);
        positionText.setTextSize(10);
        timeline.addView(positionText, new LinearLayout.LayoutParams(dp(38), ViewGroup.LayoutParams.WRAP_CONTENT));
        seekBar = new SeekBar(activity);
        seekBar.setMax(1000);
        timeline.addView(seekBar, new LinearLayout.LayoutParams(0, dp(28), 1));
        durationText = new TextView(activity);
        durationText.setText("00:00");
        durationText.setTextColor(Color.LTGRAY);
        durationText.setTextSize(10);
        durationText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        timeline.addView(durationText, new LinearLayout.LayoutParams(dp(38), ViewGroup.LayoutParams.WRAP_CONTENT));
        info.addView(timeline, matchWrapParams());
        player.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        panel.addView(player, matchWrapParams());

        LinearLayout controls = new LinearLayout(activity);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(0, dp(5), 0, 0);
        Button previous = controlButton("|◀");
        Button next = controlButton("▶|");
        playPauseButton = new TextView(activity);
        playPauseButton.setText("▶");
        playPauseButton.setTextColor(Color.WHITE);
        playPauseButton.setTextSize(22);
        playPauseButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        playPauseButton.setGravity(Gravity.CENTER);
        playPauseButton.setBackground(background(Color.rgb(37, 99, 235), dp(24)));
        playPauseButton.setClickable(true);
        playPauseButton.setFocusable(true);
        LinearLayout.LayoutParams side = new LinearLayout.LayoutParams(dp(72), dp(42));
        side.setMargins(dp(6), 0, dp(6), 0);
        controls.addView(previous, side);
        LinearLayout.LayoutParams center = new LinearLayout.LayoutParams(dp(90), dp(44));
        center.setMargins(dp(8), 0, dp(8), 0);
        controls.addView(playPauseButton, center);
        controls.addView(next, side);
        panel.addView(controls, matchWrapParams());

        previous.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { listener.onPrevious(); }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { listener.onNext(); }
        });
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { listener.onPlayPause(); }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                listener.onSeekChanged(progress, fromUser);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { listener.onSeekStart(); }
            @Override public void onStopTrackingTouch(SeekBar bar) { listener.onSeekStop(bar.getProgress()); }
        });
    }

    public View getView() { return panel; }

    public void setTrack(String title, boolean playing) {
        trackTitle.setText(title == null || title.length() == 0 ? "曲を選択してください" : title);
        playbackState.setText(playing ? "再生中" : "一時停止／停止中");
        playPauseButton.setText(playing ? "Ⅱ" : "▶");
        artwork.setText(playing ? "♫" : "♪");
    }

    public void setProgress(int progress) { seekBar.setProgress(Math.max(0, Math.min(1000, progress))); }

    public void setPositionText(String position, String duration) {
        positionText.setText(position);
        durationText.setText(duration);
    }

    private Button controlButton(String label) {
        Button button = new Button(activity);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setPadding(dp(4), 0, dp(4), 0);
        button.setBackground(background(Color.rgb(45, 55, 72), dp(12)));
        return button;
    }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(1, Color.argb(45, 255, 255, 255));
        return drawable;
    }

    private LinearLayout.LayoutParams wrapContentParams() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchWrapParams() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        float density = activity.getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }
}
