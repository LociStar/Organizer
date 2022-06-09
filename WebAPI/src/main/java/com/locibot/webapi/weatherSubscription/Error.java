package com.locibot.webapi.weatherSubscription;

public class Error {
    private final String error;

    public Error(String errorType) {
        this.error = errorType;
    }

    public String getError() {
        return error;
    }
}
