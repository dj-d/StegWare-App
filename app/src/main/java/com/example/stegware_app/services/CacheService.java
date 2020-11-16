package com.example.stegware_app.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;

public class CacheService extends IntentService {
    public static final String TAG = "CacheService";
    public static final String ACTION = "CHECK";

    private Context chromeContext;

    public CacheService() {
        super("CacheService");

//        try {
//            chromeContext = createPackageContext("com.android.chrome", 0);
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "Cache dir: " + getCacheDir());
//        Log.d(TAG, "Chrome cache dir" + chromeContext.getCacheDir());
//
//        File dir = new File(chromeContext.getCacheDir(), "Cache");
//        Log.d(TAG, "dir exists: " + dir.exists());
    }

    private void searcher() {
        File dir = new File(getCacheDir(), "test");

        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                Log.d(TAG, "File in cache: " + f.getName());
            }
        }
    }
}
