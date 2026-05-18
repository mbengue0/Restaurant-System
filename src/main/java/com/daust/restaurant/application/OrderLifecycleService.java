package com.daust.restaurant.application;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderId;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderLifecycleService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public OrderLifecycleService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void startPreparation(OrderId orderId, UserId kitchenStaffId) {
        Order order = loadOrder(orderId);
        User staff = loadUser(kitchenStaffId);

        String before = order.getState().name();
        order.startPreparation();
        orderRepository.save(order);

        auditLogRepository.save(new AuditLogEntry(
                staff.getId(),
                staff.getRole(),
                "ORDER_PREPARATION_STARTED",
                "Order",
                order.getId().value().toString(),
                before,
                order.getState().name()));
    }

    @Transactional
    public void markReady(OrderId orderId, UserId kitchenStaffId) {
        Order order = loadOrder(orderId);
        User staff = loadUser(kitchenStaffId);

        String before = order.getState().name();
        order.markReady();
        orderRepository.save(order);

        auditLogRepository.save(new AuditLogEntry(
                staff.getId(),
                staff.getRole(),
                "ORDER_READY",
                "Order",
                order.getId().value().toString(),
                before,
                order.getState().name()));
    }

    @Transactional
    public void markServed(OrderId orderId, UserId waiterId) {
        Order order = loadOrder(orderId);
        User waiter = loadUser(waiterId);

        String before = order.getState().name();
        order.markServed();
        orderRepository.save(order);

        auditLogRepository.save(new AuditLogEntry(
                waiter.getId(),
                waiter.getRole(),
                "ORDER_SERVED",
                "Order",
                order.getId().value().toString(),
                before,
                order.getState().name()));
    }

    private Order loadOrder(OrderId orderId) {
        return orderRepository
                .findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    private User loadUser(UserId userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }
}
