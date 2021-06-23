package com.android.stegware_app.api.schema;

public class TimingSchema {
    public int parseTime;
    public int compileTime;
    public int dynamicLoadingTime;
    public int executionTime;

    public TimingSchema(int parseTime, int compileTime, int dynamicLoadingTime, int executionTime) {
        this.parseTime = parseTime;
        this.compileTime = compileTime;
        this.dynamicLoadingTime = dynamicLoadingTime;
        this.executionTime = executionTime;
    }
}
