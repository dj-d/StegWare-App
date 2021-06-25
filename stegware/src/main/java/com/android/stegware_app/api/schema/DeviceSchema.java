package com.android.stegware_app.api.schema;

import java.util.List;

public class DeviceSchema {
    public String model;
    public int api;
    public List<String> permissions;

    public DeviceSchema(String model, int api, List<String> permissions) {
        this.model = model;
        this.api = api;
        this.permissions = permissions;
    }
}
