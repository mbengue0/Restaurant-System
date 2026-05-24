package com.daust.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogQueryServiceTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AuditLogQueryService service;

    @Test
    void recentEntries_mapsToViewsAndResolvesUsername() {
        User admin = new User("alice", HASH, "Alice Admin", Role.ADMIN, false);
        AuditLogEntry entry = new AuditLogEntry(
                admin.getId(),
                Role.ADMIN,
                "CONFIG_UPDATED",
                "Configuration",
                "1",
                "tax=0.18",
                "tax=0.20");
        when(auditLogRepository.findRecent(50)).thenReturn(List.of(entry));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        List<AuditLogView> views = service.recentEntries(50);

        assertThat(views).hasSize(1);
        AuditLogView v = views.get(0);
        assertThat(v.username()).isEqualTo("alice");
        assertThat(v.userRoleAtTime()).isEqualTo(Role.ADMIN);
        assertThat(v.eventType()).isEqualTo("CONFIG_UPDATED");
        assertThat(v.affectedEntityType()).isEqualTo("Configuration");
        assertThat(v.affectedEntityId()).isEqualTo("1");
        assertThat(v.beforeValue()).isEqualTo("tax=0.18");
        assertThat(v.afterValue()).isEqualTo("tax=0.20");
    }

    @Test
    void recentEntries_fallsBackToUuidWhenUserDeleted() {
        UserId deletedUserId = UserId.of(UUID.randomUUID());
        AuditLogEntry entry = new AuditLogEntry(
                deletedUserId,
                Role.MANAGER,
                "ORDER_CANCELLED",
                "Order",
                "abc",
                null,
                null);
        when(auditLogRepository.findRecent(10)).thenReturn(List.of(entry));
        when(userRepository.findById(deletedUserId)).thenReturn(Optional.empty());

        List<AuditLogView> views = service.recentEntries(10);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).username()).isEqualTo(deletedUserId.value().toString());
    }

    @Test
    void recentEntries_cachesUsernameLookupAcrossEntries() {
        User waiter = new User("bob", HASH, "Bob Waiter", Role.WAITER, false);
        AuditLogEntry e1 = new AuditLogEntry(
                waiter.getId(), Role.WAITER, "ORDER_STARTED", "Order", "o1", null, null);
        AuditLogEntry e2 = new AuditLogEntry(
                waiter.getId(), Role.WAITER, "ORDER_SUBMITTED", "Order", "o1", null, null);
        when(auditLogRepository.findRecent(100)).thenReturn(List.of(e1, e2));
        when(userRepository.findById(waiter.getId())).thenReturn(Optional.of(waiter));

        List<AuditLogView> views = service.recentEntries(100);

        assertThat(views).extracting(AuditLogView::username).containsExactly("bob", "bob");
        verify(userRepository).findById(waiter.getId());
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void entriesBetween_passesBoundsToRepository() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 31, 23, 59, 59);
        when(auditLogRepository.findByTimestampBetween(from, to)).thenReturn(List.of());

        List<AuditLogView> views = service.entriesBetween(from, to);

        assertThat(views).isEmpty();
        verify(auditLogRepository).findByTimestampBetween(eq(from), eq(to));
    }

    @Test
    void entriesByEventType_delegatesAndMaps() {
        User admin = new User("alice", HASH, "Alice Admin", Role.ADMIN, false);
        AuditLogEntry entry = new AuditLogEntry(
                admin.getId(), Role.ADMIN, "PAYMENT_RECORDED", "Payment", "p1", null, "amount=12000");
        when(auditLogRepository.findByEventType("PAYMENT_RECORDED")).thenReturn(List.of(entry));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        List<AuditLogView> views = service.entriesByEventType("PAYMENT_RECORDED");

        assertThat(views).hasSize(1);
        assertThat(views.get(0).eventType()).isEqualTo("PAYMENT_RECORDED");
        assertThat(views.get(0).username()).isEqualTo("alice");
    }

    @Test
    void recentEntries_emptyResultReturnsEmptyList() {
        when(auditLogRepository.findRecent(25)).thenReturn(List.of());

        assertThat(service.recentEntries(25)).isEmpty();
    }
}
