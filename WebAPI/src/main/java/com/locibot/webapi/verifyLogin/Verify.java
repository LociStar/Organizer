package com.locibot.webapi.verifyLogin;

public class Verify {

    private boolean valid;

    public Verify(boolean valid) {
        this.valid = valid;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public String toString() {
        return "Verify{" +
                "valid=" + valid +
                '}';
    }
}
