package com.daust.restaurant.application;

import com.daust.restaurant.domain.Role;
import java.time.LocalDateTime;

/**
 * Read-only view of an {@link com.daust.restaurant.domain.AuditLogEntry} for UC20.
 *
 * <p>{@code username} is resolved via {@link com.daust.restaurant.domain.UserRepository}; if the
 * acting user no longer exists in the system, the raw UUID string is exposed so the audit record
 * still surfaces (NFR-AUD-2: audit log must survive user deactivation/deletion).
 */
public record AuditLogView(
        LocalDateTime timestamp,
        String username,
        Role userRoleAtTime,
        String eventType,
        String affectedEntityType,
        String affectedEntityId,
        String beforeValue,
        String afterValue) {}
