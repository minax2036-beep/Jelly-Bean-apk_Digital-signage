package com.minax333.signage;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Builds the settings UI. Settings behavior is delegated to MainActivity.
 */
public final class SettingsPanel {

    public interface Listener {
        void onToggleSlideshow();
        void onToggleDisplayMode();
        void onChangeInterval();
        void onToggleAudio();
        void onClose();
    }

    private final LinearLayout panel;
    public final TextView displayModeStatusText;
    public final TextView slideshowStatusText;
    public final TextView intervalStatusText;
    public final TextView audioStatusText;

    public SettingsPanel(Activity activity, final Listener listener) {
        panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(20, 18, 20, 18);
        panel.setBackgroundColor(Color.rgb(17, 24, 39));
        panel.setVisibility(View.GONE);

        ScrollView scrollView = new ScrollView(activity);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(activity);
        title.setText("Signage 設定");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 8, 0, 24);
        content.addView(title, matchWrapParams());

        TextView displayModeTitle = new TextView(activity);
        displayModeTitle.setText("背景表示モード");
        displayModeTitle.setTextColor(Color.WHITE);
        displayModeTitle.setTextSize(20);
        displayModeTitle.setPadding(0, 8, 0, 0);
        content.addView(displayModeTitle);

        displayModeStatusText = new TextView(activity);
        displayModeStatusText.setTextColor(Color.LTGRAY);
        displayModeStatusText.setTextSize(16);
        displayModeStatusText.setPadding(0, 8, 0, 8);
        content.addView(displayModeStatusText);

        Button displayModeButton = new Button(activity);
        displayModeButton.setText("画像／動画を切り替え");
        displayModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onToggleDisplayMode();
            }
        });
        UiStyle.styleButton(displayModeButton);
        content.addView(displayModeButton, buttonParams());

        TextView slideshowTitle = new TextView(activity);
        slideshowTitle.setText("画像スライドショー");
        slideshowTitle.setTextColor(Color.WHITE);
        slideshowTitle.setTextSize(20);
        content.addView(slideshowTitle);

        slideshowStatusText = new TextView(activity);
        slideshowStatusText.setTextColor(Color.LTGRAY);
        slideshowStatusText.setTextSize(16);
        slideshowStatusText.setPadding(0, 8, 0, 8);
        content.addView(slideshowStatusText);

        Button toggleButton = new Button(activity);
        toggleButton.setText("スライドショー オン／オフ");
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onToggleSlideshow();
            }
        });
        UiStyle.styleButton(toggleButton);
        content.addView(toggleButton, buttonParams());

        intervalStatusText = new TextView(activity);
        intervalStatusText.setTextColor(Color.LTGRAY);
        intervalStatusText.setTextSize(16);
        intervalStatusText.setPadding(0, 20, 0, 8);
        content.addView(intervalStatusText);

        Button intervalButton = new Button(activity);
        intervalButton.setText("切り替え秒数を変更");
        intervalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onChangeInterval();
            }
        });
        UiStyle.styleButton(intervalButton);
        content.addView(intervalButton, buttonParams());

        TextView audioTitle = new TextView(activity);
        audioTitle.setText("\nバックグラウンド音楽");
        audioTitle.setTextColor(Color.WHITE);
        audioTitle.setTextSize(20);
        content.addView(audioTitle);

        audioStatusText = new TextView(activity);
        audioStatusText.setTextColor(Color.LTGRAY);
        audioStatusText.setTextSize(16);
        audioStatusText.setPadding(0, 8, 0, 8);
        content.addView(audioStatusText);

        Button audioToggleButton = new Button(activity);
        audioToggleButton.setText("BGM オン／オフ");
        audioToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onToggleAudio();
            }
        });
        UiStyle.styleButton(audioToggleButton);
        content.addView(audioToggleButton, buttonParams());

        TextView playbackInfo = new TextView(activity);
        playbackInfo.setText("\nBGMの動作\n"
                + "・ホームや画像／時計画面で再生\n"
                + "・動画再生中は自動停止\n"
                + "・音楽ライブラリの曲再生とは別設定");
        playbackInfo.setTextColor(Color.LTGRAY);
        playbackInfo.setTextSize(16);
        playbackInfo.setPadding(0, 12, 0, 16);
        content.addView(playbackInfo, matchWrapParams());

        TextView futureTitle = new TextView(activity);
        futureTitle.setText("\\nメディアフォルダー\\n"
                + "画像：signage/images/\\n"
                + "動画：signage/videos/\\n"
                + "動画は端末が対応する形式で保存してください。");
        futureTitle.setTextColor(Color.LTGRAY);
        futureTitle.setTextSize(17);
        futureTitle.setPadding(0, 16, 0, 16);
        content.addView(futureTitle, matchWrapParams());

        Button closeButton = new Button(activity);
        closeButton.setText("設定を閉じる");
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onClose();
            }
        });
        UiStyle.styleSecondaryButton(closeButton);
        content.addView(closeButton, buttonParams());

        scrollView.addView(content);
        panel.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    }

    public View getView() {
        return panel;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = matchWrapParams();
        params.setMargins(0, 8, 0, 8);
        return params;
    }

    private LinearLayout.LayoutParams matchWrapParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
