package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogId;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.UserId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final AuditLogJpaRepository jpaRepository;

    AuditLogRepositoryImpl(AuditLogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(AuditLogEntry entry) {
        jpaRepository.save(AuditLogMapper.toEntity(entry));
    }

    @Override
    public Optional<AuditLogEntry> findById(AuditLogId id) {
        return jpaRepository.findById(id.value()).map(AuditLogMapper::toDomain);
    }

    @Override
    public List<AuditLogEntry> findByUserId(UserId userId) {
        return jpaRepository.findByUserIdOrderByTimestampDesc(userId.value()).stream()
                .map(AuditLogMapper::toDomain)
                .toList();
    }

    @Override
    public List<AuditLogEntry> findByEventType(String eventType) {
        return jpaRepository.findByEventTypeOrderByTimestampDesc(eventType).stream()
                .map(AuditLogMapper::toDomain)
                .toList();
    }

    @Override
    public List<AuditLogEntry> findByTimestampBetween(LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByTimestampBetweenOrderByTimestampDesc(from, to).stream()
                .map(AuditLogMapper::toDomain)
                .toList();
    }

    @Override
    public List<AuditLogEntry> findByAffectedEntity(String entityType, String entityId) {
        return jpaRepository
                .findByAffectedEntityTypeAndAffectedEntityIdOrderByTimestampDesc(entityType, entityId)
                .stream()
                .map(AuditLogMapper::toDomain)
                .toList();
    }
}
