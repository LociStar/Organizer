package com.locibot.webapi.weatherSubscription;

public class Data {
    private final String data;

    public Data(String dataType) {
        this.data = dataType;
    }

    public String getError() {
        return data;
    }
}
