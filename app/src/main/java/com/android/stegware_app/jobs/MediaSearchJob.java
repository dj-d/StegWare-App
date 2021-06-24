package com.android.stegware_app.jobs;

import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.ayush.imagesteganographylibrary.Text.AsyncTaskCallback.TextDecodingCallback;
import com.ayush.imagesteganographylibrary.Text.ImageSteganography;

import java.io.File;

public class MediaSearchJob extends AppCompatActivity implements TextDecodingCallback {
    public static final String TAG = "MediaSearchJob";

    public static final String IMAGE_TO_FIND_NAME = "StegWare_IMG.jpg";

    @Override
    public void onStartTextEncoding() {
        //Whatever you want to do by the start of textDecoding
    }

    @Override
    public void onCompleteTextEncoding(ImageSteganography result) {
        if (result != null) {
            if (!result.isDecoded())
                Log.d(TAG, "No message found");
            else {
                if (!result.isSecretKeyWrong()) {
                    Log.d(TAG, "Decoded");
                } else {
                    Log.d(TAG, "Wrong secret key");
                }
            }
        } else {
            Log.d(TAG, "Select Image First");
        }
    }

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
