package com.daust.restaurant.application;

public class BillAlreadyGeneratedException extends RuntimeException {
    public BillAlreadyGeneratedException(String message) {
        super(message);
    }
}
