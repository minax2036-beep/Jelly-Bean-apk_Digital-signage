package com.minax333.signage;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Lightweight video controls designed for Android 4.2.2 (API 17).
 */
public final class VideoControlsPanel {

    public interface Listener {
        void onPlayPause();
        void onPrevious();
        void onNext();
        void onSeekStart();
        void onSeekChanged(int progress, boolean fromUser);
        void onSeekStop(int progress);
    }

    private final FrameLayout root;
    private final TextView titleText;
    private final TextView positionText;
    private final TextView durationText;
    private final TextView playPauseButton;
    private final SeekBar seekBar;

    public VideoControlsPanel(Activity activity, final Listener listener) {
        root = new FrameLayout(activity);
        root.setFocusable(false);

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(activity, 18), dp(activity, 12), dp(activity, 18), dp(activity, 12));
        header.setBackground(background(Color.argb(205, 8, 15, 28), dp(activity, 16)));

        TextView eyebrow = text(activity, "NOW PLAYING", 10, Color.rgb(120, 190, 255), true);
        header.addView(eyebrow, matchWrap());

        titleText = text(activity, "動画を読み込み中…", 17, Color.WHITE, true);
        titleText.setMaxLines(1);
        titleText.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        titleText.setPadding(0, dp(activity, 3), 0, 0);
        header.addView(titleText, matchWrap());

        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
                dp(activity, 360), ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        headerParams.setMargins(dp(activity, 12), dp(activity, 12), dp(activity, 12), 0);
        root.addView(header, headerParams);

        LinearLayout controls = new LinearLayout(activity);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(dp(activity, 16), dp(activity, 10), dp(activity, 16), dp(activity, 12));
        controls.setBackground(background(Color.argb(220, 8, 15, 28), dp(activity, 18)));

        LinearLayout timeline = new LinearLayout(activity);
        timeline.setOrientation(LinearLayout.HORIZONTAL);
        timeline.setGravity(Gravity.CENTER_VERTICAL);

        positionText = text(activity, "00:00", 12, Color.WHITE, true);
        positionText.setGravity(Gravity.CENTER);
        timeline.addView(positionText, new LinearLayout.LayoutParams(dp(activity, 48), ViewGroup.LayoutParams.WRAP_CONTENT));

        seekBar = new SeekBar(activity);
        seekBar.setMax(1000);
        seekBar.setProgress(0);
        timeline.addView(seekBar, new LinearLayout.LayoutParams(0, dp(activity, 32), 1));

        durationText = text(activity, "00:00", 12, Color.LTGRAY, false);
        durationText.setGravity(Gravity.CENTER);
        timeline.addView(durationText, new LinearLayout.LayoutParams(dp(activity, 48), ViewGroup.LayoutParams.WRAP_CONTENT));
        controls.addView(timeline, matchWrap());

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        buttons.setPadding(0, dp(activity, 4), 0, 0);

        Button previous = button(activity, "|◀");
        Button next = button(activity, "▶|");
        playPauseButton = text(activity, "▶", 22, Color.WHITE, true);
        playPauseButton.setGravity(Gravity.CENTER);
        playPauseButton.setBackground(background(Color.rgb(37, 99, 235), dp(activity, 28)));
        playPauseButton.setPadding(dp(activity, 22), dp(activity, 9), dp(activity, 22), dp(activity, 9));
        playPauseButton.setClickable(true);
        playPauseButton.setFocusable(true);

        LinearLayout.LayoutParams sideParams = new LinearLayout.LayoutParams(dp(activity, 66), dp(activity, 44));
        sideParams.setMargins(dp(activity, 6), dp(activity, 3), dp(activity, 6), 0);
        buttons.addView(previous, sideParams);
        LinearLayout.LayoutParams mainParams = new LinearLayout.LayoutParams(dp(activity, 90), dp(activity, 48));
        mainParams.setMargins(dp(activity, 8), dp(activity, 3), dp(activity, 8), 0);
        buttons.addView(playPauseButton, mainParams);
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(dp(activity, 66), dp(activity, 44));
        nextParams.setMargins(dp(activity, 6), dp(activity, 3), dp(activity, 6), 0);
        buttons.addView(next, nextParams);
        controls.addView(buttons, matchWrap());

        FrameLayout.LayoutParams controlsParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        controlsParams.setMargins(dp(activity, 18), 0, dp(activity, 18), dp(activity, 18));
        root.addView(controls, controlsParams);

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
            @Override public void onStartTrackingTouch(SeekBar bar) {
                listener.onSeekStart();
            }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                listener.onSeekStop(bar.getProgress());
            }
        });
    }

    public View getView() {
        return root;
    }

    public void setTitle(String title) {
        titleText.setText(title == null ? "動画" : title);
    }

    public void setPlayPauseText(String text) {
        playPauseButton.setText(text);
    }

    public void setProgress(int progress) {
        seekBar.setProgress(Math.max(0, Math.min(1000, progress)));
    }

    public void setPositionText(String position, String duration) {
        positionText.setText(position);
        durationText.setText(duration);
    }

    private Button button(Activity activity, String label) {
        Button button = new Button(activity);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setPadding(dp(activity, 4), 0, dp(activity, 4), 0);
        button.setBackground(background(Color.rgb(45, 55, 72), dp(activity, 14)));
        return button;
    }

    private TextView text(Activity activity, String value, int size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(1, Color.argb(45, 255, 255, 255));
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static int dp(Activity activity, int value) {
        float density = activity.getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }
}
