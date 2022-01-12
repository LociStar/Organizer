package com.locibot.webapi.login;

public class Login {
    private String accessToken;

    public Login() {
    }

    public Login(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getMessage() {
        return this.accessToken;
    }

    public void setMessage(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String toString() {
        return "Login{" +
                "access='" + accessToken + '\'' +
                '}';
    }
}
