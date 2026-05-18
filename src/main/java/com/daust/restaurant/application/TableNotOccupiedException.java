package com.daust.restaurant.application;

public class TableNotOccupiedException extends RuntimeException {
    public TableNotOccupiedException(String message) {
        super(message);
    }
}
