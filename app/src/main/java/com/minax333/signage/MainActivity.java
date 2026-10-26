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
    private VideoControlsPanel videoControlsPanel;
    private boolean userSeekingVideo = false;
    private final Runnable hideVideoControlsRunnable = new Runnable() {
        @Override public void run() {
            if (videoControlsPanel != null && settingsManager.videoMode && !settingsVisible && !musicVisible && !homeVisible) {
                videoControlsPanel.getView().setVisibility(View.GONE);
            }
        }
    };
    private final Runnable updateVideoControlsRunnable = new Runnable() {
        @Override public void run() {
            if (!running || videoControlsPanel == null || !settingsManager.videoMode) return;
            if (videoControlsPanel.getView().getVisibility() == View.VISIBLE) updateVideoControls();
            handler.postDelayed(this, 500L);
        }
    };
    private TextView clockTextView;
    private TextView dateTextView;
    private TextView statusTextView;

    private Button settingsButton;
    private Button musicButton;
    private Button homeButton;

    private SettingsPanel settingsPanel;
    private MusicPanel musicPanel;
    private Home homePanel;
    private CreativeDisplayView creativeDisplayView;
    private boolean creativeDisplayVisible = false;

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
    private boolean homeVisible = true;
    private Intent pendingMediaIntent;


    private boolean audioPreparing = false;
    private boolean manualAudioPlayback = false;
    private boolean userSeekingAudio = false;
    private final Runnable updateMusicPlayerRunnable = new Runnable() {
        @Override public void run() {
            if (!running) return;
            updateMusicPlayerUi();
            handler.postDelayed(this, 500L);
        }
    };
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
        if (!homeVisible && !settingsManager.videoMode) {
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

        homeVisible = false;
        homePanel.getView().setVisibility(View.GONE);
        homeButton.setVisibility(View.VISIBLE);
        settingsButton.setVisibility(View.VISIBLE);
        musicButton.setVisibility(View.VISIBLE);

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
            clockTextView.setVisibility(View.GONE);
            dateTextView.setVisibility(View.GONE);
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

        videoControlsPanel = new VideoControlsPanel(this, new VideoControlsPanel.Listener() {
            @Override public void onPlayPause() {
                videoBackgroundManager.togglePlayback();
                updateVideoControls();
                showVideoControls();
            }
            @Override public void onPrevious() {
                videoBackgroundManager.playPreviousManually();
                updateVideoControls();
                showVideoControls();
            }
            @Override public void onNext() {
                videoBackgroundManager.playNextManually();
                updateVideoControls();
                showVideoControls();
            }
            @Override public void onSeekStart() {
                userSeekingVideo = true;
            }
            @Override public void onSeekChanged(int progress, boolean fromUser) {
                if (fromUser) {
                    int duration = videoBackgroundManager.getDuration();
                    videoControlsPanel.setPositionText(
                            formatVideoTime((int) ((long) duration * progress / 1000L)),
                            formatVideoTime(duration));
                }
            }
            @Override public void onSeekStop(int progress) {
                int duration = videoBackgroundManager.getDuration();
                if (duration > 0) videoBackgroundManager.seekTo((int) ((long) duration * progress / 1000L));
                userSeekingVideo = false;
                updateVideoControls();
                showVideoControls();
            }
        });
        videoControlsPanel.getView().setVisibility(View.GONE);
        rootLayout.addView(videoControlsPanel.getView(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        videoView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (videoControlsPanel.getView().getVisibility() == View.VISIBLE) {
                    handler.removeCallbacks(hideVideoControlsRunnable);
                    videoControlsPanel.getView().setVisibility(View.GONE);
                } else {
                    showVideoControls();
                }
            }
        });

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

        homeButton = new Button(this);
        homeButton.setText("ホーム");
        UiStyle.styleSecondaryButton(homeButton);
        homeButton.setVisibility(View.GONE);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showHome();
            }
        });
        FrameLayout.LayoutParams homeButtonParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP | Gravity.LEFT
                );
        homeButtonParams.setMargins(12, 12, 0, 0);
        rootLayout.addView(homeButton, homeButtonParams);

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
        createHomePanel();
        createCreativeDisplay();

        setContentView(rootLayout);
        showHome();
    }

    private void createCreativeDisplay() {
        creativeDisplayView = new CreativeDisplayView(this);
        creativeDisplayView.setVisibility(View.GONE);
        rootLayout.addView(creativeDisplayView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void showCreativeDisplay(int mode) {
        homeVisible = false;
        settingsVisible = false;
        musicVisible = false;
        creativeDisplayVisible = true;
        manualAudioPlayback = false;
        stopAudioPlayback();
        slideshowManager.stopTimer();
        videoBackgroundManager.stop();
        handler.removeCallbacks(updateVideoControlsRunnable);
        handler.removeCallbacks(hideVideoControlsRunnable);

        if (homePanel != null) homePanel.getView().setVisibility(View.GONE);
        if (settingsPanel != null) settingsPanel.getView().setVisibility(View.GONE);
        if (musicPanel != null) musicPanel.getView().setVisibility(View.GONE);
        if (videoControlsPanel != null) videoControlsPanel.getView().setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        videoView.setVisibility(View.GONE);
        statusTextView.setVisibility(View.GONE);
        clockTextView.setVisibility(View.GONE);
        dateTextView.setVisibility(View.GONE);
        settingsButton.setVisibility(View.GONE);
        musicButton.setVisibility(View.GONE);
        homeButton.setVisibility(View.VISIBLE);

        creativeDisplayView.setMode(mode);
        creativeDisplayView.setVisibility(View.VISIBLE);
        creativeDisplayView.bringToFront();
        homeButton.bringToFront();
    }

    private void leaveCreativeDisplay() {
        creativeDisplayVisible = false;
        if (creativeDisplayView != null) {
            creativeDisplayView.setVisibility(View.GONE);
        }
        showHome();
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

    private void createHomePanel() {
        homePanel = new Home(this, new Home.Listener() {
            @Override
            public void onVideoSelected() {
                settingsManager.videoMode = true;
                saveSettings();
                hideHome();
                applyDisplayMode();
            }

            @Override
            public void onMusicSelected() {
                hideHome();
                showMusicLibrary();
            }

            @Override
            public void onImagesSelected() {
                settingsManager.videoMode = false;
                saveSettings();
                hideHome();
                applyDisplayMode();
            }

            @Override
            public void onSettingsSelected() {
                hideHome();
                showSettings();
            }

            @Override
            public void onVisualizerSelected() {
                showCreativeDisplay(CreativeDisplayView.MODE_VISUALIZER);
            }

            @Override
            public void onFullscreenClockSelected() {
                showCreativeDisplay(CreativeDisplayView.MODE_CLOCK);
            }
        });
        rootLayout.addView(
                homePanel.getView(),
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                )
        );
    }

    private void showHome() {
        creativeDisplayVisible = false;
        if (creativeDisplayView != null) creativeDisplayView.setVisibility(View.GONE);
        homeVisible = true;
        settingsVisible = false;
        musicVisible = false;

        if (settingsPanel != null) {
            settingsPanel.getView().setVisibility(View.GONE);
        }
        if (musicPanel != null) {
            musicPanel.getView().setVisibility(View.GONE);
        }
        if (videoBackgroundManager != null) {
            videoBackgroundManager.stop();
        }
        handler.removeCallbacks(updateVideoControlsRunnable);
        handler.removeCallbacks(hideVideoControlsRunnable);
        if (videoControlsPanel != null) videoControlsPanel.getView().setVisibility(View.GONE);
        if (slideshowManager != null) {
            slideshowManager.stopTimer();
        }
        manualAudioPlayback = false;
        stopAudioPlayback();

        if (homePanel != null) {
            homePanel.getView().setVisibility(View.VISIBLE);
            homePanel.getView().bringToFront();
        }
        if (homeButton != null) {
            homeButton.setVisibility(View.GONE);
        }
        if (settingsButton != null) {
            settingsButton.setVisibility(View.GONE);
        }
        if (musicButton != null) {
            musicButton.setVisibility(View.GONE);
        }
        updateClock();
        if (running && settingsManager.audioEnabled) {
            startAudioPlayback();
        }
    }

    private void hideHome() {
        homeVisible = false;
        if (homePanel != null) {
            homePanel.getView().setVisibility(View.GONE);
        }
        if (homeButton != null) {
            homeButton.setVisibility(View.VISIBLE);
            homeButton.bringToFront();
        }
        if (settingsButton != null) {
            settingsButton.setVisibility(View.VISIBLE);
            settingsButton.bringToFront();
        }
        if (musicButton != null) {
            musicButton.setVisibility(View.VISIBLE);
            musicButton.bringToFront();
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
                    public void onSelectTrack(int index) { selectAudioTrack(index); }
                    @Override
                    public void onPlayPause() { toggleAudioPlayback(); }
                    @Override
                    public void onPrevious() { playAdjacentTrack(-1); }
                    @Override
                    public void onNext() { playAdjacentTrack(1); }
                    @Override
                    public void onSeekStart() { userSeekingAudio = true; }
                    @Override
                    public void onSeekChanged(int progress, boolean fromUser) {
                        if (fromUser) {
                            int duration = getAudioDuration();
                            musicPanel.setPositionText(formatVideoTime((int) ((long) duration * progress / 1000L)),
                                    formatVideoTime(duration));
                        }
                    }
                    @Override
                    public void onSeekStop(int progress) {
                        int duration = getAudioDuration();
                        if (duration > 0 && mediaPlayer != null && !audioPreparing) {
                            try { mediaPlayer.seekTo((int) ((long) duration * progress / 1000L)); }
                            catch (IllegalStateException ignored) { }
                        }
                        userSeekingAudio = false;
                        updateMusicPlayerUi();
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
        manualAudioPlayback = false;
        stopAudioPlayback();

        slideshowManager.stopTimer();

        loadAudioFiles();
        updateMusicLibrary();

        musicPanel.getView().setVisibility(View.VISIBLE);
        musicPanel.getView().bringToFront();
    }

    private void hideMusicLibrary() {
        musicVisible = false;
        musicPanel.getView().setVisibility(View.GONE);
        showHome();
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
        if (running) {
            manualAudioPlayback = true;
            playAudioAtIndex(audioIndex);
        }
    }

    private void updateNowPlayingLabel() {
        if (musicPanel.nowPlayingText == null) return;
        if (settingsManager.selectedAudioName == null || settingsManager.selectedAudioName.length() == 0) {
            musicPanel.nowPlayingText.setText("NOW PLAYING  •  選択中の曲：なし");
        } else {
            musicPanel.nowPlayingText.setText("NOW PLAYING  •  " + settingsManager.selectedAudioName);
        }
        updateMusicPlayerUi();
    }

    private void toggleAudioPlayback() {
        if (mediaPlayer != null && !audioPreparing) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    manualAudioPlayback = true;
                } else {
                    manualAudioPlayback = true;
                    mediaPlayer.start();
                }
            } catch (IllegalStateException ignored) { }
            updateMusicPlayerUi();
            return;
        }
        loadAudioFiles();
        if (audioFiles.isEmpty()) {
            updateMusicLibrary();
            return;
        }
        manualAudioPlayback = true;
        int selected = findAudioIndex(settingsManager.selectedAudioName);
        if (selected >= 0) audioIndex = selected;
        playAudioAtIndex(audioIndex);
    }

    private void playAdjacentTrack(int direction) {
        loadAudioFiles();
        if (audioFiles.isEmpty()) return;
        int current = findAudioIndex(settingsManager.selectedAudioName);
        if (current < 0) current = audioIndex;
        int next = (current + direction + audioFiles.size()) % audioFiles.size();
        manualAudioPlayback = true;
        playAudioAtIndex(next);
        updateMusicLibrary();
    }

    private int getAudioDuration() {
        try { return mediaPlayer != null && !audioPreparing ? mediaPlayer.getDuration() : 0; }
        catch (IllegalStateException ignored) { return 0; }
    }

    private int getAudioPosition() {
        try { return mediaPlayer != null && !audioPreparing ? mediaPlayer.getCurrentPosition() : 0; }
        catch (IllegalStateException ignored) { return 0; }
    }

    private void updateMusicPlayerUi() {
        if (musicPanel == null) return;
        boolean playing = false;
        try { playing = mediaPlayer != null && !audioPreparing && mediaPlayer.isPlaying(); }
        catch (IllegalStateException ignored) { }
        musicPanel.setTrack(settingsManager.selectedAudioName, playing);
        int duration = getAudioDuration();
        int position = getAudioPosition();
        musicPanel.setPositionText(formatVideoTime(position), formatVideoTime(duration));
        if (!userSeekingAudio) {
            musicPanel.setProgress(duration > 0 ? (int) ((long) position * 1000L / duration) : 0);
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
                            if (!settingsManager.videoMode && !musicVisible) {
                                startAudioPlayback();
                            }
                        } else if (!manualAudioPlayback) {
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
        showHome();
    }

    private void updateSettingsLabels() {
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
            manualAudioPlayback = false;
            stopAudioPlayback();
            settingsButton.setVisibility(View.GONE);
            musicButton.setVisibility(View.GONE);

            // The standby TextView is layered above the VideoView.
            // Hide it while video mode is active so it cannot cover the video.
            statusTextView.setVisibility(View.GONE);
            clockTextView.setVisibility(View.GONE);
            dateTextView.setVisibility(View.GONE);

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
            showVideoControls();
            handler.removeCallbacks(updateVideoControlsRunnable);
            handler.post(updateVideoControlsRunnable);
        } else {
            videoBackgroundManager.stop();
            if (!homeVisible) {
                settingsButton.setVisibility(View.VISIBLE);
                musicButton.setVisibility(View.VISIBLE);
            }
            handler.removeCallbacks(updateVideoControlsRunnable);
            handler.removeCallbacks(hideVideoControlsRunnable);
            if (videoControlsPanel != null) videoControlsPanel.getView().setVisibility(View.GONE);
            videoView.setVisibility(View.GONE);
            clockTextView.setVisibility(View.VISIBLE);
            dateTextView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.VISIBLE);
            slideshowManager.showCurrentImageOrStandby();
            slideshowManager.scheduleNextImage();

            if (running && settingsManager.audioEnabled && !musicVisible) {
                startAudioPlayback();
            }
        }
    }

    private void showVideoControls() {
        if (videoControlsPanel == null || !settingsManager.videoMode || homeVisible || settingsVisible || musicVisible) return;
        videoControlsPanel.getView().setVisibility(View.VISIBLE);
        videoControlsPanel.getView().bringToFront();
        homeButton.bringToFront();
        settingsButton.bringToFront();
        musicButton.bringToFront();
        updateVideoControls();
        handler.removeCallbacks(hideVideoControlsRunnable);
        handler.postDelayed(hideVideoControlsRunnable, 4500L);
    }

    private void updateVideoControls() {
        if (videoControlsPanel == null || videoBackgroundManager == null) return;
        String name = videoBackgroundManager.getCurrentVideoName();
        videoControlsPanel.setTitle(name == null ? "動画を読み込み中…" : name);
        videoControlsPanel.setPlayPauseText(videoBackgroundManager.isPlaying() ? "❚❚" : "▶");
        int duration = videoBackgroundManager.getDuration();
        int position = videoBackgroundManager.getCurrentPosition();
        videoControlsPanel.setPositionText(formatVideoTime(position), formatVideoTime(duration));
        if (!userSeekingVideo) {
            videoControlsPanel.setProgress(duration > 0 ? (int) ((long) position * 1000L / duration) : 0);
        }
    }

    private String formatVideoTime(int millis) {
        if (millis < 0) millis = 0;
        int totalSeconds = millis / 1000;
        return String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private void updateClock() {
        Date now = new Date();
        String time = timeFormat.format(now);
        String date = dateFormat.format(now);

        clockTextView.setText(time);
        dateTextView.setText(date);
        if (homePanel != null) {
            homePanel.updateClock(time, date);
        }
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
    private boolean shouldPlayBackgroundMusic() {
        return running
                && settingsManager.audioEnabled
                && (homeVisible || !settingsManager.videoMode)
                && !musicVisible;
    }

    private boolean shouldPlayAudio() {
        return manualAudioPlayback || shouldPlayBackgroundMusic();
    }

    private void startAudioPlayback() {
        if (!shouldPlayAudio()) {
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
        if (!shouldPlayAudio()) {
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
                                    || !shouldPlayAudio()) {
                                return;
                            }

                            audioPreparing = false;

                            try {
                                player.start();
                                updateNowPlayingLabel();
                                updateMusicPlayerUi();
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

        if (!shouldPlayAudio()) {
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
        if (creativeDisplayVisible) {
            leaveCreativeDisplay();
        } else if (settingsVisible) {
            hideSettings();
        } else if (musicVisible) {
            hideMusicLibrary();
        } else if (!homeVisible) {
            showHome();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        running = true;

        handler.removeCallbacks(clockRunnable);
        handler.removeCallbacks(updateVideoControlsRunnable);
        slideshowManager.stopTimer();

        loadSettings();
        updateClock();

        loadAudioFiles();

        if (!homeVisible) {
            if (settingsManager.videoMode) {
                applyDisplayMode();
            } else {
                slideshowManager.showFirstImageOrStandby();
                slideshowManager.scheduleNextImage();
            }
        }

        updateSettingsLabels();
        updateMusicLibrary();

        handler.postDelayed(clockRunnable, CLOCK_INTERVAL);
        handler.removeCallbacks(updateMusicPlayerRunnable);
        handler.post(updateMusicPlayerRunnable);

        if (shouldPlayBackgroundMusic()) {
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
        handler.removeCallbacks(updateVideoControlsRunnable);
        handler.removeCallbacks(hideVideoControlsRunnable);
        handler.removeCallbacks(updateMusicPlayerRunnable);
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
        handler.removeCallbacks(updateVideoControlsRunnable);
        handler.removeCallbacks(hideVideoControlsRunnable);
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
