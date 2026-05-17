package com.daust.restaurant.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuditLogJpaRepository extends JpaRepository<AuditLogEntryJpaEntity, UUID> {

    List<AuditLogEntryJpaEntity> findByUserIdOrderByTimestampDesc(UUID userId);

    List<AuditLogEntryJpaEntity> findByEventTypeOrderByTimestampDesc(String eventType);

    List<AuditLogEntryJpaEntity> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime from, LocalDateTime to);

    List<AuditLogEntryJpaEntity> findByAffectedEntityTypeAndAffectedEntityIdOrderByTimestampDesc(
            String affectedEntityType, String affectedEntityId);
}
