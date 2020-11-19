package com.android.stegware_app.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;

public class CacheService extends Service {
    public static final String TAG = "CacheService";

    public CacheService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onTaskRemoved(intent);

        // Delay 3 sec
        SystemClock.sleep(3000);

        // TODO: Add task to do

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);

        super.onTaskRemoved(rootIntent);
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
