package com.daust.restaurant.domain;

import java.util.Optional;

public interface PaymentRepository {

    void save(Payment payment);

    Optional<Payment> findById(PaymentId id);

    Optional<Payment> findByBillId(BillId billId);
}
