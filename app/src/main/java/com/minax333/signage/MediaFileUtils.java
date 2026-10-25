package com.minax333.signage;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Finds media folders and checks supported media file extensions.
 * Kept separate from MainActivity to make the media layer easier to extend.
 */
public final class MediaFileUtils {

    private MediaFileUtils() {
    }

    public static List<File> findDirectories(String folderName) {
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

    private static void addRoot(List<File> roots, File root) {
            if (root != null
                    && root.isDirectory()
                    && !roots.contains(root)) {
                roots.add(root);
            }
        }

    private static void addMediaDirectory(
                List<File> directories,
                File directory
        ) {
            if (directory.isDirectory()
                    && directory.canRead()
                    && !directories.contains(directory)) {
                directories.add(directory);
            }
        }

    public static boolean isImageFile(File file) {
            String name = file.getName().toLowerCase(Locale.US);
    
            return name.endsWith(".jpg")
                    || name.endsWith(".jpeg")
                    || name.endsWith(".png")
                    || name.endsWith(".webp")
                    || name.endsWith(".bmp")
                    || name.endsWith(".gif");
        }

    public static boolean isAudioFile(File file) {
            String name = file.getName().toLowerCase(Locale.US);
    
            return name.endsWith(".mp3")
                    || name.endsWith(".m4a")
                    || name.endsWith(".aac")
                    || name.endsWith(".ogg")
                    || name.endsWith(".wav")
                    || name.endsWith(".3gp");
        }
}
