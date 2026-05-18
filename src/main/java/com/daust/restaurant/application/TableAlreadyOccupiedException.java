package com.daust.restaurant.application;

public class TableAlreadyOccupiedException extends RuntimeException {
    public TableAlreadyOccupiedException(String message) {
        super(message);
    }
}
