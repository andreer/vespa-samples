package com.kkraune;

public class OutOfCapacityException extends Exception {
    String message;

    public OutOfCapacityException(String msg) {
        message = msg;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
