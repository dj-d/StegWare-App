package com.android.stegware_app.service;

import android.os.Environment;
import android.util.Log;

import java.io.File;

public class MediaSearchService {
    public static final String TAG = "MediaSearchJob";

    public static final String IMAGE_TO_FIND_NAME = "StegWare_IMG.jpg";

    public String getMediaPath() {
        String downloadDirectoryPath = Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOWNLOADS;
        Log.d(TAG, "Path: " + downloadDirectoryPath);
        File directory = new File(downloadDirectoryPath);
        File[] files = directory.listFiles();

        assert files != null;

        Log.d(TAG, "Size: " + files.length);

        for (File file : files) {
            String fileName = file.getName();

            Log.d(TAG, "FileName:" + fileName);

            if (fileName.equals(IMAGE_TO_FIND_NAME)) {
                Log.d(TAG, "FoundImage:" + fileName);
                Log.d(TAG, file.getAbsolutePath());

                return file.getAbsolutePath();
            } else {
                Log.d(TAG, "Image not found");
            }
        }

        return "";
    }

    public void removeMediaByPath(String absolutePath) {
        File file = new File(absolutePath);
        boolean deleted = file.delete();
        if (!deleted) {
            Log.d(TAG, "Not deleted");
        } else {
            Log.d(TAG, "Deleted!");
        }

    }
}
