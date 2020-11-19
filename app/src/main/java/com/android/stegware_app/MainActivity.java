package com.android.stegware_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.stegware_app.services.CacheService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();

        Button encode = findViewById(R.id.encode_button);

        encode.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), Encode.class)));

        Intent cacheService = new Intent(getApplicationContext(), CacheService.class);
        startService(cacheService);
    }

    private void checkAndRequestPermissions() {
        int permissionWriteStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int ReadPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();

        Log.d(CacheService.TAG, "1");

        if (ReadPermission != PackageManager.PERMISSION_GRANTED) {
            Log.d(CacheService.TAG, "2");
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (permissionWriteStorage != PackageManager.PERMISSION_GRANTED) {
            Log.d(CacheService.TAG, "3");
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            Log.d(CacheService.TAG, "4");
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), 1);
        }

        Log.d(CacheService.TAG, "5");
    }
}