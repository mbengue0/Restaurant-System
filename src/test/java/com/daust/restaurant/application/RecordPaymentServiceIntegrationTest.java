package com.daust.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Bill;
import com.daust.restaurant.domain.BillId;
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.MenuItemRepository;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderId;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.OrderState;
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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RecordPaymentServiceIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private TableRepository tableRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private BillRepository billRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @Autowired private SeatCustomersService seatCustomersService;
    @Autowired private PlaceOrderService placeOrderService;
    @Autowired private OrderLifecycleService orderLifecycleService;
    @Autowired private GenerateBillService generateBillService;
    @Autowired private RecordPaymentService recordPaymentService;

    @Test
    void fullHappyPath_seedThroughPayment_leavesExpectedDatabaseState() {
        User waiter = userRepository.findByUsername("waiter").orElseThrow();
        User kitchen = userRepository.findByUsername("kitchen").orElseThrow();
        User manager = userRepository.findByUsername("manager").orElseThrow();
        assertThat(waiter.getRole()).isEqualTo(Role.WAITER);
        assertThat(kitchen.getRole()).isEqualTo(Role.KITCHEN_STAFF);
        assertThat(manager.getRole()).isEqualTo(Role.MANAGER);

        Table table = tableRepository.findAll().stream()
                .filter(t -> t.getStatus() == TableStatus.AVAILABLE)
                .findFirst()
                .orElseThrow();
        MenuItem menuItem = menuItemRepository.findAll().get(0);

        seatCustomersService.seatTable(table.getId(), waiter.getId());

        OrderId orderId = placeOrderService.startOrder(table.getId(), waiter.getId());
        placeOrderService.addItemToOrder(orderId, menuItem.getId(), 2, waiter.getId());
        placeOrderService.submitOrder(orderId, waiter.getId());

        orderLifecycleService.startPreparation(orderId, kitchen.getId());
        orderLifecycleService.markReady(orderId, kitchen.getId());
        orderLifecycleService.markServed(orderId, waiter.getId());

        BillId billId = generateBillService.generateBill(orderId, manager.getId());
        Bill bill = billRepository.findById(billId).orElseThrow();
        BigDecimal amountPaid = bill.getTotal().add(new BigDecimal("100.00"));

        PaymentId paymentId = recordPaymentService.recordPayment(
                billId, amountPaid, PaymentMethod.CASH, "CASH-IT-1", manager.getId());

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getState()).isEqualTo(OrderState.COMPLETED);

        Table reloaded = tableRepository.findById(table.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TableStatus.AVAILABLE);

        Bill reloadedBill = billRepository.findById(billId).orElseThrow();
        assertThat(reloadedBill.isPaid()).isTrue();

        assertThat(paymentRepository.findById(paymentId)).isPresent();
        assertThat(paymentRepository.findByBillId(billId)).isPresent();

        List<AuditLogEntry> paymentAudit = auditLogRepository.findByEventType("PAYMENT_RECORDED");
        assertThat(paymentAudit).isNotEmpty();
        List<AuditLogEntry> orderCompletedAudit = auditLogRepository.findByEventType("ORDER_COMPLETED");
        assertThat(orderCompletedAudit).isNotEmpty();
        List<AuditLogEntry> tableReleasedAudit = auditLogRepository.findByEventType("TABLE_RELEASED");
        assertThat(tableReleasedAudit).isNotEmpty();
    }
}
