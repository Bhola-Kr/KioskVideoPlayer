package com.koisk.videokiosk.storage;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class StorageUtil {

    private static final String TAG = "StorageUtil";

    // ✅ Limits to prevent ANR / freeze
    private static final int MAX_FILES = 3000;      // Stop after 3000 media files
    private static final int MAX_DEPTH = 12;        // Stop deep recursion
    private static final long MAX_SCAN_TIME_MS = 8000; // Stop scanning after 8 seconds

    // ✅ Supported extensions (fast check)
    private static final String[] IMAGE_EXT = {".jpg", ".jpeg", ".png", ".webp", ".gif"};
    private static final String[] VIDEO_EXT = {".mp4", ".mkv", ".3gp", ".webm", ".mov", ".avi"};

    /**
     * Reads media files from common folders only (FAST + SAFE)
     */
    public static void readFilesFromFolder(Context context) {
        try {
            long startTime = System.currentTimeMillis();

            List<File> roots = new ArrayList<>();

            // Primary storage root
            File primary = Environment.getExternalStorageDirectory();
            if (primary != null && primary.exists()) {
                roots.add(new File(primary, "DCIM"));
                roots.add(new File(primary, "Pictures"));
                roots.add(new File(primary, "Movies"));
                roots.add(new File(primary, "Download"));
            }

            // Also scan app-specific external directory (safe)
            File appExternal = context.getExternalFilesDir(null);
            if (appExternal != null && appExternal.exists()) {
                roots.add(appExternal);
            }

            // Clear old list
            if (LocalData.allMediaList != null) {
                LocalData.allMediaList.clear();
            }

            for (File root : roots) {
                if (root != null && root.exists() && root.isDirectory()) {
                    scanFolderIterative(root, startTime);
                }

                // Stop early if too many files found
                if (LocalData.allMediaList.size() >= MAX_FILES) {
                    break;
                }

                // Stop early if scan time exceeded
                if (System.currentTimeMillis() - startTime > MAX_SCAN_TIME_MS) {
                    break;
                }
            }

            Log.d(TAG, "Scan done. Total media: " + LocalData.allMediaList.size());

        } catch (Exception e) {
            Log.d(TAG, "readFilesFromFolder error: " + e.getLocalizedMessage());
        }
    }

    /**
     * Iterative scan (NO recursion) => avoids stack overflow and deep recursion ANR
     */
    private static void scanFolderIterative(File root, long startTime) {
        try {
            Deque<FolderNode> stack = new ArrayDeque<>();
            stack.push(new FolderNode(root, 0));

            while (!stack.isEmpty()) {

                // Stop conditions
                if (LocalData.allMediaList.size() >= MAX_FILES) return;
                if (System.currentTimeMillis() - startTime > MAX_SCAN_TIME_MS) return;

                FolderNode node = stack.pop();
                File dir = node.dir;
                int depth = node.depth;

                if (dir == null || !dir.exists() || !dir.isDirectory()) continue;

                // Depth limit
                if (depth > MAX_DEPTH) continue;

                // Skip hidden/system folders
                String name = dir.getName();
                if (name != null && name.startsWith(".")) continue;
                if (dir.getAbsolutePath().contains("/Android/data")) continue;
                if (dir.getAbsolutePath().contains("/Android/obb")) continue;

                File[] files = dir.listFiles();
                if (files == null) continue;

                for (File f : files) {
                    if (f == null) continue;

                    if (f.isDirectory()) {
                        stack.push(new FolderNode(f, depth + 1));
                    } else {
                        if (isSupportedMediaFile(f)) {
                            LocalData.allMediaList.add(f);

                            // Stop early
                            if (LocalData.allMediaList.size() >= MAX_FILES) return;
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.d(TAG, "scanFolderIterative error: " + e.getLocalizedMessage());
        }
    }

    private static boolean isSupportedMediaFile(File file) {
        if (file == null) return false;

        String path = file.getAbsolutePath();
        if (path == null) return false;

        String lower = path.toLowerCase();

        String support = LocalData.getSupportMedia(); // IMAGE / VIDEO / BOTH
        if (support == null) support = "BOTH";

        if (support.equalsIgnoreCase("IMAGE")) {
            return hasAnyExtension(lower, IMAGE_EXT);
        } else if (support.equalsIgnoreCase("VIDEO")) {
            return hasAnyExtension(lower, VIDEO_EXT);
        } else {
            return hasAnyExtension(lower, IMAGE_EXT) || hasAnyExtension(lower, VIDEO_EXT);
        }
    }

    private static boolean hasAnyExtension(String filePath, String[] exts) {
        for (String ext : exts) {
            if (filePath.endsWith(ext)) return true;
        }
        return false;
    }

    private static class FolderNode {
        File dir;
        int depth;

        FolderNode(File dir, int depth) {
            this.dir = dir;
            this.depth = depth;
        }
    }
}
