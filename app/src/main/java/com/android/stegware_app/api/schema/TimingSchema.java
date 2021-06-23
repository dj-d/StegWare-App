package com.android.stegware_app.api.schema;

public class TimingSchema {
    public double parseTime;
    public double compileTime;
    public double dynamicLoadingTime;
    public double executionTime;

    public TimingSchema(double parseTime, double compileTime, double dynamicLoadingTime, double executionTime) {
        this.parseTime = parseTime;
        this.compileTime = compileTime;
        this.dynamicLoadingTime = dynamicLoadingTime;
        this.executionTime = executionTime;
    }
}
