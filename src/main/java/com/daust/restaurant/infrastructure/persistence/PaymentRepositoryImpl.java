package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.BillId;
import com.daust.restaurant.domain.Payment;
import com.daust.restaurant.domain.PaymentId;
import com.daust.restaurant.domain.PaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    PaymentRepositoryImpl(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Payment payment) {
        jpaRepository.save(PaymentMapper.toEntity(payment));
    }

    @Override
    public Optional<Payment> findById(PaymentId id) {
        return jpaRepository.findById(id.value()).map(PaymentMapper::toDomain);
    }

    @Override
    public Optional<Payment> findByBillId(BillId billId) {
        return jpaRepository.findByBillId(billId.value()).map(PaymentMapper::toDomain);
    }

    @Override
    public List<Payment> findByRecordedAtBetween(LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByRecordedAtBetween(from, to).stream()
                .map(PaymentMapper::toDomain)
                .toList();
    }
}
