package com.daust.restaurant.domain;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(OrderId id);

    List<Order> findActiveByTableId(TableId tableId);

    List<Order> findByState(OrderState state);
}
