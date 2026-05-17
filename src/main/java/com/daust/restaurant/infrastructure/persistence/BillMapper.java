package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.Bill;
import com.daust.restaurant.domain.BillId;
import com.daust.restaurant.domain.BillLineSnapshot;
import com.daust.restaurant.domain.OrderId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class BillMapper {

    private BillMapper() {
    }

    static Bill toDomain(BillJpaEntity entity) {
        List<BillLineSnapshot> lineItems = entity.getLineItems().stream()
                .map(child -> new BillLineSnapshot(
                        child.getMenuItemName(), child.getQuantity(), child.getRecordedUnitPrice()))
                .collect(Collectors.toList());
        Set<OrderId> orderIds = entity.getOrderIds().stream().map(OrderId::of).collect(Collectors.toSet());
        return Bill.reconstitute(
                BillId.of(entity.getId()),
                entity.getBillNumber(),
                orderIds,
                entity.getGeneratedAt(),
                lineItems,
                entity.getItemsSubtotal(),
                entity.getTaxAmount(),
                entity.getServiceChargeAmount(),
                entity.getCoverChargeAmount(),
                entity.getTotal(),
                entity.isPaid());
    }

    static BillJpaEntity toEntity(Bill bill) {
        List<BillLineSnapshotJpaEntity> children = new ArrayList<>(bill.getLineItems().size());
        for (BillLineSnapshot snap : bill.getLineItems()) {
            children.add(new BillLineSnapshotJpaEntity(
                    UUID.randomUUID(),
                    snap.menuItemName(),
                    snap.quantity(),
                    snap.recordedUnitPrice()));
        }
        Set<UUID> orderIdUuids = new HashSet<>(bill.getOrderIds().size());
        for (OrderId oid : bill.getOrderIds()) {
            orderIdUuids.add(oid.value());
        }
        return new BillJpaEntity(
                bill.getId().value(),
                bill.getBillNumber(),
                bill.getGeneratedAt(),
                bill.getItemsSubtotal(),
                bill.getTaxAmount(),
                bill.getServiceChargeAmount(),
                bill.getCoverChargeAmount(),
                bill.getTotal(),
                bill.isPaid(),
                orderIdUuids,
                children);
    }
}
