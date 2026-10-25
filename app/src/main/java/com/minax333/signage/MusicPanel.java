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
 * Builds the music library UI. Playback and file selection are handled by MainActivity.
 */
public final class MusicPanel {

    public interface Listener {
        void onBack();
        void onSelectTrack(int index);
    }

    private final Activity activity;
    private final Listener listener;
    private final LinearLayout panel;
    public final LinearLayout listLayout;
    public final TextView nowPlayingText;
    public final TextView emptyText;

    public MusicPanel(Activity activity, final Listener listener) {
        this.activity = activity;
        this.listener = listener;

        panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(20, 16, 20, 16);
        panel.setBackgroundColor(Color.rgb(24, 24, 24));
        panel.setVisibility(View.GONE);

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button backButton = new Button(activity);
        backButton.setText("戻る");
        backButton.setTextSize(16);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MusicPanel.this.listener.onBack();
            }
        });
        header.addView(backButton, wrapContentParams());

        TextView title = new TextView(activity);
        title.setText("音楽ライブラリ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        panel.addView(header, matchWrapParams());

        nowPlayingText = new TextView(activity);
        nowPlayingText.setTextColor(Color.LTGRAY);
        nowPlayingText.setTextSize(17);
        nowPlayingText.setPadding(8, 12, 8, 12);
        panel.addView(nowPlayingText, matchWrapParams());

        emptyText = new TextView(activity);
        emptyText.setTextColor(Color.LTGRAY);
        emptyText.setTextSize(18);
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setText("音楽ファイルがありません。\n"
                + "/sdcard/signage/audio/ に音楽を入れてください。");
        emptyText.setPadding(12, 24, 12, 24);
        panel.addView(emptyText, matchWrapParams());

        ScrollView scrollView = new ScrollView(activity);
        listLayout = new LinearLayout(activity);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listLayout);
        panel.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        TextView hint = new TextView(activity);
        hint.setText("曲をタップすると選択できます。\n"
                + "左スワイプでメイン画面へ戻ります。");
        hint.setTextColor(Color.LTGRAY);
        hint.setTextSize(14);
        hint.setPadding(8, 8, 8, 8);
        panel.addView(hint, matchWrapParams());
    }

    public View getView() {
        return panel;
    }

    private LinearLayout.LayoutParams wrapContentParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchWrapParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
