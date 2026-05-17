package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogId;
import com.daust.restaurant.domain.UserId;

final class AuditLogMapper {

    private AuditLogMapper() {
    }

    static AuditLogEntry toDomain(AuditLogEntryJpaEntity entity) {
        return AuditLogEntry.reconstitute(
                AuditLogId.of(entity.getId()),
                entity.getTimestamp(),
                UserId.of(entity.getUserId()),
                entity.getUserRoleAtTime(),
                entity.getEventType(),
                entity.getAffectedEntityType(),
                entity.getAffectedEntityId(),
                entity.getBeforeValue(),
                entity.getAfterValue());
    }

    static AuditLogEntryJpaEntity toEntity(AuditLogEntry entry) {
        return new AuditLogEntryJpaEntity(
                entry.getId().value(),
                entry.getTimestamp(),
                entry.getUserId().value(),
                entry.getUserRoleAtTime(),
                entry.getEventType(),
                entry.getAffectedEntityType(),
                entry.getAffectedEntityId(),
                entry.getBeforeValue(),
                entry.getAfterValue());
    }
}
