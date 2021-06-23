package com.android.stegware_app.api.schema;

public class AttackSchema {
    public DeviceSchema deviceSchema;
    public String payloadId;
    public TimingSchema timingSchema;
    public String resultType;
    public String result;

    public AttackSchema(DeviceSchema deviceSchema, String payloadId, TimingSchema timingSchema, String resultType, String result) {
        this.deviceSchema = deviceSchema;
        this.payloadId = payloadId;
        this.timingSchema = timingSchema;
        this.resultType = resultType;
        this.result = result;
    }
}