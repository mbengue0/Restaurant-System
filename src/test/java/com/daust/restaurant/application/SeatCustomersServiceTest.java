package com.daust.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.domain.TableStatus;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeatCustomersServiceTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock private TableRepository tableRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private SeatCustomersService service;

    @Test
    void seatTable_seatsAndEmitsAudit_onHappyPath() {
        Table table = new Table(4);
        User waiter = new User("waiter", HASH, "W", Role.WAITER, false);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(userRepository.findById(waiter.getId())).thenReturn(Optional.of(waiter));

        service.seatTable(table.getId(), waiter.getId());

        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        verify(tableRepository).save(table);
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntry entry = captor.getValue();
        assertThat(entry.getEventType()).isEqualTo("TABLE_SEATED");
        assertThat(entry.getUserId()).isEqualTo(waiter.getId());
        assertThat(entry.getUserRoleAtTime()).isEqualTo(Role.WAITER);
    }

    @Test
    void seatTable_throwsTableAlreadyOccupied_whenTableOccupied() {
        Table table = new Table(2);
        table.seatCustomers();
        User waiter = new User("waiter", HASH, "W", Role.WAITER, false);
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(userRepository.findById(waiter.getId())).thenReturn(Optional.of(waiter));

        assertThatThrownBy(() -> service.seatTable(table.getId(), waiter.getId()))
                .isInstanceOf(TableAlreadyOccupiedException.class);

        verify(tableRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void seatTable_throwsTableNotFound_whenMissing() {
        TableId id = TableId.generate();
        when(tableRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.seatTable(id, UserId.generate()))
                .isInstanceOf(TableNotFoundException.class);
    }
}
