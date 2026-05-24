package com.daust.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.CancellationReason;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderId;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.OrderState;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelOrderServiceTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private TableRepository tableRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private CancelOrderService cancelOrderService;

    @Test
    void cancelOrder_succeeds_whenWaiterCancelsPlacedOrder() {
        Order order = new Order(TableId.generate());
        User waiter = new User("alice", HASH, "Alice", Role.WAITER, false);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findById(waiter.getId())).thenReturn(Optional.of(waiter));

        cancelOrderService.cancelOrder(
                order.getId(), CancellationReason.CUSTOMER_LEFT, "left after 5min", waiter.getId());

        assertThat(order.getState()).isEqualTo(OrderState.CANCELLED);
        assertThat(order.getCancellationReason()).isEqualTo(CancellationReason.CUSTOMER_LEFT);
        assertThat(order.getCancelledBy()).isEqualTo(waiter.getId());
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_succeeds_whenManagerCancelsInPreparationOrder() {
        Order order = orderInState(OrderState.IN_PREPARATION);
        User manager = new User("bob", HASH, "Bob", Role.MANAGER, false);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));

        cancelOrderService.cancelOrder(
                order.getId(), CancellationReason.KITCHEN_ERROR, "burned", manager.getId());

        assertThat(order.getState()).isEqualTo(OrderState.CANCELLED);
        assertThat(order.getCancellationReason()).isEqualTo(CancellationReason.KITCHEN_ERROR);
        assertThat(order.getCancelledBy()).isEqualTo(manager.getId());
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_throws_whenWaiterTriesToCancelInPreparationOrder_andDoesNotSave() {
        Order order = orderInState(OrderState.IN_PREPARATION);
        User waiter = new User("alice", HASH, "Alice", Role.WAITER, false);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findById(waiter.getId())).thenReturn(Optional.of(waiter));

        assertThatThrownBy(() -> cancelOrderService.cancelOrder(
                        order.getId(), CancellationReason.OTHER, null, waiter.getId()))
                .isInstanceOf(com.daust.restaurant.domain.UnauthorizedException.class)
                .hasMessageContaining("MANAGER");

        assertThat(order.getState()).isEqualTo(OrderState.IN_PREPARATION);
        verify(orderRepository, never()).save(order);
        verify(auditLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cancelOrder_writesAuditEntry_withBeforeStateAndReason() {
        Order order = new Order(TableId.generate());
        User waiter = new User("alice", HASH, "Alice", Role.WAITER, false);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(userRepository.findById(waiter.getId())).thenReturn(Optional.of(waiter));

        cancelOrderService.cancelOrder(
                order.getId(), CancellationReason.WRONG_ORDER, "double-tap", waiter.getId());

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntry entry = captor.getValue();
        assertThat(entry.getEventType()).isEqualTo("ORDER_CANCELLED");
        assertThat(entry.getAffectedEntityType()).isEqualTo("Order");
        assertThat(entry.getAffectedEntityId()).isEqualTo(order.getId().value().toString());
        assertThat(entry.getBeforeValue()).isEqualTo("PLACED");
        assertThat(entry.getAfterValue()).contains("CANCELLED").contains("WRONG_ORDER");
        assertThat(entry.getUserId()).isEqualTo(waiter.getId());
        assertThat(entry.getUserRoleAtTime()).isEqualTo(Role.WAITER);
    }

    @Test
    void cancelOrder_throwsOrderNotFound_whenOrderMissing() {
        OrderId orderId = OrderId.generate();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cancelOrderService.cancelOrder(
                        orderId, CancellationReason.OTHER, null, com.daust.restaurant.domain.UserId.generate()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    private static Order orderInState(OrderState state) {
        return Order.reconstitute(
                OrderId.generate(),
                TableId.generate(),
                state,
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().minusMinutes(9),
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                new ArrayList<>());
    }
}
