package com.android.stegware_app.api.schema;

public class AttackSchema {
    public DeviceSchema device;
    public String payloadId;
    public TimingSchema timing;
    public String resultType;
    public String result;

    public AttackSchema(DeviceSchema device, String payloadId, TimingSchema timing, String resultType, String result) {
        this.device = device;
        this.payloadId = payloadId;
        this.timing = timing;
        this.resultType = resultType;
        this.result = result;
    }
}