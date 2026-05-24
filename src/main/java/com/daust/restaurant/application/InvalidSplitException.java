package com.daust.restaurant.application;

/**
 * The grouping passed to {@link SplitOrderService#splitOrder} did not form a valid partition of
 * the order's items — duplicates, missing items, unknown ids, or empty groups all surface here.
 */
public class InvalidSplitException extends RuntimeException {
    public InvalidSplitException(String message) {
        super(message);
    }
}
