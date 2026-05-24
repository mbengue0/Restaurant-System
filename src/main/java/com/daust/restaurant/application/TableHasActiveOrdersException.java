package com.daust.restaurant.application;

/**
 * BR1 — a table cannot be deactivated while it still has at least one non-terminal Order.
 * Thrown by {@link TableManagementService#deactivateTable} when the deferred cross-aggregate
 * guard rejects the request.
 */
public class TableHasActiveOrdersException extends RuntimeException {
    public TableHasActiveOrdersException(String message) {
        super(message);
    }
}
