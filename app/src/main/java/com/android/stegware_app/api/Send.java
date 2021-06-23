package com.android.stegware_app.api;

import com.android.stegware_app.api.schema.AttackSchema;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Body;

public interface Send {
    @POST("api/attack")
    Call<String> AttackData(
            @Body AttackSchema jsonObject
    );
}
