package com.daust.restaurant.application;

/**
 * BR6 — split/merge is gated on {@link com.daust.restaurant.domain.Configuration#isSplitMergePolicyEnabled()}.
 * Thrown by {@link SplitOrderService#splitOrder} (and any merge equivalent) when the policy is off.
 */
public class SplitMergeDisabledException extends RuntimeException {
    public SplitMergeDisabledException(String message) {
        super(message);
    }
}
