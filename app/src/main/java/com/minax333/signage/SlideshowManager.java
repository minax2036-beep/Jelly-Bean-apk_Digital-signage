package com.minax333.signage;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Owns slideshow file discovery, image decoding, display and scheduling.
 * Compatible with Android API 17.
 */
public final class SlideshowManager {

    public interface StateProvider {
        boolean isRunning();
        boolean isSettingsVisible();
        boolean isMusicVisible();
    }

    private final Activity activity;
    private final Handler handler;
    private final SettingsManager settingsManager;
    private final ImageView imageView;
    private final TextView statusTextView;
    private final TextView clockTextView;
    private final TextView dateTextView;
    private final StateProvider stateProvider;
    private final List<File> imageFiles = new ArrayList<File>();

    private int imageIndex = 0;

    private final Runnable imageRunnable = new Runnable() {
        @Override
        public void run() {
            if (!canAdvance()) {
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

    public SlideshowManager(
            Activity activity,
            Handler handler,
            SettingsManager settingsManager,
            ImageView imageView,
            TextView statusTextView,
            TextView clockTextView,
            TextView dateTextView,
            StateProvider stateProvider
    ) {
        this.activity = activity;
        this.handler = handler;
        this.settingsManager = settingsManager;
        this.imageView = imageView;
        this.statusTextView = statusTextView;
        this.clockTextView = clockTextView;
        this.dateTextView = dateTextView;
        this.stateProvider = stateProvider;
    }

    public void loadImages() {
        imageFiles.clear();

        List<File> directories = MediaFileUtils.findDirectories("images");

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
                if (file.isFile() && MediaFileUtils.isImageFile(file)
                        && !imageFiles.contains(file)) {
                    imageFiles.add(file);
                }
            }
        }

        if (imageIndex >= imageFiles.size()) {
            imageIndex = 0;
        }
    }

    public void showFirstImageOrStandby() {
        loadImages();

        if (imageFiles.isEmpty()) {
            showStandby();
        } else {
            imageIndex = 0;
            showImage(imageFiles.get(imageIndex));
        }
    }

    public void showCurrentImageOrStandby() {
        loadImages();

        if (imageFiles.isEmpty()) {
            showStandby();
        } else {
            if (imageIndex >= imageFiles.size()) {
                imageIndex = 0;
            }

            showImage(imageFiles.get(imageIndex));
        }
    }

    public void restartTimer() {
        handler.removeCallbacks(imageRunnable);
        scheduleNextImage();
    }

    public void scheduleNextImage() {
        handler.removeCallbacks(imageRunnable);

        if (canSchedule()) {
            handler.postDelayed(
                    imageRunnable,
                    settingsManager.slideshowSeconds * 1000L
            );
        }
    }

    public void stopTimer() {
        handler.removeCallbacks(imageRunnable);
    }

    public void clearDisplayedImage() {
        stopTimer();
        imageView.setImageDrawable(null);
    }

    private boolean canAdvance() {
        return stateProvider.isRunning()
                && !stateProvider.isSettingsVisible()
                && !stateProvider.isMusicVisible()
                && settingsManager.slideshowEnabled;
    }

    private boolean canSchedule() {
        return canAdvance();
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
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

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
            return BitmapFactory.decodeStream(input, null, options);
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
}
