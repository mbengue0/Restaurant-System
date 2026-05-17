package com.daust.restaurant.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

public class Payment {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private final PaymentId id;
    private final BillId billId;
    private final BigDecimal amountDue;
    private final BigDecimal amountPaid;
    private final PaymentMethod method;
    private final String reference;
    private final LocalDateTime recordedAt;
    private final UserId recordedBy;
    private final BigDecimal changeDue;

    public Payment(Bill bill, BigDecimal amountPaid, PaymentMethod method, String reference, User recordedBy) {
        Objects.requireNonNull(bill, "bill must not be null");
        Objects.requireNonNull(amountPaid, "amountPaid must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(recordedBy, "recordedBy must not be null");
        if (recordedBy.getRole() != Role.MANAGER) {
            throw new UnauthorizedException(
                    "Only MANAGER can record payment (BR4); was " + recordedBy.getRole());
        }
        BigDecimal normalizedAmountPaid = amountPaid.setScale(MONEY_SCALE, MONEY_ROUNDING);
        BigDecimal billTotal = bill.getTotal();
        if (normalizedAmountPaid.compareTo(billTotal) < 0) {
            throw new IllegalArgumentException(
                    "amountPaid (" + normalizedAmountPaid + ") must be >= bill total (" + billTotal + ")");
        }

        this.id = PaymentId.generate();
        this.billId = bill.getId();
        this.amountDue = billTotal;
        this.amountPaid = normalizedAmountPaid;
        this.method = method;
        this.reference = reference;
        this.recordedAt = LocalDateTime.now();
        this.recordedBy = recordedBy.getId();
        this.changeDue = normalizedAmountPaid.subtract(billTotal);
    }

    public static Payment reconstitute(
            PaymentId id,
            BillId billId,
            BigDecimal amountDue,
            BigDecimal amountPaid,
            PaymentMethod method,
            String reference,
            LocalDateTime recordedAt,
            UserId recordedBy,
            BigDecimal changeDue) {
        return new Payment(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(billId, "billId"),
                Objects.requireNonNull(amountDue, "amountDue"),
                Objects.requireNonNull(amountPaid, "amountPaid"),
                Objects.requireNonNull(method, "method"),
                reference,
                Objects.requireNonNull(recordedAt, "recordedAt"),
                Objects.requireNonNull(recordedBy, "recordedBy"),
                Objects.requireNonNull(changeDue, "changeDue"));
    }

    private Payment(
            PaymentId id,
            BillId billId,
            BigDecimal amountDue,
            BigDecimal amountPaid,
            PaymentMethod method,
            String reference,
            LocalDateTime recordedAt,
            UserId recordedBy,
            BigDecimal changeDue) {
        this.id = id;
        this.billId = billId;
        this.amountDue = amountDue;
        this.amountPaid = amountPaid;
        this.method = method;
        this.reference = reference;
        this.recordedAt = recordedAt;
        this.recordedBy = recordedBy;
        this.changeDue = changeDue;
    }

    public PaymentId getId() {
        return id;
    }

    public BillId getBillId() {
        return billId;
    }

    public BigDecimal getAmountDue() {
        return amountDue;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public String getReference() {
        return reference;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public UserId getRecordedBy() {
        return recordedBy;
    }

    public BigDecimal getChangeDue() {
        return changeDue;
    }
}
