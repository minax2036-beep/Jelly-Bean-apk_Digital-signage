package com.minax333.signage;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;
import android.widget.VideoView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Plays videos from signage/videos in filename order.
 * Uses VideoView APIs available on Android 4.2.2 (API 17).
 */
public final class VideoBackgroundManager {

    public interface Listener {
        void onNoVideosFound();
    }

    private final Activity activity;
    private final VideoView videoView;
    private final Listener listener;
    private final List<File> videoFiles = new ArrayList<File>();

    private int videoIndex = 0;
    private int failedAttempts = 0;
    private boolean released = false;
    private boolean playbackRequested = false;
    private String currentVideoPath = null;

    public VideoBackgroundManager(
            Activity activity,
            VideoView videoView,
            Listener listener
    ) {
        this.activity = activity;
        this.videoView = videoView;
        this.listener = listener;

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer player) {
                playNextVideo();
            }
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer player, int what, int extra) {
                failedAttempts++;
                if (failedAttempts >= videoFiles.size()) {
                    stop();
                    VideoBackgroundManager.this.listener.onNoVideosFound();
                } else {
                    playNextVideo();
                }
                return true;
            }
        });

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {
                if (!released && videoView.getVisibility() == View.VISIBLE) {
                    failedAttempts = 0;
                    try {
                        player.setLooping(false);
                        videoView.start();
                    } catch (IllegalStateException ignored) {
                    }
                }
            }
        });
    }

    public void start() {
        released = false;
        loadVideos();

        if (videoFiles.isEmpty()) {
            videoView.stopPlayback();
            listener.onNoVideosFound();
            return;
        }

        if (videoIndex < 0 || videoIndex >= videoFiles.size()) {
            videoIndex = 0;
        }

        File selectedFile = videoFiles.get(videoIndex);
        if (playbackRequested
                && selectedFile.getAbsolutePath().equals(currentVideoPath)
                && videoView.getVisibility() == View.VISIBLE) {
            return;
        }

        failedAttempts = 0;
        playCurrentVideo();
    }

    public void stop() {
        released = true;
        playbackRequested = false;
        currentVideoPath = null;
        try {
            videoView.stopPlayback();
        } catch (IllegalStateException ignored) {
        }
    }

    public void release() {
        stop();
        videoView.setOnCompletionListener(null);
        videoView.setOnErrorListener(null);
        videoView.setOnPreparedListener(null);
    }

    private void loadVideos() {
        videoFiles.clear();

        List<File> directories = MediaFileUtils.findDirectories("videos");
        for (File directory : directories) {
            File[] files = directory.listFiles();
            if (files == null) {
                continue;
            }

            Arrays.sort(files, new java.util.Comparator<File>() {
                @Override
                public int compare(File first, File second) {
                    return first.getName().compareToIgnoreCase(second.getName());
                }
            });

            for (File file : files) {
                if (file.isFile()
                        && file.canRead()
                        && MediaFileUtils.isVideoFile(file)
                        && !videoFiles.contains(file)) {
                    videoFiles.add(file);
                }
            }
        }

        if (videoIndex >= videoFiles.size()) {
            videoIndex = 0;
        }
    }

    private void playCurrentVideo() {
        if (released || videoFiles.isEmpty()) {
            return;
        }

        File file = videoFiles.get(videoIndex);
        try {
            currentVideoPath = file.getAbsolutePath();
            playbackRequested = true;
            videoView.setVideoURI(Uri.fromFile(file));
            videoView.requestFocus();
            videoView.requestLayout();
            videoView.invalidate();
        } catch (IllegalArgumentException exception) {
            failedAttempts++;
            playNextVideo();
        }
    }

    private void playNextVideo() {
        if (released || videoFiles.isEmpty()) {
            return;
        }

        videoIndex++;
        if (videoIndex >= videoFiles.size()) {
            videoIndex = 0;
        }

        if (failedAttempts >= videoFiles.size()) {
            stop();
            listener.onNoVideosFound();
            return;
        }

        playCurrentVideo();
    }
}
