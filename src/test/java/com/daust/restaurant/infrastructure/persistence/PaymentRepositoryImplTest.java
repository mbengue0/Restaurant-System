package com.daust.restaurant.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daust.restaurant.domain.Bill;
import com.daust.restaurant.domain.BillId;
import com.daust.restaurant.domain.CategoryId;
import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.Payment;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.domain.PaymentRepository;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.User;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(PaymentRepositoryImpl.class)
class PaymentRepositoryImplTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Autowired
    private PaymentRepository paymentRepository;

    private static User manager() {
        return new User("mgr", HASH, "Manager", Role.MANAGER, false);
    }

    private static Bill billTotalling1780() {
        CategoryId category = CategoryId.generate();
        MenuItem pizza = new MenuItem("Pizza", null, new BigDecimal("1000.00"), category);
        Order order = new Order(TableId.generate());
        order.addItem(pizza, 1);
        Configuration cfg = new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("500.00"),
                false,
                EnumSet.of(PaymentMethod.CASH));
        return new Bill(order, cfg, Map.of(pizza.getId(), "Pizza"));
    }

    @Test
    void saveAndFindById_roundTripsAllFields() {
        Bill bill = billTotalling1780();
        User mgr = manager();
        Payment original = new Payment(bill, new BigDecimal("2000.00"), PaymentMethod.CASH, "txn-42", mgr);

        paymentRepository.save(original);

        Payment reloaded = paymentRepository.findById(original.getId()).orElseThrow();
        assertThat(reloaded.getId()).isEqualTo(original.getId());
        assertThat(reloaded.getBillId()).isEqualTo(bill.getId());
        assertThat(reloaded.getAmountDue()).isEqualByComparingTo("1780.00");
        assertThat(reloaded.getAmountPaid()).isEqualByComparingTo("2000.00");
        assertThat(reloaded.getMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(reloaded.getReference()).isEqualTo("txn-42");
        assertThat(reloaded.getRecordedBy()).isEqualTo(mgr.getId());
        assertThat(reloaded.getChangeDue()).isEqualByComparingTo("220.00");
        assertThat(reloaded.getRecordedAt()).isNotNull();
    }

    @Test
    void save_persistsNullReference() {
        Bill bill = billTotalling1780();
        Payment payment = new Payment(bill, new BigDecimal("1780.00"), PaymentMethod.CASH, null, manager());

        paymentRepository.save(payment);

        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(reloaded.getReference()).isNull();
    }

    @Test
    void findByBillId_returnsThePaymentForThatBill() {
        Bill bill = billTotalling1780();
        Payment payment = new Payment(bill, new BigDecimal("1780.00"), PaymentMethod.CARD, "card-99", manager());
        paymentRepository.save(payment);

        Optional<Payment> found = paymentRepository.findByBillId(bill.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(payment.getId());
    }

    @Test
    void findByBillId_returnsEmptyWhenNoPaymentExistsForBill() {
        Optional<Payment> found = paymentRepository.findByBillId(BillId.generate());

        assertThat(found).isEmpty();
    }

    @Test
    void save_rejectsSecondPaymentForSameBill_uk_bill_id() {
        Bill bill = billTotalling1780();
        paymentRepository.save(new Payment(bill, new BigDecimal("1780.00"), PaymentMethod.CASH, null, manager()));

        assertThatThrownBy(() -> {
            paymentRepository.save(new Payment(bill, new BigDecimal("2000.00"), PaymentMethod.CARD, null, manager()));
            // Force flush via a query that bypasses the persistence-context cache
            paymentRepository.findByBillId(BillId.generate());
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
