package com.daust.restaurant.application;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.domain.TableStatus;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeatCustomersService {

    private final TableRepository tableRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final OrderRepository orderRepository;

    public SeatCustomersService(
            TableRepository tableRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            OrderRepository orderRepository) {
        this.tableRepository = tableRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void seatTable(TableId tableId, UserId waiterId) {
        Table table = tableRepository
                .findById(tableId)
                .orElseThrow(() -> new TableNotFoundException("Table not found: " + tableId));
        User waiter = userRepository
                .findById(waiterId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + waiterId));

        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw new TableAlreadyOccupiedException("Table " + tableId + " is already occupied");
        }

        table.seatCustomers();
        tableRepository.save(table);

        auditLogRepository.save(new AuditLogEntry(
                waiter.getId(),
                waiter.getRole(),
                "TABLE_SEATED",
                "Table",
                table.getId().value().toString(),
                "AVAILABLE",
                "OCCUPIED"));
    }

    /**
     * Manually mark an OCCUPIED table as AVAILABLE. Needed when an order is cancelled — per BR5
     * item 4 the table stays OCCUPIED, and (because no payment is ever recorded for a cancelled
     * order) the automatic release in {@link RecordPaymentService} never runs.
     *
     * <p>Cross-aggregate guard: rejects with {@link TableHasActiveOrdersException} if any active
     * (non-terminal) order still references this table. Same guard shape as
     * {@link TableManagementService#deactivateTable}.
     */
    @Transactional
    public void releaseTable(TableId tableId, UserId actorId) {
        Table table = tableRepository
                .findById(tableId)
                .orElseThrow(() -> new TableNotFoundException("Table not found: " + tableId));
        User actor = userRepository
                .findById(actorId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + actorId));

        if (table.getStatus() != TableStatus.OCCUPIED) {
            throw new TableNotOccupiedException(
                    "Table " + tableId + " is not occupied; status=" + table.getStatus());
        }

        List<?> activeOrders = orderRepository.findActiveByTableId(tableId);
        if (!activeOrders.isEmpty()) {
            throw new TableHasActiveOrdersException(
                    "Cannot release table " + tableId + " — it has "
                            + activeOrders.size() + " active order(s).");
        }

        table.markAvailable();
        tableRepository.save(table);

        auditLogRepository.save(new AuditLogEntry(
                actor.getId(),
                actor.getRole(),
                "TABLE_RELEASED",
                "Table",
                table.getId().value().toString(),
                "OCCUPIED",
                "AVAILABLE"));
    }
}
