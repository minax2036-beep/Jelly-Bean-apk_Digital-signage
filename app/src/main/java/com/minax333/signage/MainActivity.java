
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
    private LinearLayout settingsPanel;
    private TextView slideshowStatusText;
    private TextView intervalStatusText;
    private TextView audioStatusText;

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

    private boolean running = false;
    private boolean settingsVisible = false;
    private boolean slideshowEnabled = true;
    private boolean audioEnabled = false;
    private boolean audioPreparing = false;

    private int slideshowSeconds = 10;

    private static final String PREFS_NAME = "signage_settings";
    private static final String KEY_SLIDESHOW_ENABLED =
            "slideshow_enabled";
    private static final String KEY_SLIDESHOW_SECONDS =
            "slideshow_seconds";
    private static final String KEY_AUDIO_ENABLED =
            "audio_enabled";

    private static final long CLOCK_INTERVAL = 1000L;

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
            if (!running || settingsVisible || !slideshowEnabled) {
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

        if (imageFiles.isEmpty()) {
            showStandby();
        } else {
            imageIndex = 0;
            showImage(imageFiles.get(imageIndex));
        }

        updateSettingsLabels();
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
                .apply();
    }

    private void createLayout() {
        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.BLACK);

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

        createSettingsPanel();
        setContentView(rootLayout);
    }

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
            );
        }
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

        if (running && !settingsVisible && slideshowEnabled) {
            scheduleNextImage();
        }
    }

    private void scheduleNextImage() {
        handler.removeCallbacks(imageRunnable);

        if (running && !settingsVisible && slideshowEnabled) {
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
    }

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

        if (audioIndex >= audioFiles.size()) {
            audioIndex = 0;
        }
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
            return;
        }

        if (audioIndex >= audioFiles.size()) {
            audioIndex = 0;
        }

        playAudioAtIndex(audioIndex);
    }

    private void playAudioAtIndex(int index) {
        if (!running || !audioEnabled || audioFiles.isEmpty()) {
            return;
        }

        if (index < 0 || index >= audioFiles.size()) {
            index = 0;
        }

        releaseAudioPlayer();

        final MediaPlayer player = new MediaPlayer();
        mediaPlayer = player;
        audioPreparing = true;
        audioIndex = index;

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

                            audioIndex++;

                            if (audioIndex >= audioFiles.size()) {
                                audioIndex = 0;
                            }

                            releaseAudioPlayer();
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

        if (!running || !audioEnabled || audioFiles.isEmpty()) {
            return;
        }

        audioIndex++;

        if (audioIndex >= audioFiles.size()) {
            audioIndex = 0;
        }

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
