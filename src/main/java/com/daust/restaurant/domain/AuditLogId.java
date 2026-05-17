package com.daust.restaurant.domain;

import java.util.Objects;
import java.util.UUID;

public record AuditLogId(UUID value) {

    public AuditLogId {
        Objects.requireNonNull(value, "AuditLogId value must not be null");
    }

    public static AuditLogId generate() {
        return new AuditLogId(UUID.randomUUID());
    }

    public static AuditLogId of(UUID value) {
        return new AuditLogId(value);
    }
}
