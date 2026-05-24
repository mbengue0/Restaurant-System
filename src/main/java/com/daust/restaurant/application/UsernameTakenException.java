package com.daust.restaurant.application;

public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String message) {
        super(message);
    }
}
