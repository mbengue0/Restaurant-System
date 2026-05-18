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
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserRepository;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenerateBillServiceTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock private OrderRepository orderRepository;
    @Mock private BillRepository billRepository;
    @Mock private ConfigurationRepository configurationRepository;
    @Mock private MenuItemRepository menuItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private GenerateBillService service;

    private Order servedOrder(MenuItem item) {
        Table table = new Table(4);
        table.seatCustomers();
        Order order = new Order(table.getId());
        order.addItem(item, 2);
        order.submit();
        order.startPreparation();
        order.markReady();
        order.markServed();
        return order;
    }

    private Configuration defaultConfig() {
        return new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                BigDecimal.ZERO,
                false,
                EnumSet.of(PaymentMethod.CASH, PaymentMethod.CARD));
    }

    @Test
    void generateBill_buildsBillAndAudits_onHappyPath() {
        MenuItem item = new MenuItem("Thieb", null, new BigDecimal("8500"), CategoryId.generate());
        Order order = servedOrder(item);
        User manager = new User("m", HASH, "M", Role.MANAGER, false);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(billRepository.findByOrderId(order.getId())).thenReturn(List.of());
        when(configurationRepository.load()).thenReturn(Optional.of(defaultConfig()));
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        BillId billId = service.generateBill(order.getId(), manager.getId());

        assertThat(billId).isNotNull();
        ArgumentCaptor<Bill> billCaptor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository).save(billCaptor.capture());
        Bill bill = billCaptor.getValue();
        assertThat(bill.getOrderIds()).containsExactly(order.getId());
        assertThat(bill.getLineItems()).hasSize(1);
        ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getEventType()).isEqualTo("BILL_GENERATED");
    }

    @Test
    void generateBill_throws_whenOrderNotServed() {
        MenuItem item = new MenuItem("Thieb", null, new BigDecimal("8500"), CategoryId.generate());
        Table table = new Table(4);
        table.seatCustomers();
        Order order = new Order(table.getId());
        order.addItem(item, 1);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.generateBill(order.getId(), com.daust.restaurant.domain.UserId.generate()))
                .isInstanceOf(IllegalStateException.class);
        verify(billRepository, never()).save(any());
    }

    @Test
    void generateBill_throws_whenBillAlreadyExists() {
        MenuItem item = new MenuItem("Thieb", null, new BigDecimal("8500"), CategoryId.generate());
        Order order = servedOrder(item);
        Bill existing = new Bill(order, defaultConfig(), java.util.Map.of(item.getId(), item.getName()));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(billRepository.findByOrderId(order.getId())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.generateBill(order.getId(), com.daust.restaurant.domain.UserId.generate()))
                .isInstanceOf(BillAlreadyGeneratedException.class);
        verify(billRepository, never()).save(any());
    }
}
