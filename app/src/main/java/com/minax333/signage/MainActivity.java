package com.minax333.signage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.media.MediaPlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private FrameLayout rootLayout;
    private ImageView imageView;
    private TextView clockTextView;
    private TextView dateTextView;
    private TextView statusTextView;

    private Button settingsButton;
    private Button musicButton;

    private LinearLayout settingsPanel;
    private LinearLayout musicPanel;
    private LinearLayout musicListLayout;

    private TextView slideshowStatusText;
    private TextView intervalStatusText;
    private TextView audioStatusText;
    private TextView musicNowPlayingText;
    private TextView musicEmptyText;

    private final Handler handler = new Handler();

    private SharedPreferences preferences;
    private MediaPlayer mediaPlayer;

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

    private final List<File> imageFiles = new ArrayList<File>();
    private final List<File> audioFiles = new ArrayList<File>();

    private int imageIndex = 0;
    private int audioIndex = 0;

    private float touchStartX;
    private float touchStartY;

    private boolean running = false;
    private boolean settingsVisible = false;
    private boolean musicVisible = false;
    private boolean slideshowEnabled = true;
    private boolean audioEnabled = false;
    private boolean audioPreparing = false;

    private int slideshowSeconds = 10;

    private String selectedAudioName = "";

    private static final String PREFS_NAME = "signage_settings";

    private static final String KEY_SLIDESHOW_ENABLED =
            "slideshow_enabled";

    private static final String KEY_SLIDESHOW_SECONDS =
            "slideshow_seconds";

    private static final String KEY_AUDIO_ENABLED =
            "audio_enabled";

    private static final String KEY_SELECTED_AUDIO =
            "selected_audio_name";

    private static final long CLOCK_INTERVAL = 1000L;

    private static final float SWIPE_DISTANCE = 90f;
    private static final float SWIPE_VERTICAL_LIMIT = 120f;

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }

            updateClock();
            handler.postDelayed(this, CLOCK_INTERVAL);
        }
    };

    private final Runnable imageRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running || settingsVisible
                    || musicVisible || !slideshowEnabled) {
                return;
            }

            loadImages();

            if (!imageFiles.isEmpty()) {
                if (imageIndex >= imageFiles.size()) {
                    imageIndex = 0;
                }

                if (imageFiles.size() > 1) {
                    imageIndex++;

                    if (imageIndex >= imageFiles.size()) {
                        imageIndex = 0;
                    }
                }

                showImage(imageFiles.get(imageIndex));
            } else {
                showStandby();
            }

            scheduleNextImage();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        preferences = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        );

        loadSettings();
        createLayout();

        updateClock();
        loadImages();
        loadAudioFiles();

        if (imageFiles.isEmpty()) {
            showStandby();
        } else {
            imageIndex = 0;
            showImage(imageFiles.get(imageIndex));
        }

        updateSettingsLabels();
        updateMusicLibrary();
    }

    private void loadSettings() {
        slideshowEnabled = preferences.getBoolean(
                KEY_SLIDESHOW_ENABLED,
                true
        );

        slideshowSeconds = preferences.getInt(
                KEY_SLIDESHOW_SECONDS,
                10
        );

        audioEnabled = preferences.getBoolean(
                KEY_AUDIO_ENABLED,
                false
        );

        selectedAudioName = preferences.getString(
                KEY_SELECTED_AUDIO,
                ""
        );

        if (slideshowSeconds < 1) {
            slideshowSeconds = 1;
        }

        if (slideshowSeconds > 3600) {
            slideshowSeconds = 3600;
        }
    }

    private void saveSettings() {
        preferences.edit()
                .putBoolean(
                        KEY_SLIDESHOW_ENABLED,
                        slideshowEnabled
                )
                .putInt(
                        KEY_SLIDESHOW_SECONDS,
                        slideshowSeconds
                )
                .putBoolean(
                        KEY_AUDIO_ENABLED,
                        audioEnabled
                )
                .putString(
                        KEY_SELECTED_AUDIO,
                        selectedAudioName
                )
                .apply();
    }

    private void createLayout() {
        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.BLACK);

        /*
         * Main slideshow image.
         */
        imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundColor(Color.BLACK);

        rootLayout.addView(
                imageView,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        /*
         * Standby status.
         */
        statusTextView = new TextView(this);
        statusTextView.setTextColor(Color.LTGRAY);
        statusTextView.setTextSize(18);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setText("Signage\n待機中");

        FrameLayout.LayoutParams statusParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                );

        rootLayout.addView(statusTextView, statusParams);

        /*
         * Date display.
         */
        dateTextView = new TextView(this);
        dateTextView.setTextColor(Color.WHITE);
        dateTextView.setTextSize(18);
        dateTextView.setGravity(Gravity.CENTER);
        dateTextView.setShadowLayer(4, 1, 1, Color.BLACK);
        dateTextView.setPadding(12, 8, 12, 4);

        FrameLayout.LayoutParams dateParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP | Gravity.CENTER_HORIZONTAL
                );

        rootLayout.addView(dateTextView, dateParams);

        /*
         * Clock display.
         */
        clockTextView = new TextView(this);
        clockTextView.setTextColor(Color.WHITE);
        clockTextView.setTextSize(32);
        clockTextView.setGravity(Gravity.CENTER);
        clockTextView.setShadowLayer(4, 1, 1, Color.BLACK);
        clockTextView.setPadding(12, 4, 12, 12);

        FrameLayout.LayoutParams clockParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
                );

        rootLayout.addView(clockTextView, clockParams);

        /*
         * Settings button.
         */
        settingsButton = new Button(this);
        settingsButton.setText("設定");
        settingsButton.setTextSize(14);

        settingsButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showSettings();
                    }
                }
        );

        FrameLayout.LayoutParams settingsButtonParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP | Gravity.RIGHT
                );

        settingsButtonParams.setMargins(0, 8, 8, 0);

        rootLayout.addView(
                settingsButton,
                settingsButtonParams
        );

        /*
         * Music library button.
         * This is provided as an alternative to swiping.
         */
        musicButton = new Button(this);
        musicButton.setText("音楽 ♪");
        musicButton.setTextSize(16);

        musicButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showMusicLibrary();
                    }
                }
        );

        FrameLayout.LayoutParams musicButtonParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM | Gravity.RIGHT
                );

        musicButtonParams.setMargins(0, 0, 12, 90);

        rootLayout.addView(
                musicButton,
                musicButtonParams
        );

        /*
         * Swipe navigation.
         * The listener returns false so ordinary touch handling
         * remains available to child views.
         */
        rootLayout.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(
                            View view,
                            MotionEvent event
                    ) {
                        handleSwipe(event);
                        return false;
                    }
                }
        );

        createMusicPanel();
        createSettingsPanel();

        setContentView(rootLayout);
    }

    private void handleSwipe(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchStartX = event.getX();
            touchStartY = event.getY();

        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            float deltaX = event.getX() - touchStartX;
            float deltaY = event.getY() - touchStartY;

            if (Math.abs(deltaX) < SWIPE_DISTANCE) {
                return;
            }

            if (Math.abs(deltaY) > SWIPE_VERTICAL_LIMIT) {
                return;
            }

            if (settingsVisible) {
                return;
            }

            if (!musicVisible && deltaX < 0) {
                showMusicLibrary();

            } else if (musicVisible && deltaX > 0) {
                hideMusicLibrary();
            }
        }
    }

    /*
     * Music library screen.
     */
    private void createMusicPanel() {
        musicPanel = new LinearLayout(this);
        musicPanel.setOrientation(LinearLayout.VERTICAL);
        musicPanel.setPadding(20, 16, 20, 16);
        musicPanel.setBackgroundColor(Color.rgb(24, 24, 24));
        musicPanel.setVisibility(View.GONE);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button backButton = new Button(this);
        backButton.setText("戻る");
        backButton.setTextSize(16);

        backButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        hideMusicLibrary();
                    }
                }
        );

        header.addView(
                backButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView title = new TextView(this);
        title.setText("音楽ライブラリ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        header.addView(title, titleParams);

        musicPanel.addView(
                header,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        musicNowPlayingText = new TextView(this);
        musicNowPlayingText.setTextColor(Color.LTGRAY);
        musicNowPlayingText.setTextSize(17);
        musicNowPlayingText.setPadding(8, 12, 8, 12);

        musicPanel.addView(
                musicNowPlayingText,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        musicEmptyText = new TextView(this);
        musicEmptyText.setTextColor(Color.LTGRAY);
        musicEmptyText.setTextSize(18);
        musicEmptyText.setGravity(Gravity.CENTER);
        musicEmptyText.setText(
                "音楽ファイルがありません。\n"
                        + "/sdcard/signage/audio/ に音楽を入れてください。"
        );
        musicEmptyText.setPadding(12, 24, 12, 24);

        musicPanel.addView(
                musicEmptyText,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        ScrollView scrollView = new ScrollView(this);

        musicListLayout = new LinearLayout(this);
        musicListLayout.setOrientation(LinearLayout.VERTICAL);

        scrollView.addView(musicListLayout);

        musicPanel.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        TextView hint = new TextView(this);
        hint.setText(
                "曲をタップすると選択できます。\n"
                        + "左スワイプでメイン画面へ戻ります。"
        );
        hint.setTextColor(Color.LTGRAY);
        hint.setTextSize(14);
        hint.setPadding(8, 8, 8, 8);

        musicPanel.addView(
                hint,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        FrameLayout.LayoutParams panelParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                );

        rootLayout.addView(musicPanel, panelParams);
    }

    private void showMusicLibrary() {
        if (settingsVisible) {
            return;
        }

        musicVisible = true;

        handler.removeCallbacks(imageRunnable);

        loadAudioFiles();
        updateMusicLibrary();

        musicPanel.setVisibility(View.VISIBLE);
        musicPanel.bringToFront();
    }

    private void hideMusicLibrary() {
        musicVisible = false;
        musicPanel.setVisibility(View.GONE);

        settingsButton.bringToFront();
        musicButton.bringToFront();

        updateClock();
        loadImages();

        if (imageFiles.isEmpty()) {
            showStandby();
        } else {
            if (imageIndex >= imageFiles.size()) {
                imageIndex = 0;
            }

            showImage(imageFiles.get(imageIndex));
        }

        scheduleNextImage();
    }

    private void updateMusicLibrary() {
        if (musicListLayout == null) {
            return;
        }

        loadAudioFiles();

        musicListLayout.removeAllViews();

        if (audioFiles.isEmpty()) {
            musicEmptyText.setVisibility(View.VISIBLE);
        } else {
            musicEmptyText.setVisibility(View.GONE);
        }

        for (int i = 0; i < audioFiles.size(); i++) {
            final int index = i;
            final File audioFile = audioFiles.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(12, 12, 12, 12);

            final boolean selected =
                    audioFile.getName().equals(selectedAudioName);

            if (selected) {
                row.setBackgroundColor(Color.rgb(55, 75, 95));
            } else {
                row.setBackgroundColor(Color.rgb(36, 36, 36));
            }

            TextView number = new TextView(this);
            number.setText(String.valueOf(i + 1));
            number.setTextColor(Color.LTGRAY);
            number.setTextSize(16);
            number.setGravity(Gravity.CENTER);
            number.setMinWidth(40);

            row.addView(
                    number,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            TextView name = new TextView(this);
            name.setText(audioFile.getName());
            name.setTextColor(Color.WHITE);
            name.setTextSize(18);
            name.setPadding(12, 4, 12, 4);

            row.addView(
                    name,
                    new LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1
                    )
            );

            TextView state = new TextView(this);
            state.setText(selected ? "選択中" : "");
            state.setTextColor(Color.rgb(150, 220, 255));
            state.setTextSize(14);

            row.addView(
                    state,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            row.setMinimumHeight(64);

            row.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            selectAudioTrack(index);
                        }
                    }
            );

            musicListLayout.addView(
                    row,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            View separator = new View(this);
            separator.setBackgroundColor(Color.DKGRAY);

            musicListLayout.addView(
                    separator,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            1
                    )
            );
        }

        updateNowPlayingLabel();
    }

    private void selectAudioTrack(int index) {
        loadAudioFiles();

        if (index < 0 || index >= audioFiles.size()) {
            return;
        }

        audioIndex = index;
        selectedAudioName = audioFiles.get(index).getName();

        saveSettings();
        updateMusicLibrary();
        updateSettingsLabels();

        /*
         * If music is already enabled, immediately start the
         * selected song. Otherwise, it will play when enabled.
         */
        if (audioEnabled && running) {
            playAudioAtIndex(audioIndex);
        }
    }

    private void updateNowPlayingLabel() {
        if (musicNowPlayingText == null) {
            return;
        }

        if (selectedAudioName == null
                || selectedAudioName.length() == 0) {
            musicNowPlayingText.setText(
                    "選択中の曲：なし\n"
                            + "曲をタップして選択してください。"
            );
        } else {
            musicNowPlayingText.setText(
                    "選択中の曲：" + selectedAudioName
                            + "\n再生：ファイル名順・一覧を繰り返す"
            );
        }
    }

    /*
     * Settings screen.
     */
    private void createSettingsPanel() {
        settingsPanel = new LinearLayout(this);
        settingsPanel.setOrientation(LinearLayout.VERTICAL);
        settingsPanel.setGravity(Gravity.CENTER_HORIZONTAL);
        settingsPanel.setPadding(24, 24, 24, 24);
        settingsPanel.setBackgroundColor(Color.rgb(30, 30, 30));
        settingsPanel.setVisibility(View.GONE);

        ScrollView scrollView = new ScrollView(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("Signage 設定");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 8, 0, 24);

        content.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView slideshowTitle = new TextView(this);
        slideshowTitle.setText("画像スライドショー");
        slideshowTitle.setTextColor(Color.WHITE);
        slideshowTitle.setTextSize(20);

        content.addView(slideshowTitle);

        slideshowStatusText = new TextView(this);
        slideshowStatusText.setTextColor(Color.LTGRAY);
        slideshowStatusText.setTextSize(16);
        slideshowStatusText.setPadding(0, 8, 0, 8);

        content.addView(slideshowStatusText);

        Button toggleButton = new Button(this);
        toggleButton.setText("スライドショー オン／オフ");

        toggleButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        slideshowEnabled = !slideshowEnabled;

                        saveSettings();
                        updateSettingsLabels();
                        restartImageTimer();
                    }
                }
        );

        content.addView(toggleButton, buttonLayoutParams());

        intervalStatusText = new TextView(this);
        intervalStatusText.setTextColor(Color.LTGRAY);
        intervalStatusText.setTextSize(16);
        intervalStatusText.setPadding(0, 20, 0, 8);

        content.addView(intervalStatusText);

        Button intervalButton = new Button(this);
        intervalButton.setText("切り替え秒数を変更");

        intervalButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showIntervalDialog();
                    }
                }
        );

        content.addView(intervalButton, buttonLayoutParams());

        TextView audioTitle = new TextView(this);
        audioTitle.setText("\n音楽再生");
        audioTitle.setTextColor(Color.WHITE);
        audioTitle.setTextSize(20);

        content.addView(audioTitle);

        audioStatusText = new TextView(this);
        audioStatusText.setTextColor(Color.LTGRAY);
        audioStatusText.setTextSize(16);
        audioStatusText.setPadding(0, 8, 0, 8);

        content.addView(audioStatusText);

        Button audioToggleButton = new Button(this);
        audioToggleButton.setText("音楽 オン／オフ");

        audioToggleButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        audioEnabled = !audioEnabled;

                        saveSettings();
                        updateSettingsLabels();

                        if (audioEnabled) {
                            startAudioPlayback();
                        } else {
                            stopAudioPlayback();
                        }
                    }
                }
        );

        content.addView(
                audioToggleButton,
                buttonLayoutParams()
        );

        TextView playbackInfo = new TextView(this);
        playbackInfo.setText(
                "\n再生方法\n"
                        + "・曲一覧のファイル名順\n"
                        + "・最後の曲の次は先頭へ戻る\n"
                        + "・音楽一覧から曲を選択\n"
                        + "・選択曲は設定に保存"
        );
        playbackInfo.setTextColor(Color.LTGRAY);
        playbackInfo.setTextSize(16);
        playbackInfo.setPadding(0, 12, 0, 16);

        content.addView(
                playbackInfo,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView futureTitle = new TextView(this);
        futureTitle.setText(
                "\n今後追加する機能\n"
                        + "・背景動画\n"
                        + "・画像の選択\n"
                        + "・文字表示の設定"
        );
        futureTitle.setTextColor(Color.LTGRAY);
        futureTitle.setTextSize(17);
        futureTitle.setPadding(0, 16, 0, 16);

        content.addView(
                futureTitle,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        Button closeButton = new Button(this);
        closeButton.setText("設定を閉じる");

        closeButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        hideSettings();
                    }
                }
        );

        content.addView(closeButton, buttonLayoutParams());

        scrollView.addView(content);

        settingsPanel.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        FrameLayout.LayoutParams panelParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                );

        rootLayout.addView(settingsPanel, panelParams);
    }

    private LinearLayout.LayoutParams buttonLayoutParams() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 6, 0, 6);
        return params;
    }

    private void showSettings() {
        settingsVisible = true;

        handler.removeCallbacks(imageRunnable);

        updateSettingsLabels();

        settingsPanel.setVisibility(View.VISIBLE);
        settingsPanel.bringToFront();
    }

    private void hideSettings() {
        settingsVisible = false;
        settingsPanel.setVisibility(View.GONE);

        settingsButton.bringToFront();
        musicButton.bringToFront();

        updateClock();
        loadImages();

        if (imageFiles.isEmpty()) {
            showStandby();
        } else {
            if (imageIndex >= imageFiles.size()) {
                imageIndex = 0;
            }

            showImage(imageFiles.get(imageIndex));
        }

        scheduleNextImage();
    }

    private void updateSettingsLabels() {
        if (slideshowStatusText != null) {
            slideshowStatusText.setText(
                    "状態：" + (slideshowEnabled ? "オン" : "オフ")
            );
        }

        if (intervalStatusText != null) {
            intervalStatusText.setText(
                    "切り替え間隔：" + slideshowSeconds + " 秒"
            );
        }

        if (audioStatusText != null) {
            audioStatusText.setText(
                    "状態：" + (audioEnabled ? "オン" : "オフ")
                            + "\n音楽ファイル数：" + audioFiles.size()
                            + "\n選択中：" + getSelectedAudioDisplayName()
            );
        }
    }

    private String getSelectedAudioDisplayName() {
        if (selectedAudioName == null
                || selectedAudioName.length() == 0) {
            return "なし";
        }

        return selectedAudioName;
    }

    private void showIntervalDialog() {
        final String[] options = new String[] {
                "1 秒",
                "3 秒",
                "5 秒",
                "10 秒",
                "15 秒",
                "30 秒",
                "60 秒",
                "120 秒",
                "300 秒"
        };

        final int[] seconds = new int[] {
                1, 3, 5, 10, 15, 30, 60, 120, 300
        };

        new AlertDialog.Builder(this)
                .setTitle("画像の切り替え間隔")
                .setItems(
                        options,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialog,
                                    int which
                            ) {
                                slideshowSeconds = seconds[which];

                                saveSettings();
                                updateSettingsLabels();
                                restartImageTimer();
                            }
                        }
                )
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void restartImageTimer() {
        handler.removeCallbacks(imageRunnable);

        if (running && !settingsVisible
                && !musicVisible && slideshowEnabled) {
            scheduleNextImage();
        }
    }

    private void scheduleNextImage() {
        handler.removeCallbacks(imageRunnable);

        if (running && !settingsVisible
                && !musicVisible && slideshowEnabled) {
            handler.postDelayed(
                    imageRunnable,
                    slideshowSeconds * 1000L
            );
        }
    }

    private void updateClock() {
        Date now = new Date();

        clockTextView.setText(timeFormat.format(now));
        dateTextView.setText(dateFormat.format(now));
    }

    /*
     * Image file discovery.
     */
    private void loadImages() {
        imageFiles.clear();

        List<File> directories = findDirectories("images");

        for (File directory : directories) {
            File[] files = directory.listFiles();

            if (files == null) {
                continue;
            }

            Arrays.sort(files, new java.util.Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });

            for (File file : files) {
                if (file.isFile() && isImageFile(file)
                        && !imageFiles.contains(file)) {
                    imageFiles.add(file);
                }
            }
        }

        if (imageIndex >= imageFiles.size()) {
            imageIndex = 0;
        }
    }

    /*
     * Audio file discovery.
     */
    private void loadAudioFiles() {
        audioFiles.clear();

        List<File> directories = findDirectories("audio");

        for (File directory : directories) {
            File[] files = directory.listFiles();

            if (files == null) {
                continue;
            }

            Arrays.sort(files, new java.util.Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });

            for (File file : files) {
                if (file.isFile() && isAudioFile(file)
                        && !audioFiles.contains(file)) {
                    audioFiles.add(file);
                }
            }
        }

        /*
         * Restore the selected track by filename.
         * This avoids relying on an index that could change
         * if files are added or removed.
         */
        if (selectedAudioName != null
                && selectedAudioName.length() > 0) {
            int foundIndex = findAudioIndex(selectedAudioName);

            if (foundIndex >= 0) {
                audioIndex = foundIndex;
            } else if (!audioFiles.isEmpty()) {
                audioIndex = 0;
            } else {
                audioIndex = 0;
            }
        } else {
            if (audioIndex >= audioFiles.size()) {
                audioIndex = 0;
            }

            if (!audioFiles.isEmpty()
                    && selectedAudioName.length() == 0) {
                audioIndex = 0;
            }
        }

        if (!audioFiles.isEmpty()) {
            if (audioIndex < 0 || audioIndex >= audioFiles.size()) {
                audioIndex = 0;
            }
        } else {
            audioIndex = 0;
        }
    }

    private int findAudioIndex(String fileName) {
        for (int i = 0; i < audioFiles.size(); i++) {
            if (audioFiles.get(i).getName().equals(fileName)) {
                return i;
            }
        }

        return -1;
    }

    private List<File> findDirectories(String folderName) {
        List<File> result = new ArrayList<File>();
        List<File> roots = new ArrayList<File>();

        addRoot(roots, Environment.getExternalStorageDirectory());
        addRoot(roots, new File("/sdcard"));
        addRoot(roots, new File("/storage"));
        addRoot(roots, new File("/mnt"));
        addRoot(roots, new File("/mnt/media_rw"));

        for (File root : roots) {
            addMediaDirectory(
                    result,
                    new File(root, "signage/" + folderName)
            );

            File[] children = root.listFiles();

            if (children == null) {
                continue;
            }

            for (File child : children) {
                if (child.isDirectory()) {
                    addMediaDirectory(
                            result,
                            new File(
                                    child,
                                    "signage/" + folderName
                            )
                    );
                }
            }
        }

        return result;
    }

    private void addRoot(List<File> roots, File root) {
        if (root != null
                && root.isDirectory()
                && !roots.contains(root)) {
            roots.add(root);
        }
    }

    private void addMediaDirectory(
            List<File> directories,
            File directory
    ) {
        if (directory.isDirectory()
                && directory.canRead()
                && !directories.contains(directory)) {
            directories.add(directory);
        }
    }

    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase(Locale.US);

        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png")
                || name.endsWith(".webp")
                || name.endsWith(".bmp")
                || name.endsWith(".gif");
    }

    private boolean isAudioFile(File file) {
        String name = file.getName().toLowerCase(Locale.US);

        return name.endsWith(".mp3")
                || name.endsWith(".m4a")
                || name.endsWith(".aac")
                || name.endsWith(".ogg")
                || name.endsWith(".wav")
                || name.endsWith(".3gp");
    }

    /*
     * Image display.
     */
    private void showImage(File file) {
        Bitmap bitmap = decodeImage(file);

        if (bitmap == null) {
            imageView.setImageDrawable(null);
            showStandby();
            return;
        }

        imageView.setImageBitmap(bitmap);
        statusTextView.setVisibility(View.GONE);

        clockTextView.setVisibility(View.VISIBLE);
        dateTextView.setVisibility(View.VISIBLE);
    }

    private Bitmap decodeImage(File file) {
        BitmapFactory.Options options =
                new BitmapFactory.Options();

        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(
                file.getAbsolutePath(),
                options
        );

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null;
        }

        int maxDimension = 2048;
        int sampleSize = 1;

        while (options.outWidth / sampleSize > maxDimension
                || options.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2;
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;

        FileInputStream input = null;

        try {
            input = new FileInputStream(file);

            return BitmapFactory.decodeStream(
                    input,
                    null,
                    options
            );
        } catch (IOException e) {
            return null;
        } catch (OutOfMemoryError e) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void showStandby() {
        imageView.setImageDrawable(null);

        statusTextView.setText("Signage\n画像待機中");
        statusTextView.setVisibility(View.VISIBLE);

        clockTextView.setVisibility(View.VISIBLE);
        dateTextView.setVisibility(View.VISIBLE);
    }

    /*
     * Audio playback.
     *
     * Tracks play in filename order. After the last track,
     * playback returns to the first track.
     */
    private void startAudioPlayback() {
        if (!running || !audioEnabled) {
            return;
        }

        if (mediaPlayer != null || audioPreparing) {
            return;
        }

        loadAudioFiles();
        updateSettingsLabels();

        if (audioFiles.isEmpty()) {
            updateMusicLibrary();
            return;
        }

        if (audioIndex < 0 || audioIndex >= audioFiles.size()) {
            audioIndex = 0;
        }

        playAudioAtIndex(audioIndex);
    }

    private void playAudioAtIndex(int index) {
        if (!running || !audioEnabled) {
            return;
        }

        loadAudioFiles();

        if (audioFiles.isEmpty()) {
            return;
        }

        if (index < 0 || index >= audioFiles.size()) {
            index = 0;
        }

        releaseAudioPlayer();

        audioIndex = index;
        selectedAudioName = audioFiles.get(audioIndex).getName();

        saveSettings();
        updateNowPlayingLabel();
        updateSettingsLabels();

        final MediaPlayer player = new MediaPlayer();

        mediaPlayer = player;
        audioPreparing = true;

        try {
            player.setDataSource(
                    audioFiles.get(audioIndex).getAbsolutePath()
            );

            player.setOnPreparedListener(
                    new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            if (mediaPlayer != player
                                    || !running
                                    || !audioEnabled) {
                                return;
                            }

                            audioPreparing = false;

                            try {
                                player.start();
                                updateNowPlayingLabel();
                            } catch (IllegalStateException e) {
                                skipToNextTrack();
                            }
                        }
                    }
            );

            player.setOnCompletionListener(
                    new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            if (mediaPlayer != player) {
                                return;
                            }

                            releaseAudioPlayer();

                            loadAudioFiles();

                            if (audioFiles.isEmpty()) {
                                return;
                            }

                            int nextIndex = audioIndex + 1;

                            if (nextIndex >= audioFiles.size()) {
                                nextIndex = 0;
                            }

                            audioIndex = nextIndex;
                            selectedAudioName =
                                    audioFiles.get(audioIndex).getName();

                            saveSettings();
                            updateNowPlayingLabel();
                            updateSettingsLabels();

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    startAudioPlayback();
                                }
                            });
                        }
                    }
            );

            player.setOnErrorListener(
                    new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(
                                MediaPlayer mp,
                                int what,
                                int extra
                        ) {
                            if (mediaPlayer == player) {
                                skipToNextTrack();
                            }

                            return true;
                        }
                    }
            );

            player.prepareAsync();

        } catch (IOException e) {
            skipToNextTrack();
        } catch (IllegalArgumentException e) {
            skipToNextTrack();
        } catch (IllegalStateException e) {
            skipToNextTrack();
        }
    }

    private void skipToNextTrack() {
        releaseAudioPlayer();

        if (!running || !audioEnabled) {
            return;
        }

        loadAudioFiles();

        if (audioFiles.isEmpty()) {
            return;
        }

        audioIndex++;

        if (audioIndex >= audioFiles.size()) {
            audioIndex = 0;
        }

        selectedAudioName = audioFiles.get(audioIndex).getName();

        saveSettings();
        updateNowPlayingLabel();
        updateSettingsLabels();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startAudioPlayback();
            }
        }, 500L);
    }

    private void stopAudioPlayback() {
        releaseAudioPlayer();
    }

    private void releaseAudioPlayer() {
        if (mediaPlayer != null) {
            MediaPlayer oldPlayer = mediaPlayer;

            mediaPlayer = null;
            audioPreparing = false;

            oldPlayer.setOnPreparedListener(null);
            oldPlayer.setOnCompletionListener(null);
            oldPlayer.setOnErrorListener(null);

            try {
                oldPlayer.stop();
            } catch (IllegalStateException ignored) {
            }

            oldPlayer.release();
        } else {
            audioPreparing = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (settingsVisible) {
            hideSettings();
        } else if (musicVisible) {
            hideMusicLibrary();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        running = true;

        handler.removeCallbacks(clockRunnable);
        handler.removeCallbacks(imageRunnable);

        loadSettings();
        updateClock();

        loadImages();
        loadAudioFiles();

        if (imageFiles.isEmpty()) {
            showStandby();
        } else {
            imageIndex = 0;
            showImage(imageFiles.get(imageIndex));
        }

        updateSettingsLabels();
        updateMusicLibrary();

        handler.postDelayed(clockRunnable, CLOCK_INTERVAL);
        scheduleNextImage();

        if (audioEnabled) {
            startAudioPlayback();
        }
    }

    @Override
    protected void onPause() {
        running = false;

        handler.removeCallbacks(clockRunnable);
        handler.removeCallbacks(imageRunnable);

        stopAudioPlayback();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        running = false;

        handler.removeCallbacks(clockRunnable);
        handler.removeCallbacks(imageRunnable);

        stopAudioPlayback();

        if (imageView != null) {
            imageView.setImageDrawable(null);
        }

        super.onDestroy();
    }
}
