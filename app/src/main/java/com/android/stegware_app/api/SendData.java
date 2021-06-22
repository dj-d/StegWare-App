package com.android.stegware_app.api;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Body;

public interface SendData {
    @POST("api/attack")
    Call<JSONObject> Send(
            @Body JSONObject jsonObject
    );
}
