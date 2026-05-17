package com.daust.restaurant.domain;

import java.util.List;
import java.util.Optional;

public interface BillRepository {

    void save(Bill bill);

    Optional<Bill> findById(BillId id);

    List<Bill> findByOrderId(OrderId orderId);
}
