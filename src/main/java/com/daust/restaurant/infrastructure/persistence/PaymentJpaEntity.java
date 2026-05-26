package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_payments_bill_id", columnNames = "bill_id"))
// Set-membership check encoded as a LIKE; see OrderJpaEntity comment.
@Check(
        name = "ck_payments_method",
        constraints = "',CASH,CARD,MOBILE_MONEY,' like '%,' || method || ',%'")
@Check(name = "ck_payments_amount_due_nonneg", constraints = "amount_due >= 0")
@Check(name = "ck_payments_amount_paid_nonneg", constraints = "amount_paid >= 0")
@Check(name = "ck_payments_change_due_nonneg", constraints = "change_due >= 0")
@Check(name = "ck_payments_paid_ge_due", constraints = "amount_paid >= amount_due")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
class PaymentJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "bill_id", nullable = false)
    private UUID billId;

    @Column(name = "amount_due", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20, columnDefinition = "varchar(20)")
    private PaymentMethod method;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "recorded_by", nullable = false)
    private UUID recordedBy;

    @Column(name = "change_due", nullable = false, precision = 12, scale = 2)
    private BigDecimal changeDue;
}
