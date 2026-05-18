package com.daust.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.CategoryId;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.OrderState;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleServiceTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private OrderLifecycleService service;

    private Order placedOrderWithItems() {
        Table table = new Table(4);
        table.seatCustomers();
        Order order = new Order(table.getId());
        MenuItem item = new MenuItem("Yassa", null, new BigDecimal("7500"), CategoryId.generate());
        order.addItem(item, 1);
        order.submit();
        return order;
    }

    @Test
    void startPreparation_transitionsAndAudits() {
        Order order = placedOrderWithItems();
        User kitchen = new User("k", HASH, "K", Role.KITCHEN_STAFF, false);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findById(kitchen.getId())).thenReturn(Optional.of(kitchen));

        service.startPreparation(order.getId(), kitchen.getId());

        assertThat(order.getState()).isEqualTo(OrderState.IN_PREPARATION);
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("ORDER_PREPARATION_STARTED");
    }

    @Test
    void markReady_transitionsAndAudits() {
        Order order = placedOrderWithItems();
        order.startPreparation();
        User kitchen = new User("k", HASH, "K", Role.KITCHEN_STAFF, false);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findById(kitchen.getId())).thenReturn(Optional.of(kitchen));

        service.markReady(order.getId(), kitchen.getId());

        assertThat(order.getState()).isEqualTo(OrderState.READY);
    }

    @Test
    void markServed_transitionsAndAudits() {
        Order order = placedOrderWithItems();
        order.startPreparation();
        order.markReady();
        User waiter = new User("w", HASH, "W", Role.WAITER, false);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findById(waiter.getId())).thenReturn(Optional.of(waiter));

        service.markServed(order.getId(), waiter.getId());

        assertThat(order.getState()).isEqualTo(OrderState.SERVED);
    }

    @Test
    void markReady_throws_whenOrderNotInPreparation() {
        Order order = placedOrderWithItems();
        User kitchen = new User("k", HASH, "K", Role.KITCHEN_STAFF, false);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findById(kitchen.getId())).thenReturn(Optional.of(kitchen));

        assertThatThrownBy(() -> service.markReady(order.getId(), kitchen.getId()))
                .isInstanceOf(IllegalStateException.class);
    }
}
