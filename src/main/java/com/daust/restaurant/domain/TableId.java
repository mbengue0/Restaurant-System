package com.daust.restaurant.domain;

import java.util.Objects;
import java.util.UUID;

public record TableId(UUID value) {

    public TableId {
        Objects.requireNonNull(value, "TableId value must not be null");
    }

    public static TableId generate() {
        return new TableId(UUID.randomUUID());
    }

    public static TableId of(UUID value) {
        return new TableId(value);
    }
}
