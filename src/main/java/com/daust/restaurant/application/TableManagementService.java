package com.daust.restaurant.application;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC02 — Manage Tables (Admin-only). Provides create / deactivate / list beyond the seed data.
 *
 * <p>The deferred BR1 guard is enforced here: a table cannot be deactivated while it still has at
 * least one active (non-terminal) {@link com.daust.restaurant.domain.Order}. The Domain's
 * {@link Table#deactivate()} stays unconditional because the check needs the OrderRepository,
 * which lives outside the Table aggregate.
 *
 * <p>FR1: capacity is required and must be positive (Domain enforces). FR8: deactivation never
 * hard-deletes — it flips the {@code active} flag so historical references remain intact.
 */
@Service
@Transactional
public class TableManagementService {

    private final TableRepository tableRepository;
    private final OrderRepository orderRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public TableManagementService(
            TableRepository tableRepository,
            OrderRepository orderRepository,
            AuditLogRepository auditLogRepository,
            UserRepository userRepository) {
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public Table createTable(int capacity, UserId adminId) {
        User admin = loadAdmin(adminId);
        Table table = new Table(capacity);
        tableRepository.save(table);

        auditLogRepository.save(new AuditLogEntry(
                admin.getId(),
                admin.getRole(),
                "TABLE_CREATED",
                "Table",
                table.getId().value().toString(),
                null,
                "capacity=" + table.getCapacity() + ", status=" + table.getStatus()));
        return table;
    }

    public void deactivateTable(TableId tableId, UserId adminId) {
        User admin = loadAdmin(adminId);
        Table table = tableRepository
                .findById(tableId)
                .orElseThrow(() -> new TableNotFoundException("Table not found: " + tableId));

        List<?> activeOrders = orderRepository.findActiveByTableId(tableId);
        if (!activeOrders.isEmpty()) {
            throw new TableHasActiveOrdersException(
                    "Cannot deactivate table " + tableId + " — it has "
                            + activeOrders.size() + " active order(s).");
        }

        table.deactivate();
        tableRepository.save(table);

        auditLogRepository.save(new AuditLogEntry(
                admin.getId(),
                admin.getRole(),
                "TABLE_DEACTIVATED",
                "Table",
                table.getId().value().toString(),
                "active=true",
                "active=false"));
    }

    @Transactional(readOnly = true)
    public List<Table> listTables() {
        return tableRepository.findAll();
    }

    private User loadAdmin(UserId adminId) {
        return userRepository
                .findById(adminId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + adminId));
    }
}
