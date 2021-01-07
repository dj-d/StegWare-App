package com.android.stegware_app;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.stegware_app.jobs.MediaSearchJob;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();

        Button encode = findViewById(R.id.encode_button);

        encode.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), Encode.class)));

        Button decode = findViewById(R.id.decode_button);

        decode.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), Decode.class)));

//        Intent intent = new Intent(getApplicationContext(), MediaSearchService.class);
//        startService(intent);

        // Creation and start of Job
        JobInfo.Builder builder = new JobInfo.Builder(1, new ComponentName(getApplicationContext(), MediaSearchJob.class.getName()));
        JobInfo job = builder.build();
        JobScheduler scheduler = (JobScheduler) MainActivity.this.getSystemService(JOB_SCHEDULER_SERVICE);

        int result = scheduler.schedule(job);

        if (result == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job started");
        } else {
            Log.d(TAG, "Job error");
        }
    }

    private void checkAndRequestPermissions() {
        int permissionWriteStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int ReadPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (ReadPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (permissionWriteStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), 1);
        }
    }
}