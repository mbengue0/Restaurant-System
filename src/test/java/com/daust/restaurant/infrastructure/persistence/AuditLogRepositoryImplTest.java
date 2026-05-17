package com.daust.restaurant.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogId;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.UserId;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(AuditLogRepositoryImpl.class)
class AuditLogRepositoryImplTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private static AuditLogEntry entryAt(LocalDateTime ts, UserId user, String eventType, String type, String id) {
        return AuditLogEntry.reconstitute(
                AuditLogId.generate(), ts, user, Role.MANAGER, eventType, type, id, null, null);
    }

    @Test
    void saveAndFindById_roundTripsAllFields() {
        UserId user = UserId.generate();
        AuditLogEntry original = new AuditLogEntry(
                user,
                Role.MANAGER,
                "ORDER_CANCELLED",
                "Order",
                "ord-1",
                "{\"state\":\"PLACED\"}",
                "{\"state\":\"CANCELLED\"}");

        auditLogRepository.save(original);

        AuditLogEntry reloaded = auditLogRepository.findById(original.getId()).orElseThrow();
        assertThat(reloaded.getId()).isEqualTo(original.getId());
        assertThat(reloaded.getUserId()).isEqualTo(user);
        assertThat(reloaded.getUserRoleAtTime()).isEqualTo(Role.MANAGER);
        assertThat(reloaded.getEventType()).isEqualTo("ORDER_CANCELLED");
        assertThat(reloaded.getAffectedEntityType()).isEqualTo("Order");
        assertThat(reloaded.getAffectedEntityId()).isEqualTo("ord-1");
        assertThat(reloaded.getBeforeValue()).isEqualTo("{\"state\":\"PLACED\"}");
        assertThat(reloaded.getAfterValue()).isEqualTo("{\"state\":\"CANCELLED\"}");
        assertThat(reloaded.getTimestamp()).isNotNull();
    }

    @Test
    void save_persistsAllOptionalFieldsAsNull() {
        AuditLogEntry entry = new AuditLogEntry(UserId.generate(), Role.WAITER, "LOGIN", null, null, null, null);

        auditLogRepository.save(entry);

        AuditLogEntry reloaded = auditLogRepository.findById(entry.getId()).orElseThrow();
        assertThat(reloaded.getAffectedEntityType()).isNull();
        assertThat(reloaded.getAffectedEntityId()).isNull();
        assertThat(reloaded.getBeforeValue()).isNull();
        assertThat(reloaded.getAfterValue()).isNull();
    }

    @Test
    void findByUserId_returnsOnlyEntriesForThatUserOrderedNewestFirst() {
        UserId alice = UserId.generate();
        UserId bob = UserId.generate();
        AuditLogEntry old = entryAt(LocalDateTime.of(2026, 1, 1, 8, 0), alice, "LOGIN", null, null);
        AuditLogEntry recent = entryAt(LocalDateTime.of(2026, 1, 1, 10, 0), alice, "ORDER_PLACED", "Order", "o1");
        AuditLogEntry foreign = entryAt(LocalDateTime.of(2026, 1, 1, 9, 0), bob, "LOGIN", null, null);

        auditLogRepository.save(old);
        auditLogRepository.save(recent);
        auditLogRepository.save(foreign);

        List<AuditLogEntry> entries = auditLogRepository.findByUserId(alice);

        assertThat(entries).extracting(AuditLogEntry::getId).containsExactly(recent.getId(), old.getId());
    }

    @Test
    void findByEventType_returnsOnlyMatchingEvents() {
        UserId user = UserId.generate();
        AuditLogEntry placed = entryAt(LocalDateTime.of(2026, 1, 1, 8, 0), user, "ORDER_PLACED", "Order", "o1");
        AuditLogEntry cancelled = entryAt(LocalDateTime.of(2026, 1, 1, 9, 0), user, "ORDER_CANCELLED", "Order", "o1");
        AuditLogEntry otherEvent = entryAt(LocalDateTime.of(2026, 1, 1, 10, 0), user, "LOGIN", null, null);

        auditLogRepository.save(placed);
        auditLogRepository.save(cancelled);
        auditLogRepository.save(otherEvent);

        List<AuditLogEntry> placedOnly = auditLogRepository.findByEventType("ORDER_PLACED");

        assertThat(placedOnly).extracting(AuditLogEntry::getId).containsExactly(placed.getId());
    }

    @Test
    void findByTimestampBetween_returnsEntriesInRangeOrderedNewestFirst() {
        UserId user = UserId.generate();
        LocalDateTime jan1 = LocalDateTime.of(2026, 1, 1, 12, 0);
        LocalDateTime jan15 = LocalDateTime.of(2026, 1, 15, 12, 0);
        LocalDateTime jan31 = LocalDateTime.of(2026, 1, 31, 12, 0);
        LocalDateTime feb15 = LocalDateTime.of(2026, 2, 15, 12, 0);

        AuditLogEntry e1 = entryAt(jan1, user, "X", null, null);
        AuditLogEntry e2 = entryAt(jan15, user, "X", null, null);
        AuditLogEntry e3 = entryAt(jan31, user, "X", null, null);
        AuditLogEntry e4 = entryAt(feb15, user, "X", null, null);

        auditLogRepository.save(e1);
        auditLogRepository.save(e2);
        auditLogRepository.save(e3);
        auditLogRepository.save(e4);

        List<AuditLogEntry> januaryEntries = auditLogRepository.findByTimestampBetween(
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59));

        assertThat(januaryEntries)
                .extracting(AuditLogEntry::getId)
                .containsExactly(e3.getId(), e2.getId(), e1.getId());
    }

    @Test
    void findByAffectedEntity_returnsOnlyEntriesAboutThatEntity() {
        UserId user = UserId.generate();
        AuditLogEntry order1Placed = entryAt(
                LocalDateTime.of(2026, 1, 1, 8, 0), user, "ORDER_PLACED", "Order", "o1");
        AuditLogEntry order1Cancelled = entryAt(
                LocalDateTime.of(2026, 1, 1, 9, 0), user, "ORDER_CANCELLED", "Order", "o1");
        AuditLogEntry order2Placed = entryAt(
                LocalDateTime.of(2026, 1, 1, 10, 0), user, "ORDER_PLACED", "Order", "o2");
        AuditLogEntry billGenerated = entryAt(
                LocalDateTime.of(2026, 1, 1, 11, 0), user, "BILL_GENERATED", "Bill", "b1");

        auditLogRepository.save(order1Placed);
        auditLogRepository.save(order1Cancelled);
        auditLogRepository.save(order2Placed);
        auditLogRepository.save(billGenerated);

        List<AuditLogEntry> order1History = auditLogRepository.findByAffectedEntity("Order", "o1");

        assertThat(order1History)
                .extracting(AuditLogEntry::getId)
                .containsExactly(order1Cancelled.getId(), order1Placed.getId());
    }

    @Test
    void findByUserId_returnsEntryEvenAfterTheUserIsConceptuallyDeactivated() {
        // The user_id column is a plain UUID with no FK constraint, so audit entries survive
        // even if no User row exists for that id (NFR-AUD-2). We simulate this by saving an
        // audit entry whose user_id was never inserted into the users table.
        UserId neverPersisted = UserId.generate();
        AuditLogEntry entry = entryAt(LocalDateTime.of(2026, 1, 1, 10, 0), neverPersisted, "LOGIN", null, null);

        auditLogRepository.save(entry);

        List<AuditLogEntry> entries = auditLogRepository.findByUserId(neverPersisted);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getUserId()).isEqualTo(neverPersisted);
    }

    @Test
    void userIdField_isPlainUUIDColumn_notForeignKeyToUserJpaEntity() throws NoSuchFieldException {
        Field userIdField = AuditLogEntryJpaEntity.class.getDeclaredField("userId");

        assertThat(userIdField.getType()).isEqualTo(UUID.class);
        assertThat(userIdField.isAnnotationPresent(ManyToOne.class))
                .as("user_id must not be a @ManyToOne to UserJpaEntity (audit entries outlive users)")
                .isFalse();
        assertThat(userIdField.isAnnotationPresent(JoinColumn.class))
                .as("user_id must not have a @JoinColumn (no FK to users.id)")
                .isFalse();
    }
}
