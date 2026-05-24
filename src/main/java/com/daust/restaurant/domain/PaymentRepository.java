package com.daust.restaurant.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    void save(Payment payment);

    Optional<Payment> findById(PaymentId id);

    Optional<Payment> findByBillId(BillId billId);

    List<Payment> findByRecordedAtBetween(LocalDateTime from, LocalDateTime to);
}
