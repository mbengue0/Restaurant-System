package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
class AuditLogEntryJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // PLAIN UUID column, NOT a @ManyToOne / @JoinColumn to UserJpaEntity.
    // Audit entries must remain valid after the referenced User is deactivated
    // or otherwise mutated (per Section 7 + NFR-AUD-2). No FK constraint.
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role_at_time", nullable = false, updatable = false, length = 20)
    private Role userRoleAtTime;

    @Column(name = "event_type", nullable = false, updatable = false, length = 100)
    private String eventType;

    @Column(name = "affected_entity_type", updatable = false, length = 50)
    private String affectedEntityType;

    @Column(name = "affected_entity_id", updatable = false, length = 50)
    private String affectedEntityId;

    @Column(name = "before_value", updatable = false, columnDefinition = "TEXT")
    private String beforeValue;

    @Column(name = "after_value", updatable = false, columnDefinition = "TEXT")
    private String afterValue;
}
