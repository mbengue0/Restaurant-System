package com.daust.restaurant.domain;

import java.util.Objects;
import java.util.UUID;

public record BillId(UUID value) {

    public BillId {
        Objects.requireNonNull(value, "BillId value must not be null");
    }

    public static BillId generate() {
        return new BillId(UUID.randomUUID());
    }

    public static BillId of(UUID value) {
        return new BillId(value);
    }
}
