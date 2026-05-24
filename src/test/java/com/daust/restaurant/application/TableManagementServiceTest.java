package com.daust.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TableManagementServiceTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock private TableRepository tableRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private TableManagementService service;

    @Test
    void createTable_savesAndAudits() {
        User admin = newAdmin();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        Table created = service.createTable(4, admin.getId());

        assertThat(created.getCapacity()).isEqualTo(4);
        assertThat(created.isActive()).isTrue();
        verify(tableRepository).save(created);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntry entry = captor.getValue();
        assertThat(entry.getEventType()).isEqualTo("TABLE_CREATED");
        assertThat(entry.getAffectedEntityType()).isEqualTo("Table");
        assertThat(entry.getAffectedEntityId()).isEqualTo(created.getId().value().toString());
        assertThat(entry.getUserRoleAtTime()).isEqualTo(Role.ADMIN);
        assertThat(entry.getAfterValue()).contains("capacity=4");
    }

    @Test
    void createTable_rejectsNonPositiveCapacity_andDoesNotSave() {
        User admin = newAdmin();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.createTable(0, admin.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capacity");

        verify(tableRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void createTable_throwsWhenAdminMissing() {
        UserId adminId = UserId.of(UUID.randomUUID());
        when(userRepository.findById(adminId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createTable(2, adminId))
                .isInstanceOf(UserNotFoundException.class);

        verify(tableRepository, never()).save(any());
    }

    @Test
    void deactivateTable_succeeds_whenNoActiveOrders() {
        User admin = newAdmin();
        Table table = new Table(4);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(orderRepository.findActiveByTableId(table.getId())).thenReturn(List.of());

        service.deactivateTable(table.getId(), admin.getId());

        assertThat(table.isActive()).isFalse();
        verify(tableRepository).save(table);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntry entry = captor.getValue();
        assertThat(entry.getEventType()).isEqualTo("TABLE_DEACTIVATED");
        assertThat(entry.getBeforeValue()).isEqualTo("active=true");
        assertThat(entry.getAfterValue()).isEqualTo("active=false");
    }

    @Test
    void deactivateTable_throwsWhenActiveOrderExists() {
        User admin = newAdmin();
        Table table = new Table(4);
        Order activeOrder = newOrder(table.getId());
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));
        when(orderRepository.findActiveByTableId(table.getId())).thenReturn(List.of(activeOrder));

        assertThatThrownBy(() -> service.deactivateTable(table.getId(), admin.getId()))
                .isInstanceOf(TableHasActiveOrdersException.class)
                .hasMessageContaining("active order");

        assertThat(table.isActive()).isTrue();
        verify(tableRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void deactivateTable_throwsWhenTableMissing() {
        User admin = newAdmin();
        TableId missing = TableId.of(UUID.randomUUID());
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tableRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateTable(missing, admin.getId()))
                .isInstanceOf(TableNotFoundException.class);

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void listTables_returnsRepositoryContents() {
        Table t1 = new Table(2);
        Table t2 = new Table(4);
        when(tableRepository.findAll()).thenReturn(List.of(t1, t2));

        List<Table> tables = service.listTables();

        assertThat(tables).containsExactly(t1, t2);
    }

    private static User newAdmin() {
        return new User("root", HASH, "Root Admin", Role.ADMIN, false);
    }

    private static Order newOrder(TableId tableId) {
        return new Order(tableId);
    }
}
