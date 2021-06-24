package com.android.stegware_app.services;

public class CommunicationService {
    private static final String TAG = "Communication_Task";

    private String getDeviceModel() {
        return android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
    }
}
