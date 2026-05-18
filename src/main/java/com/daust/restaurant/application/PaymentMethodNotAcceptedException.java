package com.daust.restaurant.application;

public class PaymentMethodNotAcceptedException extends RuntimeException {
    public PaymentMethodNotAcceptedException(String message) {
        super(message);
    }
}
