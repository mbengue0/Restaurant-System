package com.daust.restaurant.application;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Bill;
import com.daust.restaurant.domain.BillId;
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.ConfigurationRepository;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderId;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.Payment;
import com.daust.restaurant.domain.PaymentId;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.domain.PaymentRepository;
import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordPaymentService {

    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TableRepository tableRepository;
    private final UserRepository userRepository;
    private final ConfigurationRepository configurationRepository;
    private final AuditLogRepository auditLogRepository;

    public RecordPaymentService(
            BillRepository billRepository,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            TableRepository tableRepository,
            UserRepository userRepository,
            ConfigurationRepository configurationRepository,
            AuditLogRepository auditLogRepository) {
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.tableRepository = tableRepository;
        this.userRepository = userRepository;
        this.configurationRepository = configurationRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public PaymentId recordPayment(
            BillId billId,
            BigDecimal amountPaid,
            PaymentMethod method,
            String reference,
            UserId managerId) {
        Bill bill = billRepository
                .findById(billId)
                .orElseThrow(() -> new BillNotFoundException("Bill not found: " + billId));

        if (bill.isPaid()) {
            throw new BillAlreadyPaidException("Bill " + bill.getBillNumber() + " is already paid");
        }
        if (paymentRepository.findByBillId(billId).isPresent()) {
            throw new BillAlreadyPaidException(
                    "Payment already recorded for Bill " + bill.getBillNumber());
        }

        Configuration configuration = configurationRepository
                .load()
                .orElseThrow(() -> new IllegalStateException("Configuration singleton not initialized"));

        if (!configuration.getAcceptedPaymentMethods().contains(method)) {
            throw new PaymentMethodNotAcceptedException(
                    "Payment method " + method + " is not currently accepted (BR1)");
        }

        User manager = userRepository
                .findById(managerId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + managerId));

        Payment payment = new Payment(bill, amountPaid, method, reference, manager);
        paymentRepository.save(payment);

        OrderId orderId = bill.getOrderIds().iterator().next();
        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        String orderBefore = order.getState().name();
        order.markCompleted();
        orderRepository.save(order);

        Table table = tableRepository
                .findById(order.getTableId())
                .orElseThrow(() -> new TableNotFoundException("Table not found: " + order.getTableId()));
        String tableBefore = table.getStatus().name();
        table.markAvailable();
        tableRepository.save(table);

        bill.markPaid();
        billRepository.save(bill);

        auditLogRepository.save(new AuditLogEntry(
                manager.getId(),
                manager.getRole(),
                "PAYMENT_RECORDED",
                "Payment",
                payment.getId().value().toString(),
                "bill=" + bill.getBillNumber() + ", total=" + bill.getTotal(),
                "method=" + method + ", paid=" + payment.getAmountPaid()
                        + ", change=" + payment.getChangeDue()));
        auditLogRepository.save(new AuditLogEntry(
                manager.getId(),
                manager.getRole(),
                "ORDER_COMPLETED",
                "Order",
                order.getId().value().toString(),
                orderBefore,
                order.getState().name()));
        auditLogRepository.save(new AuditLogEntry(
                manager.getId(),
                manager.getRole(),
                "TABLE_RELEASED",
                "Table",
                table.getId().value().toString(),
                tableBefore,
                table.getStatus().name()));

        return payment.getId();
    }
}
