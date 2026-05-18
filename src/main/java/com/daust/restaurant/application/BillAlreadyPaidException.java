package com.daust.restaurant.application;

public class BillAlreadyPaidException extends RuntimeException {
    public BillAlreadyPaidException(String message) {
        super(message);
    }
}
