package com.minax333.signage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
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
import android.view.TextureView;
import android.media.MediaPlayer;

import java.io.File;
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
    private TextureView videoView;
    private VideoBackgroundManager videoBackgroundManager;
    private TextView clockTextView;
    private TextView dateTextView;
    private TextView statusTextView;

    private Button settingsButton;
    private Button musicButton;

    private SettingsPanel settingsPanel;
    private MusicPanel musicPanel;

    private final Handler handler = new Handler();

    private SharedPreferences preferences;
    private final SettingsManager settingsManager = new SettingsManager();
    private MediaPlayer mediaPlayer;

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

    private final List<File> audioFiles = new ArrayList<File>();

    private int audioIndex = 0;

    private float touchStartX;
    private float touchStartY;

    private boolean running = false;
    private boolean settingsVisible = false;
    private boolean musicVisible = false;
    private Intent pendingMediaIntent;


    private boolean audioPreparing = false;
    private SlideshowManager slideshowManager;















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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        pendingMediaIntent = getIntent();

        preferences = getSharedPreferences(
                SettingsManager.PREFS_NAME,
                MODE_PRIVATE
        );

        loadSettings();
        createLayout();

        updateClock();
        loadAudioFiles();
        // Video playback starts in onResume(), after the Activity is active.
        // Starting VideoView here as well can create overlapping playback sessions.
        if (!settingsManager.videoMode) {
            slideshowManager.showFirstImageOrStandby();
        }

        updateSettingsLabels();
        updateMusicLibrary();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        pendingMediaIntent = intent;
        if (running) {
            handleIncomingMediaIntent();
        }
    }

    private void handleIncomingMediaIntent() {
        Intent intent = pendingMediaIntent;
        pendingMediaIntent = null;

        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) {
            return;
        }

        final Uri uri = intent.getData();
        if (uri == null) {
            return;
        }

        String type = intent.getType();
        if (type == null) {
            type = getContentResolver().getType(uri);
        }
        if (type == null) {
            String path = uri.getLastPathSegment();
            if (path != null) {
                String lower = path.toLowerCase(Locale.US);
                if (lower.endsWith(".mp4") || lower.endsWith(".3gp")
                        || lower.endsWith(".m4v") || lower.endsWith(".webm")
                        || lower.endsWith(".mkv") || lower.endsWith(".avi")) {
                    type = "video/*";
                } else if (lower.endsWith(".mp3") || lower.endsWith(".m4a")
                        || lower.endsWith(".aac") || lower.endsWith(".ogg")
                        || lower.endsWith(".wav")) {
                    type = "audio/*";
                } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                        || lower.endsWith(".png") || lower.endsWith(".webp")
                        || lower.endsWith(".bmp") || lower.endsWith(".gif")) {
                    type = "image/*";
                }
            }
        }

        if (type == null) {
            showOpenMediaError("このファイル形式には対応していません。");
            return;
        }

        if (type.startsWith("video/")) {
            settingsManager.videoMode = true;
            saveSettings();
            updateSettingsLabels();
            videoBackgroundManager.playExternalUri(uri);
            applyDisplayMode();
        } else if (type.startsWith("audio/")) {
            settingsManager.videoMode = false;
            saveSettings();
            videoBackgroundManager.stop();
            videoView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            slideshowManager.stopTimer();
            slideshowManager.showCurrentImageOrStandby();
            updateClock();
            stopAudioPlayback();
            playExternalAudio(uri);
        } else if (type.startsWith("image/")) {
            settingsManager.videoMode = false;
            saveSettings();
            videoBackgroundManager.stop();
            stopAudioPlayback();
            slideshowManager.stopTimer();
            videoView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageURI(uri);
            statusTextView.setVisibility(View.GONE);
            clockTextView.setVisibility(View.VISIBLE);
            dateTextView.setVisibility(View.VISIBLE);
            statusTextView.bringToFront();
            dateTextView.bringToFront();
            clockTextView.bringToFront();
            settingsButton.bringToFront();
            musicButton.bringToFront();
        } else {
            showOpenMediaError("このファイル形式には対応していません。");
        }
    }

    private void showOpenMediaError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("ファイルを開けません")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void playExternalAudio(final Uri uri) {
        releaseAudioPlayer();

        final MediaPlayer player = new MediaPlayer();
        mediaPlayer = player;
        audioPreparing = true;

        try {
            player.setDataSource(this, uri);
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    if (mediaPlayer != player || !running) {
                        return;
                    }
                    audioPreparing = false;
                    try {
                        player.start();
                    } catch (IllegalStateException exception) {
                        releaseAudioPlayer();
                        showOpenMediaError("音声ファイルを再生できませんでした。");
                    }
                }
            });
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (mediaPlayer == player) {
                        releaseAudioPlayer();
                    }
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    if (mediaPlayer == player) {
                        releaseAudioPlayer();
                        showOpenMediaError("音声ファイルを再生できませんでした。");
                    }
                    return true;
                }
            });
            player.prepareAsync();
        } catch (IOException exception) {
            releaseAudioPlayer();
            showOpenMediaError("音声ファイルを開けませんでした。");
        } catch (IllegalArgumentException exception) {
            releaseAudioPlayer();
            showOpenMediaError("この音声形式には対応していません。");
        } catch (IllegalStateException exception) {
            releaseAudioPlayer();
            showOpenMediaError("音声ファイルを再生できませんでした。");
        }
    }

    private void loadSettings() {
        settingsManager.load(preferences);
    }

    private void saveSettings() {
        settingsManager.save(preferences);
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

        videoView = new TextureView(this);
        videoView.setBackgroundColor(Color.BLACK);
        videoView.setVisibility(View.GONE);
        rootLayout.addView(
                videoView,
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
        statusTextView.setTextSize(20);
        statusTextView.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(24, 18, 24, 18);
        statusTextView.setBackground(UiStyle.makeBackground(Color.argb(190, 15, 23, 42), 18));
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
        dateTextView.setGravity(Gravity.CENTER);
        UiStyle.styleOverlay(dateTextView, 17);

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
        clockTextView.setGravity(Gravity.CENTER);
        UiStyle.styleOverlay(clockTextView, 30);

        FrameLayout.LayoutParams clockParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
                );

        rootLayout.addView(clockTextView, clockParams);

        slideshowManager = new SlideshowManager(
                this,
                handler,
                settingsManager,
                imageView,
                statusTextView,
                clockTextView,
                dateTextView,
                new SlideshowManager.StateProvider() {
                    @Override
                    public boolean isRunning() {
                        return running;
                    }

                    @Override
                    public boolean isSettingsVisible() {
                        return settingsVisible;
                    }

                    @Override
                    public boolean isMusicVisible() {
                        return musicVisible;
                    }
                }
        );

        videoBackgroundManager = new VideoBackgroundManager(
                this,
                videoView,
                new VideoBackgroundManager.Listener() {
                    @Override
                    public void onNoVideosFound() {
                        if (settingsManager.videoMode) {
                            settingsManager.videoMode = false;
                            saveSettings();
                            updateSettingsLabels();
                            applyDisplayMode();
                        }
                    }
                }
        );

        /*
         * Settings button.
         */
        settingsButton = new Button(this);
        settingsButton.setText("設定");
        UiStyle.styleButton(settingsButton);

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

        settingsButtonParams.setMargins(0, 12, 12, 0);

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
        UiStyle.styleButton(musicButton);

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

        musicButtonParams.setMargins(0, 0, 12, 96);

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
        musicPanel = new MusicPanel(
                this,
                new MusicPanel.Listener() {
                    @Override
                    public void onBack() {
                        hideMusicLibrary();
                    }

                    @Override
                    public void onSelectTrack(int index) {
                        selectAudioTrack(index);
                    }
                }
        );
        rootLayout.addView(
                musicPanel.getView(),
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                )
        );
    }

    private void showMusicLibrary() {
        if (settingsVisible) {
            return;
        }

        musicVisible = true;

        slideshowManager.stopTimer();

        loadAudioFiles();
        updateMusicLibrary();

        musicPanel.getView().setVisibility(View.VISIBLE);
        musicPanel.getView().bringToFront();
    }

    private void hideMusicLibrary() {
        musicVisible = false;
        musicPanel.getView().setVisibility(View.GONE);

        settingsButton.bringToFront();
        musicButton.bringToFront();

        updateClock();
        applyDisplayMode();
    }

    private void updateMusicLibrary() {
        if (musicPanel.listLayout == null) {
            return;
        }

        loadAudioFiles();

        musicPanel.listLayout.removeAllViews();

        if (audioFiles.isEmpty()) {
            musicPanel.emptyText.setVisibility(View.VISIBLE);
        } else {
            musicPanel.emptyText.setVisibility(View.GONE);
        }

        for (int i = 0; i < audioFiles.size(); i++) {
            final int index = i;
            final File audioFile = audioFiles.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(12, 12, 12, 12);

            final boolean selected =
                    audioFile.getName().equals(settingsManager.selectedAudioName);

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

            musicPanel.listLayout.addView(
                    row,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            View separator = new View(this);
            separator.setBackgroundColor(Color.DKGRAY);

            musicPanel.listLayout.addView(
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
        settingsManager.selectedAudioName = audioFiles.get(index).getName();

        saveSettings();
        updateMusicLibrary();
        updateSettingsLabels();

        /*
         * If music is already enabled, immediately start the
         * selected song. Otherwise, it will play when enabled.
         */
        if (settingsManager.audioEnabled && running) {
            playAudioAtIndex(audioIndex);
        }
    }

    private void updateNowPlayingLabel() {
        if (musicPanel.nowPlayingText == null) {
            return;
        }

        if (settingsManager.selectedAudioName == null
                || settingsManager.selectedAudioName.length() == 0) {
            musicPanel.nowPlayingText.setText(
                    "選択中の曲：なし\n"
                            + "曲をタップして選択してください。"
            );
        } else {
            musicPanel.nowPlayingText.setText(
                    "選択中の曲：" + settingsManager.selectedAudioName
                            + "\n再生：ファイル名順・一覧を繰り返す"
            );
        }
    }

    /*
     * Settings screen.
     */
    private void createSettingsPanel() {
        settingsPanel = new SettingsPanel(
                this,
                new SettingsPanel.Listener() {
                    @Override
                    public void onToggleSlideshow() {
                        settingsManager.slideshowEnabled =
                                !settingsManager.slideshowEnabled;
                        saveSettings();
                        updateSettingsLabels();
                        restartImageTimer();
                    }

                    @Override
                    public void onToggleDisplayMode() {
                        settingsManager.videoMode = !settingsManager.videoMode;
                        saveSettings();
                        updateSettingsLabels();
                        applyDisplayMode();
                    }

                    @Override
                    public void onChangeInterval() {
                        showIntervalDialog();
                    }

                    @Override
                    public void onToggleAudio() {
                        settingsManager.audioEnabled =
                                !settingsManager.audioEnabled;
                        saveSettings();
                        updateSettingsLabels();

                        if (settingsManager.audioEnabled) {
                            startAudioPlayback();
                        } else {
                            stopAudioPlayback();
                        }
                    }

                    @Override
                    public void onClose() {
                        hideSettings();
                    }
                }
        );
        rootLayout.addView(
                settingsPanel.getView(),
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                )
        );
    }

    

    private void showSettings() {
        settingsVisible = true;

        slideshowManager.stopTimer();

        updateSettingsLabels();

        settingsPanel.getView().setVisibility(View.VISIBLE);
        settingsPanel.getView().bringToFront();
    }

    private void hideSettings() {
        settingsVisible = false;
        settingsPanel.getView().setVisibility(View.GONE);

        settingsButton.bringToFront();
        musicButton.bringToFront();

        updateClock();
        applyDisplayMode();
    }

    private void updateSettingsLabels() {
        if (settingsPanel.displayModeStatusText != null) {
            settingsPanel.displayModeStatusText.setText(
                    "現在：" + (settingsManager.videoMode ? "動画プレイヤー" : "画像スライドショー")
                            + "\\n初期設定：画像スライドショー"
            );
        }

        if (settingsPanel.slideshowStatusText != null) {
            settingsPanel.slideshowStatusText.setText(
                    "状態：" + (settingsManager.slideshowEnabled ? "オン" : "オフ")
            );
        }

        if (settingsPanel.intervalStatusText != null) {
            settingsPanel.intervalStatusText.setText(
                    "切り替え間隔：" + settingsManager.slideshowSeconds + " 秒"
            );
        }

        if (settingsPanel.audioStatusText != null) {
            settingsPanel.audioStatusText.setText(
                    "状態：" + (settingsManager.audioEnabled ? "オン" : "オフ")
                            + "\n音楽ファイル数：" + audioFiles.size()
                            + "\n選択中：" + getSelectedAudioDisplayName()
            );
        }
    }

    private String getSelectedAudioDisplayName() {
        if (settingsManager.selectedAudioName == null
                || settingsManager.selectedAudioName.length() == 0) {
            return "なし";
        }

        return settingsManager.selectedAudioName;
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
                                settingsManager.slideshowSeconds = seconds[which];

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
        if (!settingsManager.videoMode) {
            slideshowManager.restartTimer();
        }
    }

    private void applyDisplayMode() {
        if (videoBackgroundManager == null || slideshowManager == null) {
            return;
        }

        if (settingsManager.videoMode) {
            slideshowManager.stopTimer();
            stopAudioPlayback();

            // The standby TextView is layered above the VideoView.
            // Hide it while video mode is active so it cannot cover the video.
            statusTextView.setVisibility(View.GONE);
            clockTextView.setVisibility(View.VISIBLE);
            dateTextView.setVisibility(View.VISIBLE);

            imageView.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            videoView.bringToFront();

            // Keep controls and overlays above the video surface.
            statusTextView.bringToFront();
            dateTextView.bringToFront();
            clockTextView.bringToFront();
            settingsButton.bringToFront();
            musicButton.bringToFront();

            videoBackgroundManager.start();
        } else {
            videoBackgroundManager.stop();
            videoView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            slideshowManager.showCurrentImageOrStandby();
            slideshowManager.scheduleNextImage();

            if (running && settingsManager.audioEnabled) {
                startAudioPlayback();
            }
        }
    }

    private void updateClock() {
        Date now = new Date();

        clockTextView.setText(timeFormat.format(now));
        dateTextView.setText(dateFormat.format(now));
    }

    /*
     * Audio file discovery.
     */
    private void loadAudioFiles() {
        audioFiles.clear();

        List<File> directories = MediaFileUtils.findDirectories("audio");

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
                if (file.isFile() && MediaFileUtils.isAudioFile(file)
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
        if (settingsManager.selectedAudioName != null
                && settingsManager.selectedAudioName.length() > 0) {
            int foundIndex = findAudioIndex(settingsManager.selectedAudioName);

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
                    && settingsManager.selectedAudioName.length() == 0) {
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

    /*
     * Audio playback.
     *
     * Tracks play in filename order. After the last track,
     * playback returns to the first track.
     */
    private void startAudioPlayback() {
        if (!running || !settingsManager.audioEnabled) {
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
        if (!running || !settingsManager.audioEnabled) {
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
        settingsManager.selectedAudioName = audioFiles.get(audioIndex).getName();

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
                                    || !settingsManager.audioEnabled) {
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
                            settingsManager.selectedAudioName =
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

        if (!running || !settingsManager.audioEnabled) {
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

        settingsManager.selectedAudioName = audioFiles.get(audioIndex).getName();

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
        slideshowManager.stopTimer();

        loadSettings();
        updateClock();

        loadAudioFiles();

        if (settingsManager.videoMode) {
            applyDisplayMode();
        } else {
            slideshowManager.showFirstImageOrStandby();
            slideshowManager.scheduleNextImage();
        }

        updateSettingsLabels();
        updateMusicLibrary();

        handler.postDelayed(clockRunnable, CLOCK_INTERVAL);

        if (!settingsManager.videoMode && settingsManager.audioEnabled) {
            startAudioPlayback();
        }

        if (pendingMediaIntent != null) {
            handleIncomingMediaIntent();
        }
    }

    @Override
    protected void onPause() {
        running = false;

        handler.removeCallbacks(clockRunnable);
        slideshowManager.stopTimer();
        if (videoBackgroundManager != null) {
            videoBackgroundManager.stop();
        }

        stopAudioPlayback();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        running = false;

        handler.removeCallbacks(clockRunnable);
        slideshowManager.stopTimer();

        stopAudioPlayback();

        if (slideshowManager != null) {
            slideshowManager.clearDisplayedImage();
        }
        if (videoBackgroundManager != null) {
            videoBackgroundManager.release();
        }

        super.onDestroy();
    }
}
