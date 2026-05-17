package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.BillId;
import com.daust.restaurant.domain.Payment;
import com.daust.restaurant.domain.PaymentId;
import com.daust.restaurant.domain.UserId;

final class PaymentMapper {

    private PaymentMapper() {
    }

    static Payment toDomain(PaymentJpaEntity entity) {
        return Payment.reconstitute(
                PaymentId.of(entity.getId()),
                BillId.of(entity.getBillId()),
                entity.getAmountDue(),
                entity.getAmountPaid(),
                entity.getMethod(),
                entity.getReference(),
                entity.getRecordedAt(),
                UserId.of(entity.getRecordedBy()),
                entity.getChangeDue());
    }

    static PaymentJpaEntity toEntity(Payment payment) {
        return new PaymentJpaEntity(
                payment.getId().value(),
                payment.getBillId().value(),
                payment.getAmountDue(),
                payment.getAmountPaid(),
                payment.getMethod(),
                payment.getReference(),
                payment.getRecordedAt(),
                payment.getRecordedBy().value(),
                payment.getChangeDue());
    }
}
