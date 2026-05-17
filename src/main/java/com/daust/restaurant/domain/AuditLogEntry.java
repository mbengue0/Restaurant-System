package com.daust.restaurant.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class AuditLogEntry {

    private final AuditLogId id;
    private final LocalDateTime timestamp;
    private final UserId userId;
    private final Role userRoleAtTime;
    private final String eventType;
    private final String affectedEntityType;
    private final String affectedEntityId;
    private final String beforeValue;
    private final String afterValue;

    public AuditLogEntry(
            UserId userId,
            Role userRoleAtTime,
            String eventType,
            String affectedEntityType,
            String affectedEntityId,
            String beforeValue,
            String afterValue) {
        this(
                AuditLogId.generate(),
                LocalDateTime.now(),
                Objects.requireNonNull(userId, "userId must not be null"),
                Objects.requireNonNull(userRoleAtTime, "userRoleAtTime must not be null"),
                requireNonBlank(eventType, "eventType"),
                affectedEntityType,
                affectedEntityId,
                beforeValue,
                afterValue);
    }

    public static AuditLogEntry reconstitute(
            AuditLogId id,
            LocalDateTime timestamp,
            UserId userId,
            Role userRoleAtTime,
            String eventType,
            String affectedEntityType,
            String affectedEntityId,
            String beforeValue,
            String afterValue) {
        return new AuditLogEntry(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(timestamp, "timestamp"),
                Objects.requireNonNull(userId, "userId"),
                Objects.requireNonNull(userRoleAtTime, "userRoleAtTime"),
                requireNonBlank(eventType, "eventType"),
                affectedEntityType,
                affectedEntityId,
                beforeValue,
                afterValue);
    }

    private AuditLogEntry(
            AuditLogId id,
            LocalDateTime timestamp,
            UserId userId,
            Role userRoleAtTime,
            String eventType,
            String affectedEntityType,
            String affectedEntityId,
            String beforeValue,
            String afterValue) {
        this.id = id;
        this.timestamp = timestamp;
        this.userId = userId;
        this.userRoleAtTime = userRoleAtTime;
        this.eventType = eventType;
        this.affectedEntityType = affectedEntityType;
        this.affectedEntityId = affectedEntityId;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
    }

    public AuditLogId getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public UserId getUserId() {
        return userId;
    }

    public Role getUserRoleAtTime() {
        return userRoleAtTime;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAffectedEntityType() {
        return affectedEntityType;
    }

    public String getAffectedEntityId() {
        return affectedEntityId;
    }

    public String getBeforeValue() {
        return beforeValue;
    }

    public String getAfterValue() {
        return afterValue;
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
