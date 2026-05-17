package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.Bill;
import com.daust.restaurant.domain.BillId;
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.OrderId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BillRepositoryImpl implements BillRepository {

    private final BillJpaRepository jpaRepository;

    BillRepositoryImpl(BillJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Bill bill) {
        jpaRepository.save(BillMapper.toEntity(bill));
    }

    @Override
    public Optional<Bill> findById(BillId id) {
        return jpaRepository.findById(id.value()).map(BillMapper::toDomain);
    }

    @Override
    public List<Bill> findByOrderId(OrderId orderId) {
        return jpaRepository.findAllByOrderId(orderId.value()).stream()
                .map(BillMapper::toDomain)
                .toList();
    }
}
