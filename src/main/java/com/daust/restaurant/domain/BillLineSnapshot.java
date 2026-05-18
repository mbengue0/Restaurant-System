package com.daust.restaurant.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record BillLineSnapshot(String menuItemName, int quantity, BigDecimal recordedUnitPrice) {

    public BillLineSnapshot {
        Objects.requireNonNull(menuItemName, "menuItemName must not be null");
        if (menuItemName.isBlank()) {
            throw new IllegalArgumentException("menuItemName must not be blank");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be >= 1");
        }
        Objects.requireNonNull(recordedUnitPrice, "recordedUnitPrice must not be null");
        if (recordedUnitPrice.signum() <= 0) {
            throw new IllegalArgumentException("recordedUnitPrice must be > 0");
        }
    }

    public BigDecimal lineTotal() {
        return recordedUnitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
