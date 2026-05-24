package com.daust.restaurant.application;

/**
 * Thrown when an attempt is made to deactivate the last remaining active ADMIN user (FR14).
 * Domain's {@code User.deactivate()} is unconditional; this guard is owned by the Application
 * layer because the rule needs cross-aggregate visibility.
 */
public class LastActiveAdminException extends RuntimeException {

    public LastActiveAdminException(String message) {
        super(message);
    }
}
