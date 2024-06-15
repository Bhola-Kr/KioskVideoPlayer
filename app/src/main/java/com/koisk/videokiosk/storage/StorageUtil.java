package com.koisk.videokiosk.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class for getting all storage directories in an Android device
 * <a href="https://stackoverflow.com/a/40582634/3940133">Solution of this problem</a>
 * Consider using
 * <a href="https://developer.android.com/guide/topics/providers/document-provider">Storage Access Framework (SAF)</a>
 * if your min SDK version is 19 and your requirement is just for browsing and opening documents, images, and other files.
 */
public class StorageUtil {

    // Primary physical SD-CARD (not emulated)
    private static final String EXTERNAL_STORAGE = System.getenv("EXTERNAL_STORAGE");

    // All Secondary SD-CARDs (all exclude primary) separated by File.pathSeparator, i.e: ":", ";"
    private static final String SECONDARY_STORAGES = System.getenv("SECONDARY_STORAGE");

    // Primary emulated SD-CARD
    private static final String EMULATED_STORAGE_TARGET = System.getenv("EMULATED_STORAGE_TARGET");

    // PhysicalPaths based on phone model
    @SuppressLint("SdCardPath")
    @SuppressWarnings("SpellCheckingInspection")
    private static final String[] KNOWN_PHYSICAL_PATHS = new String[]{
            "/storage/sdcard0",
            "/storage/sdcard1",                 // Motorola Xoom
            "/storage/extsdcard",               // Samsung SGS3
            "/storage/sdcard0/external_sdcard", // User request
            "/mnt/extsdcard",
            "/mnt/sdcard/external_sd",          // Samsung galaxy family
            "/mnt/sdcard/ext_sd",
            "/mnt/external_sd",
            "/mnt/media_rw/sdcard1",            // 4.4.2 on CyanogenMod S3
            "/removable/microsd",               // Asus transformer prime
            "/mnt/emmc",
            "/storage/ext_sd",                  // HTC One Max
            "/storage/external_SD",
            Environment.getExternalStorageDirectory().getAbsolutePath(),
            Environment.getRootDirectory().getAbsolutePath(),
            Environment.DIRECTORY_MOVIES,
            "/storage/removable/sdcard1",       // Sony Xperia Z1
            "/data/sdext",
            "/data/sdext2",
            "/data/sdext3",
            "/data/sdext4",
            "/sdcard1",                         // Sony Xperia Z
            "/sdcard2",                         // HTC One M8s
            "/Local",
            "/Device",
            "/storage/microsd"                  // ASUS ZenFone 2
    };

    /**
     * Returns all available storage directories in the system (including emulated storage).
     *
     * @param context The context to access the system services.
     * @return Paths to all available storage directories in the system (including emulated storage).
     */
    public static String[] getStorageDirectories(Context context) {
        // Final set of paths
        final Set<String> availableDirectoriesSet = new HashSet<>();

        if (!TextUtils.isEmpty(EMULATED_STORAGE_TARGET)) {
            // Device has an emulated storage
            availableDirectoriesSet.add(getEmulatedStorageTarget());
        } else {
            // Device doesn't have an emulated storage
            availableDirectoriesSet.addAll(getExternalStorage(context));
        }

        // Add all secondary storages
        Collections.addAll(availableDirectoriesSet, getAllSecondaryStorages());

        String[] storagesArray = new String[availableDirectoriesSet.size()];
        return availableDirectoriesSet.toArray(storagesArray);
    }

    private static Set<String> getExternalStorage(Context context) {
        final Set<String> availableDirectoriesSet = new HashSet<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Solution for empty raw emulated storage for android version >= marshmallow
            // because the EXTERNAL_STORAGE becomes something like: "/Storage/A5F9-15F4",
            // so we can't access it directly
            File[] files = getExternalFilesDirs(context, null);
            for (File file : files) {
                if (file != null) {
                    String applicationSpecificAbsolutePath = file.getAbsolutePath();
                    String rootPath = applicationSpecificAbsolutePath.substring(
                            0,
                            applicationSpecificAbsolutePath.indexOf("Android/data")
                    );
                    availableDirectoriesSet.add(rootPath);
                }
            }
        } else {
            if (TextUtils.isEmpty(EXTERNAL_STORAGE)) {
                availableDirectoriesSet.addAll(getAvailablePhysicalPaths());
            } else {
                // Device has physical external storage; use plain paths.
                availableDirectoriesSet.add(EXTERNAL_STORAGE);
            }
        }
        return availableDirectoriesSet;
    }

    private static String getEmulatedStorageTarget() {
        String rawStorageId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // External storage paths should have storageId in the last segment
            // i.e: "/storage/emulated/storageId" where storageId is 0, 1, 2, ...
            final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            final String[] folders = path.split(File.separator);
            final String lastSegment = folders[folders.length - 1];
            if (!TextUtils.isEmpty(lastSegment) && TextUtils.isDigitsOnly(lastSegment)) {
                rawStorageId = lastSegment;
            }
        }

        if (TextUtils.isEmpty(rawStorageId)) {
            return EMULATED_STORAGE_TARGET;
        } else {
            return EMULATED_STORAGE_TARGET + File.separator + rawStorageId;
        }
    }

    private static String[] getAllSecondaryStorages() {
        if (!TextUtils.isEmpty(SECONDARY_STORAGES)) {
            // All Secondary SD-CARDs split into array
            return SECONDARY_STORAGES.split(File.pathSeparator);
        }
        return new String[0];
    }

    /**
     * Filter available physical paths from known physical paths
     *
     * @return List of available physical paths from current device
     */
    private static List<String> getAvailablePhysicalPaths() {
        List<String> availablePhysicalPaths = new ArrayList<>();
        for (String physicalPath : KNOWN_PHYSICAL_PATHS) {
            File file = new File(physicalPath);
            if (file.exists()) {
                availablePhysicalPaths.add(physicalPath);
            }
        }
        return availablePhysicalPaths;
    }

    /**
     * Returns absolute paths to application-specific directories on all
     * external storage devices where the application can place persistent files
     * it owns. These files are internal to the application, and not typically
     * visible to the user as media.
     * <p>
     * This is like {@link Context#getFilesDir()} in that these files will be
     * deleted when the application is uninstalled, however there are some
     * important differences:
     * <ul>
     * <li>External files are not always available: they will disappear if the
     * user mounts the external storage on a computer or removes it.
     * <li>There is no security enforced with these files.
     * </ul>
     * <p>
     * External storage devices returned here are considered a permanent part of
     * the device, including both emulated external storage and physical media
     * slots, such as SD cards in a battery compartment. The returned paths do
     * not include transient devices, such as USB flash drives.
     * <p>
     * An application may store data on any or all of the returned devices. For
     * example, an app may choose to store large files on the device with the
     * most available space, as measured by {@link android.os.StatFs}.
     * <p>
     * Starting in {@link Build.VERSION_CODES#KITKAT}, no permissions
     * are required to write to the returned paths; they're always accessible to
     * the calling app. Before then,
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE} is required to
     * write. Write access outside of these paths on secondary external storage
     * devices is not available. To request external storage access in a
     * backwards compatible way, consider using {@code android:maxSdkVersion}
     * like this:
     *
     * <pre class="prettyprint">&lt;uses-permission
     *     android:name="android.permission.WRITE_EXTERNAL_STORAGE"
     *     android:maxSdkVersion="18" /&gt;</pre>
     * <p>
     * The first path returned is the same as
     * {@link Context#getExternalFilesDir(String)}. Returned paths may be
     * {@code null} if a storage device is unavailable.
     *
     * @see Context#getExternalFilesDir(String)
     */
    private static File[] getExternalFilesDirs(Context context, String type) {
        return context.getExternalFilesDirs(type);
    }

    /**
     * Recursively lists media files (images and videos) in a given directory.
     *
     * @param dir The directory to list media files from.
     */
    private static void listMediaFiles(DocumentFile dir) {
        try {
            DocumentFile[] files = dir.listFiles();
            if (files != null) {
                for (DocumentFile file : files) {
                    if (file.isDirectory()) {
                        listMediaFiles(file); // Recursive call for directories
                    } else {
                        String fileType = file.getType();
                        if (fileType != null && (fileType.startsWith("image/") || fileType.startsWith("video/"))) {
                            LocalData.allMediaList.add(new File(file.getUri().getPath()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.d("TAG", "listMediaFiles: " + e.getLocalizedMessage());
        }
    }

    /**
     * Reads media files from all available storage directories.
     *
     * @param context The context to access the system services.
     */
    public static void readFilesFromFolder(Context context) {
        try {
            String[] storageDirectories = getStorageDirectories(context);
            for (String storageDirectory : storageDirectories) {
                File storageDirFile = new File(storageDirectory);
                if (storageDirFile.exists() && storageDirFile.isDirectory()) {
                    // Convert File to DocumentFile
                    DocumentFile documentFile = DocumentFile.fromFile(storageDirFile);
                    listMediaFiles(documentFile);
                }
            }
        } catch (Exception e) {
            Log.d("TAG", "readFilesFromFolder: " + e.getLocalizedMessage());
        }
    }
}
