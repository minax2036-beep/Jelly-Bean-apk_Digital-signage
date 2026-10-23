
package com.minax333.signage;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

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

    private final Handler handler = new Handler();

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

    private final List<File> imageFiles = new ArrayList<File>();

    private int imageIndex = 0;
    private boolean running = false;

    private static final long CLOCK_INTERVAL = 1000L;
    private static final long IMAGE_INTERVAL = 10000L;

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
            if (!running) {
                return;
            }

            loadImages();

            if (!imageFiles.isEmpty()) {
                imageIndex++;

                if (imageIndex >= imageFiles.size()) {
                    imageIndex = 0;
                }

                showImage(imageFiles.get(imageIndex));
            }

            handler.postDelayed(this, IMAGE_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createLayout();

        updateClock();
        loadImages();

        if (imageFiles.isEmpty()) {
            showStandby();
        } else {
            imageIndex = 0;
            showImage(imageFiles.get(imageIndex));
        }
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

        setContentView(rootLayout);
    }

    private void updateClock() {
        Date now = new Date();

        clockTextView.setText(timeFormat.format(now));
        dateTextView.setText(dateFormat.format(now));
    }

    private void loadImages() {
        imageFiles.clear();

        List<File> directories = findImageDirectories();

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
                if (file.isFile() && isImageFile(file)) {
                    if (!imageFiles.contains(file)) {
                        imageFiles.add(file);
                    }
                }
            }
        }
    }

    private List<File> findImageDirectories() {
        List<File> result = new ArrayList<File>();
        List<File> roots = new ArrayList<File>();

        addRoot(roots, Environment.getExternalStorageDirectory());

        addRoot(roots, new File("/sdcard"));
        addRoot(roots, new File("/storage"));
        addRoot(roots, new File("/mnt"));
        addRoot(roots, new File("/mnt/media_rw"));

        for (File root : roots) {
            addImageDirectory(result, new File(root, "signage/images"));

            File[] children = root.listFiles();

            if (children == null) {
                continue;
            }

            for (File child : children) {
                if (child.isDirectory()) {
                    addImageDirectory(
                            result,
                            new File(child, "signage/images")
                    );
                }
            }
        }

        return result;
    }

    private void addRoot(List<File> roots, File root) {
        if (root != null && root.isDirectory() && !roots.contains(root)) {
            roots.add(root);
        }
    }

    private void addImageDirectory(
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

    @Override
    protected void onResume() {
        super.onResume();

        running = true;

        handler.removeCallbacks(clockRunnable);
        handler.removeCallbacks(imageRunnable);

        updateClock();
        loadImages();

        if (imageFiles.isEmpty()) {
            showStandby();
        } else {
            imageIndex = 0;
            showImage(imageFiles.get(imageIndex));
        }

        handler.postDelayed(clockRunnable, CLOCK_INTERVAL);
        handler.postDelayed(imageRunnable, IMAGE_INTERVAL);
    }

    @Override
    protected void onPause() {
        running = false;

        handler.removeCallbacks(clockRunnable);
        handler.removeCallbacks(imageRunnable);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        running = false;

        handler.removeCallbacks(clockRunnable);
        handler.removeCallbacks(imageRunnable);

        if (imageView != null) {
            imageView.setImageDrawable(null);
        }

        super.onDestroy();
    }
}
