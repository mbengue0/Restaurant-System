package com.daust.restaurant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PaymentTest {

    private static final CategoryId CATEGORY = CategoryId.generate();
    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    // -------- helpers --------

    private static User userWith(Role role) {
        return new User("u_" + role.name().toLowerCase(), HASH, "Name " + role.name(), role, false);
    }

    private static Configuration sampleConfig() {
        return new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("500.00"),
                false,
                EnumSet.of(PaymentMethod.CASH));
    }

    /** Builds a Bill whose total is 1780.00 (pizza @ 1000.00 + 18% tax + 10% service + 500 cover). */
    private static Bill billTotalling1780() {
        MenuItem pizza = new MenuItem("Pizza", null, new BigDecimal("1000.00"), CATEGORY);
        Order order = new Order(TableId.generate());
        order.addItem(pizza, 1);
        return new Bill(order, sampleConfig(), Map.of(pizza.getId(), "Pizza"));
    }

    // -------- happy path --------

    @Test
    void newPayment_capturesAllFieldsCorrectly() {
        Bill bill = billTotalling1780();
        User manager = userWith(Role.MANAGER);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        Payment payment = new Payment(bill, new BigDecimal("2000.00"), PaymentMethod.CASH, "txn-42", manager);

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getId().value()).isNotNull();
        assertThat(payment.getBillId()).isEqualTo(bill.getId());
        assertThat(payment.getAmountDue()).isEqualByComparingTo("1780.00");
        assertThat(payment.getAmountPaid()).isEqualByComparingTo("2000.00");
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(payment.getReference()).isEqualTo("txn-42");
        assertThat(payment.getRecordedAt()).isBetween(before, after);
        assertThat(payment.getRecordedBy()).isEqualTo(manager.getId());
        assertThat(payment.getChangeDue()).isEqualByComparingTo("220.00");
    }

    @Test
    void newPayment_storesRecordedByAsUserId_notFullUser() {
        Bill bill = billTotalling1780();
        User manager = userWith(Role.MANAGER);

        Payment payment = new Payment(bill, new BigDecimal("1780.00"), PaymentMethod.CARD, null, manager);

        assertThat(payment.getRecordedBy()).isInstanceOf(UserId.class).isEqualTo(manager.getId());
    }

    @Test
    void newPayments_haveDistinctIds() {
        Bill bill = billTotalling1780();
        User manager = userWith(Role.MANAGER);

        Payment a = new Payment(bill, new BigDecimal("1780.00"), PaymentMethod.CASH, null, manager);
        Payment b = new Payment(bill, new BigDecimal("1780.00"), PaymentMethod.CASH, null, manager);

        assertThat(a.getId()).isNotEqualTo(b.getId());
    }

    // -------- amount logic --------

    @Test
    void amountPaid_equalToTotal_succeedsWithZeroChange() {
        Bill bill = billTotalling1780();

        Payment payment = new Payment(bill, new BigDecimal("1780.00"), PaymentMethod.CASH, null, userWith(Role.MANAGER));

        assertThat(payment.getChangeDue()).isEqualByComparingTo("0.00");
    }

    @Test
    void amountPaid_greaterThanTotal_computesChange() {
        Bill bill = billTotalling1780();

        Payment payment = new Payment(bill, new BigDecimal("2500.00"), PaymentMethod.CASH, null, userWith(Role.MANAGER));

        assertThat(payment.getChangeDue()).isEqualByComparingTo("720.00");
    }

    @Test
    void amountPaid_lessThanTotal_throws() {
        Bill bill = billTotalling1780();

        assertThatThrownBy(() -> new Payment(
                        bill, new BigDecimal("1779.99"), PaymentMethod.CASH, null, userWith(Role.MANAGER)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void amountPaid_oneCentBelowTotal_throws() {
        Bill bill = billTotalling1780();

        assertThatThrownBy(() -> new Payment(
                        bill, new BigDecimal("1779.99"), PaymentMethod.CARD, null, userWith(Role.MANAGER)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void amountPaid_comparisonUsesCompareToNotEquals() {
        // BigDecimal("1780") and BigDecimal("1780.00") differ by scale but are equal numerically.
        // The bill total is scale-2; paying scale-0 should still satisfy the >= check.
        Bill bill = billTotalling1780();

        Payment payment = new Payment(bill, new BigDecimal("1780"), PaymentMethod.CASH, null, userWith(Role.MANAGER));

        assertThat(payment.getChangeDue()).isEqualByComparingTo("0.00");
    }

    // -------- BR4: only MANAGER --------

    @Test
    void recordedBy_isManager_succeeds() {
        Bill bill = billTotalling1780();

        Payment payment = new Payment(bill, new BigDecimal("1780.00"), PaymentMethod.CASH, null, userWith(Role.MANAGER));

        assertThat(payment.getRecordedBy()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "WAITER", "KITCHEN_STAFF"})
    void recordedBy_nonManagerRole_throwsUnauthorizedException_BR4(Role role) {
        Bill bill = billTotalling1780();

        assertThatThrownBy(() -> new Payment(
                        bill, new BigDecimal("1780.00"), PaymentMethod.CASH, null, userWith(role)))
                .isInstanceOf(UnauthorizedException.class);
    }

    // -------- null guards --------

    @Test
    void constructor_rejectsNullBill() {
        assertThatThrownBy(() ->
                        new Payment(null, new BigDecimal("1780.00"), PaymentMethod.CASH, null, userWith(Role.MANAGER)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsNullAmountPaid() {
        Bill bill = billTotalling1780();

        assertThatThrownBy(() -> new Payment(bill, null, PaymentMethod.CASH, null, userWith(Role.MANAGER)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsNullMethod() {
        Bill bill = billTotalling1780();

        assertThatThrownBy(() -> new Payment(bill, new BigDecimal("1780.00"), null, null, userWith(Role.MANAGER)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsNullRecordedBy() {
        Bill bill = billTotalling1780();

        assertThatThrownBy(() -> new Payment(bill, new BigDecimal("1780.00"), PaymentMethod.CASH, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_acceptsNullReference() {
        Bill bill = billTotalling1780();

        Payment payment = new Payment(bill, new BigDecimal("1780.00"), PaymentMethod.CASH, null, userWith(Role.MANAGER));

        assertThat(payment.getReference()).isNull();
    }

    // -------- snapshot --------

    @Test
    void amountDue_isSnapshottedFromBillTotalAtConstructionTime() {
        Bill bill = billTotalling1780();

        Payment payment = new Payment(bill, new BigDecimal("2000.00"), PaymentMethod.CASH, null, userWith(Role.MANAGER));

        // The Domain Bill is structurally immutable except for markPaid (which doesn't touch total),
        // but the snapshot guarantee must hold regardless: the value captured at construction is what
        // future reads of payment.getAmountDue() must return.
        bill.markPaid();
        assertThat(payment.getAmountDue()).isEqualByComparingTo(bill.getTotal());
    }

    @Test
    void allPaymentsFromSameBill_shareSameAmountDue() {
        Bill bill = billTotalling1780();
        User manager = userWith(Role.MANAGER);

        Payment first = new Payment(bill, new BigDecimal("1780.00"), PaymentMethod.CASH, null, manager);
        Payment second = new Payment(bill, new BigDecimal("2500.00"), PaymentMethod.CARD, null, manager);

        assertThat(first.getAmountDue()).isEqualByComparingTo(second.getAmountDue());
    }

    // -------- payment-method coverage --------

    @ParameterizedTest
    @EnumSource(PaymentMethod.class)
    void allPaymentMethods_areAcceptedDomainSide(PaymentMethod method) {
        // Configuration.acceptedPaymentMethods is enforced by the Application service, not Payment itself.
        Bill bill = billTotalling1780();

        Payment payment = new Payment(bill, new BigDecimal("1780.00"), method, "ref", userWith(Role.MANAGER));

        assertThat(payment.getMethod()).isEqualTo(method);
    }
}
