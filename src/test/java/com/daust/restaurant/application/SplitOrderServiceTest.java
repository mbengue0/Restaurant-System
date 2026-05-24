package com.daust.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Bill;
import com.daust.restaurant.domain.BillId;
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.CategoryId;
import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.ConfigurationRepository;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.MenuItemRepository;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderItem;
import com.daust.restaurant.domain.OrderItemId;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import java.math.BigDecimal;
import java.util.EnumSet;
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
class SplitOrderServiceTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock private OrderRepository orderRepository;
    @Mock private BillRepository billRepository;
    @Mock private ConfigurationRepository configurationRepository;
    @Mock private MenuItemRepository menuItemRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private SplitOrderService service;

    private static Configuration configWithSplitEnabled() {
        return new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("600.00"),
                true,
                EnumSet.of(PaymentMethod.CASH));
    }

    private static Configuration configWithSplitDisabled() {
        return new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("600.00"),
                false,
                EnumSet.of(PaymentMethod.CASH));
    }

    private static MenuItem item(String name, String price) {
        return new MenuItem(name, null, new BigDecimal(price), CategoryId.generate());
    }

    private static Order servedOrder(MenuItem... items) {
        Table table = new Table(4);
        table.seatCustomers();
        Order order = new Order(table.getId());
        for (MenuItem mi : items) {
            order.addItem(mi, 1);
        }
        order.submit();
        order.startPreparation();
        order.markReady();
        order.markServed();
        return order;
    }

    private static User manager() {
        return new User("mgr", HASH, "Manager", Role.MANAGER, false);
    }

    @Test
    void splitOrder_twoGroups_createsTwoBills_withCoverChargeDividedByTwo() {
        MenuItem pizza = item("Pizza", "1000.00");
        MenuItem soda = item("Soda", "500.00");
        Order order = servedOrder(pizza, soda);
        User mgr = manager();

        OrderItemId pizzaId = order.getItems().get(0).getId();
        OrderItemId sodaId = order.getItems().get(1).getId();

        when(configurationRepository.load()).thenReturn(Optional.of(configWithSplitEnabled()));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(billRepository.findByOrderId(order.getId())).thenReturn(List.of());
        when(userRepository.findById(mgr.getId())).thenReturn(Optional.of(mgr));
        when(menuItemRepository.findById(pizza.getId())).thenReturn(Optional.of(pizza));
        when(menuItemRepository.findById(soda.getId())).thenReturn(Optional.of(soda));

        List<BillId> ids = service.splitOrder(
                order.getId(),
                List.of(List.of(pizzaId), List.of(sodaId)),
                mgr.getId());

        assertThat(ids).hasSize(2);

        ArgumentCaptor<Bill> captor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<Bill> bills = captor.getAllValues();

        // Cover charge: 600 / 2 = 300 per bill
        for (Bill b : bills) {
            assertThat(b.getCoverChargeAmount()).isEqualByComparingTo("300.00");
            assertThat(b.getOrderIds()).containsExactly(order.getId());
            assertThat(b.getLineItems()).hasSize(1);
        }
        // Each bill carries its own item subtotal
        assertThat(bills.get(0).getItemsSubtotal()).isEqualByComparingTo("1000.00");
        assertThat(bills.get(1).getItemsSubtotal()).isEqualByComparingTo("500.00");
    }

    @Test
    void splitOrder_threeGroups_dividesCoverChargeByThree_andEmitsAudit() {
        MenuItem a = item("A", "1000.00");
        MenuItem b = item("B", "1000.00");
        MenuItem c = item("C", "1000.00");
        Order order = servedOrder(a, b, c);
        User mgr = manager();

        OrderItemId aId = order.getItems().get(0).getId();
        OrderItemId bId = order.getItems().get(1).getId();
        OrderItemId cId = order.getItems().get(2).getId();

        when(configurationRepository.load()).thenReturn(Optional.of(configWithSplitEnabled()));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(billRepository.findByOrderId(order.getId())).thenReturn(List.of());
        when(userRepository.findById(mgr.getId())).thenReturn(Optional.of(mgr));
        when(menuItemRepository.findById(a.getId())).thenReturn(Optional.of(a));
        when(menuItemRepository.findById(b.getId())).thenReturn(Optional.of(b));
        when(menuItemRepository.findById(c.getId())).thenReturn(Optional.of(c));

        List<BillId> ids = service.splitOrder(
                order.getId(),
                List.of(List.of(aId), List.of(bId), List.of(cId)),
                mgr.getId());

        assertThat(ids).hasSize(3);

        ArgumentCaptor<Bill> billCaptor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository, org.mockito.Mockito.times(3)).save(billCaptor.capture());
        // 600 / 3 = 200.00
        for (Bill bill : billCaptor.getAllValues()) {
            assertThat(bill.getCoverChargeAmount()).isEqualByComparingTo("200.00");
        }

        ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLogEntry entry = auditCaptor.getValue();
        assertThat(entry.getEventType()).isEqualTo("ORDER_SPLIT");
        assertThat(entry.getAffectedEntityType()).isEqualTo("Order");
        assertThat(entry.getAffectedEntityId()).isEqualTo(order.getId().value().toString());
        assertThat(entry.getAfterValue()).contains("splitDivisor=3");
    }

    @Test
    void splitOrder_groupsAssignItemsToTheirRespectiveBills() {
        MenuItem pizza = item("Pizza", "1000.00");
        MenuItem soda = item("Soda", "500.00");
        MenuItem dessert = item("Dessert", "700.00");
        Order order = servedOrder(pizza, soda, dessert);
        User mgr = manager();

        OrderItemId pizzaId = order.getItems().get(0).getId();
        OrderItemId sodaId = order.getItems().get(1).getId();
        OrderItemId dessertId = order.getItems().get(2).getId();

        when(configurationRepository.load()).thenReturn(Optional.of(configWithSplitEnabled()));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(billRepository.findByOrderId(order.getId())).thenReturn(List.of());
        when(userRepository.findById(mgr.getId())).thenReturn(Optional.of(mgr));
        when(menuItemRepository.findById(pizza.getId())).thenReturn(Optional.of(pizza));
        when(menuItemRepository.findById(soda.getId())).thenReturn(Optional.of(soda));
        when(menuItemRepository.findById(dessert.getId())).thenReturn(Optional.of(dessert));

        // Group 1: pizza + soda. Group 2: dessert.
        service.splitOrder(
                order.getId(),
                List.of(List.of(pizzaId, sodaId), List.of(dessertId)),
                mgr.getId());

        ArgumentCaptor<Bill> captor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<Bill> bills = captor.getAllValues();
        assertThat(bills.get(0).getLineItems()).hasSize(2);
        assertThat(bills.get(0).getItemsSubtotal()).isEqualByComparingTo("1500.00");
        assertThat(bills.get(1).getLineItems()).hasSize(1);
        assertThat(bills.get(1).getItemsSubtotal()).isEqualByComparingTo("700.00");
    }

    @Test
    void splitOrder_throws_whenSplitMergePolicyDisabled() {
        Order order = servedOrder(item("Pizza", "1000.00"));
        User mgr = manager();
        OrderItemId itemId = order.getItems().get(0).getId();
        when(configurationRepository.load()).thenReturn(Optional.of(configWithSplitDisabled()));

        assertThatThrownBy(() -> service.splitOrder(
                        order.getId(),
                        List.of(List.of(itemId), List.of(itemId)),
                        mgr.getId()))
                .isInstanceOf(SplitMergeDisabledException.class);

        verify(billRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void splitOrder_throws_whenItemAppearsInTwoGroups() {
        MenuItem pizza = item("Pizza", "1000.00");
        MenuItem soda = item("Soda", "500.00");
        Order order = servedOrder(pizza, soda);
        User mgr = manager();
        OrderItemId pizzaId = order.getItems().get(0).getId();
        OrderItemId sodaId = order.getItems().get(1).getId();

        when(configurationRepository.load()).thenReturn(Optional.of(configWithSplitEnabled()));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(billRepository.findByOrderId(order.getId())).thenReturn(List.of());
        when(userRepository.findById(mgr.getId())).thenReturn(Optional.of(mgr));

        assertThatThrownBy(() -> service.splitOrder(
                        order.getId(),
                        List.of(List.of(pizzaId, sodaId), List.of(pizzaId)),
                        mgr.getId()))
                .isInstanceOf(InvalidSplitException.class)
                .hasMessageContaining("more than one group");

        verify(billRepository, never()).save(any());
    }

    @Test
    void splitOrder_throws_whenAnItemIsMissingFromAllGroups() {
        MenuItem pizza = item("Pizza", "1000.00");
        MenuItem soda = item("Soda", "500.00");
        MenuItem dessert = item("Dessert", "700.00");
        Order order = servedOrder(pizza, soda, dessert);
        User mgr = manager();
        OrderItemId pizzaId = order.getItems().get(0).getId();
        OrderItemId sodaId = order.getItems().get(1).getId();
        // dessert is not assigned to any group

        when(configurationRepository.load()).thenReturn(Optional.of(configWithSplitEnabled()));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(billRepository.findByOrderId(order.getId())).thenReturn(List.of());
        when(userRepository.findById(mgr.getId())).thenReturn(Optional.of(mgr));

        assertThatThrownBy(() -> service.splitOrder(
                        order.getId(),
                        List.of(List.of(pizzaId), List.of(sodaId)),
                        mgr.getId()))
                .isInstanceOf(InvalidSplitException.class)
                .hasMessageContaining("missing from split");

        verify(billRepository, never()).save(any());
    }

    @Test
    void splitOrder_throws_whenUnknownOrderItemId() {
        MenuItem pizza = item("Pizza", "1000.00");
        MenuItem soda = item("Soda", "500.00");
        Order order = servedOrder(pizza, soda);
        User mgr = manager();
        OrderItemId pizzaId = order.getItems().get(0).getId();
        OrderItemId ghost = OrderItemId.of(UUID.randomUUID());

        when(configurationRepository.load()).thenReturn(Optional.of(configWithSplitEnabled()));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(billRepository.findByOrderId(order.getId())).thenReturn(List.of());
        when(userRepository.findById(mgr.getId())).thenReturn(Optional.of(mgr));

        assertThatThrownBy(() -> service.splitOrder(
                        order.getId(),
                        List.of(List.of(pizzaId), List.of(ghost)),
                        mgr.getId()))
                .isInstanceOf(InvalidSplitException.class)
                .hasMessageContaining("Unknown OrderItem");
    }

    @Test
    void splitOrder_throws_whenEmptyGroupProvided() {
        MenuItem pizza = item("Pizza", "1000.00");
        Order order = servedOrder(pizza);
        User mgr = manager();
        OrderItemId pizzaId = order.getItems().get(0).getId();

        when(configurationRepository.load()).thenReturn(Optional.of(configWithSplitEnabled()));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(billRepository.findByOrderId(order.getId())).thenReturn(List.of());
        when(userRepository.findById(mgr.getId())).thenReturn(Optional.of(mgr));

        assertThatThrownBy(() -> service.splitOrder(
                        order.getId(),
                        List.of(List.of(pizzaId), List.of()),
                        mgr.getId()))
                .isInstanceOf(InvalidSplitException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void splitOrder_throws_whenFewerThanTwoGroups() {
        Order order = servedOrder(item("Pizza", "1000.00"));
        UserId mgrId = UserId.of(UUID.randomUUID());
        OrderItemId pizzaId = order.getItems().get(0).getId();

        assertThatThrownBy(() ->
                        service.splitOrder(order.getId(), List.of(List.of(pizzaId)), mgrId))
                .isInstanceOf(InvalidSplitException.class)
                .hasMessageContaining("at least 2 groups");
    }

    @Test
    void splitOrder_throws_whenOrderNotServed() {
        MenuItem pizza = item("Pizza", "1000.00");
        Table table = new Table(4);
        table.seatCustomers();
        Order order = new Order(table.getId()); // PLACED state
        order.addItem(pizza, 1);
        User mgr = manager();
        OrderItemId pizzaId = order.getItems().get(0).getId();

        when(configurationRepository.load()).thenReturn(Optional.of(configWithSplitEnabled()));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.splitOrder(
                        order.getId(),
                        List.of(List.of(pizzaId), List.of(pizzaId)),
                        mgr.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SERVED");

        verify(billRepository, never()).save(any());
    }

    @Test
    void splitOrder_throws_whenBillAlreadyExists() {
        MenuItem pizza = item("Pizza", "1000.00");
        Order order = servedOrder(pizza);
        Bill existing = new Bill(order, configWithSplitEnabled(), java.util.Map.of(pizza.getId(), pizza.getName()));
        User mgr = manager();
        OrderItemId pizzaId = order.getItems().get(0).getId();

        when(configurationRepository.load()).thenReturn(Optional.of(configWithSplitEnabled()));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(billRepository.findByOrderId(order.getId())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.splitOrder(
                        order.getId(),
                        List.of(List.of(pizzaId), List.of(pizzaId)),
                        mgr.getId()))
                .isInstanceOf(BillAlreadyGeneratedException.class);

        verify(billRepository, never()).save(any());
    }

    @Test
    void splitOrder_throws_whenOrderMissing() {
        OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
        com.daust.restaurant.domain.OrderId orderId =
                com.daust.restaurant.domain.OrderId.of(UUID.randomUUID());

        when(configurationRepository.load()).thenReturn(Optional.of(configWithSplitEnabled()));
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.splitOrder(
                        orderId, List.of(List.of(itemId), List.of(itemId)), UserId.of(UUID.randomUUID())))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
