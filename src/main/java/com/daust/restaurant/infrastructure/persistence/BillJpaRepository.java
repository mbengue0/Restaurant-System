package com.daust.restaurant.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BillJpaRepository extends JpaRepository<BillJpaEntity, UUID> {

    @Query("select b from BillJpaEntity b where :orderId member of b.orderIds")
    List<BillJpaEntity> findAllByOrderId(@Param("orderId") UUID orderId);
}
