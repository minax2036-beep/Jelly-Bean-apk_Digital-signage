package com.minax333.signage;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Plays videos from signage/videos in filename order.
 * Uses TextureView + MediaPlayer for Android 4.2.2 (API 17).
 */
public final class VideoBackgroundManager implements TextureView.SurfaceTextureListener {

    public interface Listener {
        void onNoVideosFound();
    }

    private final Activity activity;
    private final TextureView textureView;
    private final Listener listener;
    private final List<File> videoFiles = new ArrayList<File>();

    private int videoIndex = 0;
    private int failedAttempts = 0;
    private boolean playbackRequested = false;
    private boolean preparing = false;
    private boolean released = false;
    private String currentVideoPath = null;
    private MediaPlayer mediaPlayer;
    private Surface videoSurface;
    private SurfaceTexture surfaceTexture;
    private Uri externalUri;

    public VideoBackgroundManager(
            Activity activity,
            TextureView textureView,
            Listener listener
    ) {
        this.activity = activity;
        this.textureView = textureView;
        this.listener = listener;
        textureView.setSurfaceTextureListener(this);
    }

    public void playExternalUri(Uri uri) {
        if (uri == null) {
            return;
        }
        released = false;
        playbackRequested = true;
        externalUri = uri;
        currentVideoPath = null;
        releasePlayer();
        if (textureView.isAvailable()) {
            surfaceTexture = textureView.getSurfaceTexture();
            prepareCurrentVideo();
        }
    }

    public void start() {
        released = false;
        playbackRequested = true;
        if (externalUri != null) {
            if (textureView.isAvailable()) {
                surfaceTexture = textureView.getSurfaceTexture();
                prepareCurrentVideo();
            }
            return;
        }
        loadVideos();

        if (videoFiles.isEmpty()) {
            playbackRequested = false;
            releasePlayer();
            listener.onNoVideosFound();
            return;
        }

        if (videoIndex < 0 || videoIndex >= videoFiles.size()) {
            videoIndex = 0;
        }

        File selected = videoFiles.get(videoIndex);
        if (selected.getAbsolutePath().equals(currentVideoPath)
                && (preparing || (mediaPlayer != null && isPlayerPlayingSafely()))) {
            return;
        }

        if (textureView.isAvailable()) {
            surfaceTexture = textureView.getSurfaceTexture();
            prepareCurrentVideo();
        }
    }

    public void togglePlayback() {
        if (mediaPlayer == null || preparing) return;
        try {
            if (mediaPlayer.isPlaying()) mediaPlayer.pause();
            else mediaPlayer.start();
        } catch (IllegalStateException ignored) {
        }
    }

    public boolean isPlaying() {
        try {
            return mediaPlayer != null && mediaPlayer.isPlaying();
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    public int getDuration() {
        try {
            return mediaPlayer != null && !preparing ? mediaPlayer.getDuration() : 0;
        } catch (IllegalStateException ignored) {
            return 0;
        }
    }

    public int getCurrentPosition() {
        try {
            return mediaPlayer != null && !preparing ? mediaPlayer.getCurrentPosition() : 0;
        } catch (IllegalStateException ignored) {
            return 0;
        }
    }

    public void seekTo(int position) {
        try {
            if (mediaPlayer != null && !preparing) mediaPlayer.seekTo(Math.max(0, position));
        } catch (IllegalStateException ignored) {
        }
    }

    public String getCurrentVideoName() {
        if (externalUri != null) {
            String segment = externalUri.getLastPathSegment();
            return segment == null ? "外部動画" : segment;
        }
        if (videoFiles.isEmpty() || videoIndex < 0 || videoIndex >= videoFiles.size()) return null;
        return videoFiles.get(videoIndex).getName();
    }

    public void playNextManually() {
        if (externalUri != null) {
            releasePlayer();
            externalUri = null;
            currentVideoPath = null;
            loadVideos();
            if (!videoFiles.isEmpty()) {
                videoIndex = 0;
                prepareCurrentVideo();
            }
            return;
        }
        if (videoFiles.isEmpty()) loadVideos();
        if (videoFiles.isEmpty()) return;
        advanceVideoIndex();
        failedAttempts = 0;
        currentVideoPath = null;
        prepareCurrentVideo();
    }

    public void playPreviousManually() {
        if (externalUri != null) {
            releasePlayer();
            externalUri = null;
            currentVideoPath = null;
            loadVideos();
            if (!videoFiles.isEmpty()) {
                videoIndex = 0;
                prepareCurrentVideo();
            }
            return;
        }
        if (videoFiles.isEmpty()) loadVideos();
        if (videoFiles.isEmpty()) return;
        videoIndex--;
        if (videoIndex < 0) videoIndex = videoFiles.size() - 1;
        failedAttempts = 0;
        currentVideoPath = null;
        prepareCurrentVideo();
    }

    public void stop() {
        playbackRequested = false;
        externalUri = null;
        currentVideoPath = null;
        releasePlayer();
    }

    public void release() {
        stop();
        released = true;
        textureView.setSurfaceTextureListener(null);
        if (videoSurface != null) {
            videoSurface.release();
            videoSurface = null;
        }
    }

    private boolean isPlayerPlayingSafely() {
        try {
            return mediaPlayer != null && mediaPlayer.isPlaying();
        } catch (IllegalStateException ignored) {
            return false;
        }
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
                if (file.isFile() && file.canRead()
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

    private void prepareCurrentVideo() {
        if (released || !playbackRequested || preparing
                || surfaceTexture == null
                || (externalUri == null && videoFiles.isEmpty())) {
            return;
        }

        if (videoIndex < 0 || videoIndex >= videoFiles.size()) {
            videoIndex = 0;
        }

        final File file = externalUri == null ? videoFiles.get(videoIndex) : null;
        final Uri uri = externalUri;
        releasePlayer();
        currentVideoPath = uri == null ? file.getAbsolutePath() : uri.toString();

        try {
            videoSurface = new Surface(surfaceTexture);
            final MediaPlayer player = new MediaPlayer();
            mediaPlayer = player;
            preparing = true;

            player.setSurface(videoSurface);
            if (uri != null) {
                player.setDataSource(activity, uri);
            } else {
                player.setDataSource(file.getAbsolutePath());
            }
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    if (mediaPlayer != player || !playbackRequested || released) {
                        return;
                    }
                    preparing = false;
                    failedAttempts = 0;
                    try {
                        player.setLooping(false);
                        player.start();
                    } catch (IllegalStateException exception) {
                        handlePlaybackFailure();
                    }
                }
            });

            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (mediaPlayer == player) {
                        if (externalUri != null) {
                            externalUri = null;
                            currentVideoPath = null;
                            releasePlayer();
                            loadVideos();
                            if (videoFiles.isEmpty()) {
                                playbackRequested = false;
                                listener.onNoVideosFound();
                            } else {
                                prepareCurrentVideo();
                            }
                        } else {
                            playNextVideo();
                        }
                    }
                }
            });

            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    if (mediaPlayer == player) {
                        handlePlaybackFailure();
                    }
                    return true;
                }
            });

            player.prepareAsync();
        } catch (IOException exception) {
            handlePlaybackFailure();
        } catch (IllegalArgumentException exception) {
            handlePlaybackFailure();
        } catch (IllegalStateException exception) {
            handlePlaybackFailure();
        }
    }

    private void handlePlaybackFailure() {
        failedAttempts++;
        releasePlayer();

        if (!playbackRequested || released) {
            return;
        }

        if (failedAttempts >= videoFiles.size()) {
            playbackRequested = false;
            currentVideoPath = null;
            listener.onNoVideosFound();
        } else {
            advanceVideoIndex();
            prepareCurrentVideo();
        }
    }

    private void playNextVideo() {
        if (!playbackRequested || released || videoFiles.isEmpty()) {
            return;
        }
        failedAttempts = 0;
        advanceVideoIndex();
        currentVideoPath = null;
        prepareCurrentVideo();
    }

    private void advanceVideoIndex() {
        videoIndex++;
        if (videoIndex >= videoFiles.size()) {
            videoIndex = 0;
        }
    }

    private void releasePlayer() {
        preparing = false;

        if (mediaPlayer != null) {
            MediaPlayer oldPlayer = mediaPlayer;
            mediaPlayer = null;
            oldPlayer.setOnPreparedListener(null);
            oldPlayer.setOnCompletionListener(null);
            oldPlayer.setOnErrorListener(null);
            try {
                oldPlayer.stop();
            } catch (IllegalStateException ignored) {
            }
            oldPlayer.reset();
            oldPlayer.release();
        }

        if (videoSurface != null) {
            videoSurface.release();
            videoSurface = null;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(
            SurfaceTexture surface,
            int width,
            int height
    ) {
        surfaceTexture = surface;
        if (playbackRequested) {
            prepareCurrentVideo();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(
            SurfaceTexture surface,
            int width,
            int height
    ) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        surfaceTexture = null;
        releasePlayer();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}
