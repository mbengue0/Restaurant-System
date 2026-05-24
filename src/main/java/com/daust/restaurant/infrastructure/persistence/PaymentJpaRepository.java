package com.daust.restaurant.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByBillId(UUID billId);

    List<PaymentJpaEntity> findByRecordedAtBetween(LocalDateTime from, LocalDateTime to);
}
