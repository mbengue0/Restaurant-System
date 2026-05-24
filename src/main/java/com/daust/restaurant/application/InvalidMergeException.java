package com.daust.restaurant.application;

/**
 * Inputs to {@link MergeOrdersService#mergeOrders} were not a valid merge — fewer than two orders,
 * a duplicate, or any state-level guard that doesn't fit the more specific exceptions.
 */
public class InvalidMergeException extends RuntimeException {
    public InvalidMergeException(String message) {
        super(message);
    }
}
