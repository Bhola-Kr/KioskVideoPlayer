package com.koisk.videokiosk.storage;

import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

public class FilePathResolver {

    public static String getOriginalFilePath(Context context, Uri uri) {
        DocumentFile pickedDir = DocumentFile.fromTreeUri(context, uri);
        if (pickedDir != null && pickedDir.exists()) {
            // Traversing the directory structure of the SAF URI
            String path = resolveDocumentPath(context, pickedDir);
            return path;
        }
        return null;
    }

    private static String resolveDocumentPath(Context context, DocumentFile documentFile) {
        String filePath = null;
        DocumentFile parentFile = documentFile.getParentFile();
        if (parentFile != null) {
            String parentPath = resolveDocumentPath(context, parentFile);
            filePath = parentPath + "/" + documentFile.getName();
        } else {
            filePath = documentFile.getName();
        }
        return filePath;
    }
}
