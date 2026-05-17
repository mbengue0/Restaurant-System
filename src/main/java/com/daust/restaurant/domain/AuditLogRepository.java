package com.daust.restaurant.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuditLogRepository {

    void save(AuditLogEntry entry);

    Optional<AuditLogEntry> findById(AuditLogId id);

    List<AuditLogEntry> findByUserId(UserId userId);

    List<AuditLogEntry> findByEventType(String eventType);

    List<AuditLogEntry> findByTimestampBetween(LocalDateTime from, LocalDateTime to);

    List<AuditLogEntry> findByAffectedEntity(String entityType, String entityId);
}
