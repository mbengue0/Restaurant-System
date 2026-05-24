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
import com.daust.restaurant.domain.OrderId;
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
class MergeOrdersServiceTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock private OrderRepository orderRepository;
    @Mock private BillRepository billRepository;
    @Mock private ConfigurationRepository configurationRepository;
    @Mock private MenuItemRepository menuItemRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private MergeOrdersService service;

    private static Configuration configWithMergeEnabled() {
        return new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("500.00"),
                true,
                EnumSet.of(PaymentMethod.CASH));
    }

    private static Configuration configWithMergeDisabled() {
        return new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("500.00"),
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
    void mergeOrders_twoServedOrders_createsOneBill_withCombinedItemsAndBR3Totals() {
        MenuItem pizza = item("Pizza", "1000.00");
        MenuItem soda = item("Soda", "500.00");
        Order o1 = servedOrder(pizza);
        Order o2 = servedOrder(soda);
        User mgr = manager();

        when(configurationRepository.load()).thenReturn(Optional.of(configWithMergeEnabled()));
        when(userRepository.findById(mgr.getId())).thenReturn(Optional.of(mgr));
        when(orderRepository.findById(o1.getId())).thenReturn(Optional.of(o1));
        when(orderRepository.findById(o2.getId())).thenReturn(Optional.of(o2));
        when(billRepository.findByOrderId(o1.getId())).thenReturn(List.of());
        when(billRepository.findByOrderId(o2.getId())).thenReturn(List.of());
        when(menuItemRepository.findById(pizza.getId())).thenReturn(Optional.of(pizza));
        when(menuItemRepository.findById(soda.getId())).thenReturn(Optional.of(soda));

        BillId billId = service.mergeOrders(List.of(o1.getId(), o2.getId()), mgr.getId());

        assertThat(billId).isNotNull();
        ArgumentCaptor<Bill> captor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository).save(captor.capture());
        Bill bill = captor.getValue();

        // Bridge: both order IDs present
        assertThat(bill.getOrderIds()).containsExactlyInAnyOrder(o1.getId(), o2.getId());
        // Items: pizza + soda combined
        assertThat(bill.getLineItems()).hasSize(2);
        // Subtotal: 1000 + 500 = 1500
        assertThat(bill.getItemsSubtotal()).isEqualByComparingTo("1500.00");
        // Tax: 1500 * 0.18 = 270
        assertThat(bill.getTaxAmount()).isEqualByComparingTo("270.00");
        // Service: 1500 * 0.10 = 150
        assertThat(bill.getServiceChargeAmount()).isEqualByComparingTo("150.00");
        // Cover: 500 (applied once, NOT divided)
        assertThat(bill.getCoverChargeAmount()).isEqualByComparingTo("500.00");
        // Total: 1500 + 270 + 150 + 500 = 2420
        assertThat(bill.getTotal()).isEqualByComparingTo("2420.00");
    }

    @Test
    void mergeOrders_writesOrdersMergedAuditEntry() {
        MenuItem pizza = item("Pizza", "1000.00");
        MenuItem soda = item("Soda", "500.00");
        Order o1 = servedOrder(pizza);
        Order o2 = servedOrder(soda);
        User mgr = manager();

        when(configurationRepository.load()).thenReturn(Optional.of(configWithMergeEnabled()));
        when(userRepository.findById(mgr.getId())).thenReturn(Optional.of(mgr));
        when(orderRepository.findById(o1.getId())).thenReturn(Optional.of(o1));
        when(orderRepository.findById(o2.getId())).thenReturn(Optional.of(o2));
        when(billRepository.findByOrderId(any())).thenReturn(List.of());
        when(menuItemRepository.findById(pizza.getId())).thenReturn(Optional.of(pizza));
        when(menuItemRepository.findById(soda.getId())).thenReturn(Optional.of(soda));

        service.mergeOrders(List.of(o1.getId(), o2.getId()), mgr.getId());

        ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLogEntry entry = auditCaptor.getValue();
        assertThat(entry.getEventType()).isEqualTo("ORDERS_MERGED");
        assertThat(entry.getAffectedEntityType()).isEqualTo("Bill");
        assertThat(entry.getUserId()).isEqualTo(mgr.getId());
        assertThat(entry.getUserRoleAtTime()).isEqualTo(Role.MANAGER);
        assertThat(entry.getAfterValue()).contains("count=2");
    }

    @Test
    void mergeOrders_threeServedOrders_combinesAllItems() {
        MenuItem a = item("A", "1000.00");
        MenuItem b = item("B", "1000.00");
        MenuItem c = item("C", "1000.00");
        Order o1 = servedOrder(a);
        Order o2 = servedOrder(b);
        Order o3 = servedOrder(c);
        User mgr = manager();

        when(configurationRepository.load()).thenReturn(Optional.of(configWithMergeEnabled()));
        when(userRepository.findById(mgr.getId())).thenReturn(Optional.of(mgr));
        when(orderRepository.findById(o1.getId())).thenReturn(Optional.of(o1));
        when(orderRepository.findById(o2.getId())).thenReturn(Optional.of(o2));
        when(orderRepository.findById(o3.getId())).thenReturn(Optional.of(o3));
        when(billRepository.findByOrderId(any())).thenReturn(List.of());
        when(menuItemRepository.findById(a.getId())).thenReturn(Optional.of(a));
        when(menuItemRepository.findById(b.getId())).thenReturn(Optional.of(b));
        when(menuItemRepository.findById(c.getId())).thenReturn(Optional.of(c));

        service.mergeOrders(List.of(o1.getId(), o2.getId(), o3.getId()), mgr.getId());

        ArgumentCaptor<Bill> captor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository).save(captor.capture());
        Bill bill = captor.getValue();
        assertThat(bill.getOrderIds())
                .containsExactlyInAnyOrder(o1.getId(), o2.getId(), o3.getId());
        assertThat(bill.getLineItems()).hasSize(3);
        assertThat(bill.getItemsSubtotal()).isEqualByComparingTo("3000.00");
        // Cover charge applied once, not multiplied by 3
        assertThat(bill.getCoverChargeAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void mergeOrders_throws_whenSplitMergePolicyDisabled() {
        OrderId o1 = OrderId.generate();
        OrderId o2 = OrderId.generate();
        when(configurationRepository.load()).thenReturn(Optional.of(configWithMergeDisabled()));

        assertThatThrownBy(() -> service.mergeOrders(List.of(o1, o2), UserId.of(UUID.randomUUID())))
                .isInstanceOf(SplitMergeDisabledException.class);

        verify(billRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void mergeOrders_throws_whenOnlyOneOrderProvided() {
        assertThatThrownBy(() -> service.mergeOrders(
                        List.of(OrderId.generate()), UserId.of(UUID.randomUUID())))
                .isInstanceOf(InvalidMergeException.class)
                .hasMessageContaining("at least 2 orders");
    }

    @Test
    void mergeOrders_throws_whenEmptyList() {
        assertThatThrownBy(() -> service.mergeOrders(List.of(), UserId.of(UUID.randomUUID())))
                .isInstanceOf(InvalidMergeException.class);
    }

    @Test
    void mergeOrders_throws_whenDuplicateOrderIds() {
        OrderId duplicate = OrderId.generate();
        assertThatThrownBy(() -> service.mergeOrders(
                        List.of(duplicate, duplicate), UserId.of(UUID.randomUUID())))
                .isInstanceOf(InvalidMergeException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void mergeOrders_throws_whenAnyOrderNotServed() {
        MenuItem pizza = item("Pizza", "1000.00");
        Order served = servedOrder(pizza);
        Table table = new Table(4);
        table.seatCustomers();
        Order notServed = new Order(table.getId());
        notServed.addItem(pizza, 1); // PLACED state
        User mgr = manager();

        when(configurationRepository.load()).thenReturn(Optional.of(configWithMergeEnabled()));
        when(userRepository.findById(mgr.getId())).thenReturn(Optional.of(mgr));
        when(orderRepository.findById(served.getId())).thenReturn(Optional.of(served));
        when(orderRepository.findById(notServed.getId())).thenReturn(Optional.of(notServed));
        when(billRepository.findByOrderId(served.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.mergeOrders(
                        List.of(served.getId(), notServed.getId()), mgr.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SERVED");

        verify(billRepository, never()).save(any());
    }

    @Test
    void mergeOrders_throws_whenAnyOrderAlreadyHasBill() {
        MenuItem pizza = item("Pizza", "1000.00");
        MenuItem soda = item("Soda", "500.00");
        Order o1 = servedOrder(pizza);
        Order o2 = servedOrder(soda);
        Bill existing = new Bill(o2, configWithMergeEnabled(), java.util.Map.of(soda.getId(), soda.getName()));
        User mgr = manager();

        when(configurationRepository.load()).thenReturn(Optional.of(configWithMergeEnabled()));
        when(userRepository.findById(mgr.getId())).thenReturn(Optional.of(mgr));
        when(orderRepository.findById(o1.getId())).thenReturn(Optional.of(o1));
        when(orderRepository.findById(o2.getId())).thenReturn(Optional.of(o2));
        when(billRepository.findByOrderId(o1.getId())).thenReturn(List.of());
        when(billRepository.findByOrderId(o2.getId())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.mergeOrders(
                        List.of(o1.getId(), o2.getId()), mgr.getId()))
                .isInstanceOf(BillAlreadyGeneratedException.class);

        verify(billRepository, never()).save(any());
    }

    @Test
    void mergeOrders_throws_whenOrderMissing() {
        OrderId missing = OrderId.of(UUID.randomUUID());
        OrderId other = OrderId.of(UUID.randomUUID());
        User mgr = manager();

        when(configurationRepository.load()).thenReturn(Optional.of(configWithMergeEnabled()));
        when(userRepository.findById(mgr.getId())).thenReturn(Optional.of(mgr));
        when(orderRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.mergeOrders(List.of(missing, other), mgr.getId()))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
