package com.daust.restaurant.application;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.domain.TableStatus;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeatCustomersService {

    private final TableRepository tableRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public SeatCustomersService(
            TableRepository tableRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository) {
        this.tableRepository = tableRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
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
}
