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
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.CategoryId;
import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.ConfigurationRepository;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.OrderState;
import com.daust.restaurant.domain.Payment;
import com.daust.restaurant.domain.PaymentId;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.domain.PaymentRepository;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.domain.TableStatus;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserRepository;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordPaymentServiceTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock private BillRepository billRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private TableRepository tableRepository;
    @Mock private UserRepository userRepository;
    @Mock private ConfigurationRepository configurationRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private RecordPaymentService service;

    private Order servedOrder(Table table, MenuItem item) {
        Order order = new Order(table.getId());
        order.addItem(item, 1);
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
    void recordPayment_happyPath_completesOrderReleasesTablePaysBillAndAudits() {
        Table table = new Table(4);
        table.seatCustomers();
        MenuItem item = new MenuItem("Thieb", null, new BigDecimal("10000"), CategoryId.generate());
        Order order = servedOrder(table, item);
        Configuration config = defaultConfig();
        Bill bill = new Bill(order, config, Map.of(item.getId(), item.getName()));
        User manager = new User("m", HASH, "M", Role.MANAGER, false);

        when(billRepository.findById(bill.getId())).thenReturn(Optional.of(bill));
        when(paymentRepository.findByBillId(bill.getId())).thenReturn(Optional.empty());
        when(configurationRepository.load()).thenReturn(Optional.of(config));
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(tableRepository.findById(table.getId())).thenReturn(Optional.of(table));

        PaymentId paymentId = service.recordPayment(
                bill.getId(),
                new BigDecimal("15000.00"),
                PaymentMethod.CASH,
                "REF-1",
                manager.getId());

        assertThat(paymentId).isNotNull();
        assertThat(order.getState()).isEqualTo(OrderState.COMPLETED);
        assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(bill.isPaid()).isTrue();
        verify(paymentRepository).save(any(Payment.class));
        verify(orderRepository).save(order);
        verify(tableRepository).save(table);
        verify(billRepository).save(bill);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(AuditLogEntry::getEventType)
                .containsExactly("PAYMENT_RECORDED", "ORDER_COMPLETED", "TABLE_RELEASED");
    }

    @Test
    void recordPayment_throws_whenMethodNotAccepted() {
        Table table = new Table(2);
        table.seatCustomers();
        MenuItem item = new MenuItem("Thieb", null, new BigDecimal("5000"), CategoryId.generate());
        Order order = servedOrder(table, item);
        Configuration config = defaultConfig();
        Bill bill = new Bill(order, config, Map.of(item.getId(), item.getName()));

        when(billRepository.findById(bill.getId())).thenReturn(Optional.of(bill));
        when(paymentRepository.findByBillId(bill.getId())).thenReturn(Optional.empty());
        when(configurationRepository.load()).thenReturn(Optional.of(config));

        assertThatThrownBy(() -> service.recordPayment(
                bill.getId(),
                new BigDecimal("10000.00"),
                PaymentMethod.MOBILE_MONEY,
                "REF",
                com.daust.restaurant.domain.UserId.generate()))
                .isInstanceOf(PaymentMethodNotAcceptedException.class);

        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        verify(tableRepository, never()).save(any());
    }

    @Test
    void recordPayment_throws_whenBillAlreadyPaid() {
        Table table = new Table(2);
        table.seatCustomers();
        MenuItem item = new MenuItem("Thieb", null, new BigDecimal("5000"), CategoryId.generate());
        Order order = servedOrder(table, item);
        Bill bill = new Bill(order, defaultConfig(), Map.of(item.getId(), item.getName()));
        bill.markPaid();
        when(billRepository.findById(bill.getId())).thenReturn(Optional.of(bill));

        assertThatThrownBy(() -> service.recordPayment(
                bill.getId(),
                new BigDecimal("10000.00"),
                PaymentMethod.CASH,
                "REF",
                com.daust.restaurant.domain.UserId.generate()))
                .isInstanceOf(BillAlreadyPaidException.class);
    }
}
