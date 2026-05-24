package com.daust.restaurant.application;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.CancellationReason;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderId;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelOrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TableRepository tableRepository;
    private final AuditLogRepository auditLogRepository;

    public CancelOrderService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            TableRepository tableRepository,
            AuditLogRepository auditLogRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.tableRepository = tableRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * UC09 — Cancel an order.
     *
     * <p>Domain enforces BR5 tiered authorization (PLACED: any role;
     * IN_PREPARATION/READY/SERVED: Manager only; COMPLETED/CANCELLED: throws). Per BR5 item 4 the
     * table is NOT released here; the seating waiter explicitly frees the table in a separate
     * action.
     *
     * <p>Note: {@code tableRepository} is held as a collaborator for future side-effects (e.g. a
     * subsequent "release if no other active orders on table" follow-up); it is intentionally
     * unused on the cancellation path itself.
     */
    @Transactional
    public void cancelOrder(
            OrderId orderId, CancellationReason reason, String note, UserId byUserId) {
        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        User byUser = userRepository
                .findById(byUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + byUserId));

        String before = order.getState().name();
        order.cancel(reason, note, byUser);
        orderRepository.save(order);

        auditLogRepository.save(new AuditLogEntry(
                byUser.getId(),
                byUser.getRole(),
                "ORDER_CANCELLED",
                "Order",
                order.getId().value().toString(),
                before,
                order.getState().name() + ":" + reason.name()));
    }
}
