package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.OrderState;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findByTableIdAndStateIn(UUID tableId, Collection<OrderState> states);

    List<OrderJpaEntity> findByState(OrderState state);
}
