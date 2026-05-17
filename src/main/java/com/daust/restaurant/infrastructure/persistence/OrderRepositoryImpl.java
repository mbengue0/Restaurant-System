package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderId;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.OrderState;
import com.daust.restaurant.domain.TableId;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private static final Set<OrderState> ACTIVE_STATES =
            EnumSet.of(OrderState.PLACED, OrderState.IN_PREPARATION, OrderState.READY, OrderState.SERVED);

    private final OrderJpaRepository jpaRepository;

    OrderRepositoryImpl(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Order order) {
        jpaRepository.save(OrderMapper.toEntity(order));
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value()).map(OrderMapper::toDomain);
    }

    @Override
    public List<Order> findActiveByTableId(TableId tableId) {
        return jpaRepository.findByTableIdAndStateIn(tableId.value(), ACTIVE_STATES).stream()
                .map(OrderMapper::toDomain)
                .toList();
    }

    @Override
    public List<Order> findByState(OrderState state) {
        return jpaRepository.findByState(state).stream().map(OrderMapper::toDomain).toList();
    }
}
