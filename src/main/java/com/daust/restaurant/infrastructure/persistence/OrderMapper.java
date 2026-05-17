package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.MenuItemId;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderId;
import com.daust.restaurant.domain.OrderItem;
import com.daust.restaurant.domain.OrderItemId;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.UserId;
import java.util.ArrayList;
import java.util.List;

final class OrderMapper {

    private OrderMapper() {
    }

    static Order toDomain(OrderJpaEntity entity) {
        OrderId orderId = OrderId.of(entity.getId());
        List<OrderItem> items = new ArrayList<>(entity.getItems().size());
        for (OrderItemJpaEntity child : entity.getItems()) {
            items.add(OrderItem.reconstitute(
                    OrderItemId.of(child.getId()),
                    orderId,
                    MenuItemId.of(child.getMenuItemId()),
                    child.getQuantity(),
                    child.getRecordedUnitPrice()));
        }
        return Order.reconstitute(
                orderId,
                TableId.of(entity.getTableId()),
                entity.getState(),
                entity.getPlacedAt(),
                entity.getSubmittedAt(),
                entity.getServedAt(),
                entity.getCompletedAt(),
                entity.getCancelledAt(),
                entity.getCancellationReason(),
                entity.getCancellationNote(),
                entity.getCancelledBy() == null ? null : UserId.of(entity.getCancelledBy()),
                entity.isVisibleToKitchen(),
                items);
    }

    static OrderJpaEntity toEntity(Order order) {
        List<OrderItemJpaEntity> items = new ArrayList<>(order.getItems().size());
        for (OrderItem item : order.getItems()) {
            items.add(new OrderItemJpaEntity(
                    item.getId().value(),
                    item.getMenuItemId().value(),
                    item.getQuantity(),
                    item.getRecordedUnitPrice()));
        }
        return new OrderJpaEntity(
                order.getId().value(),
                order.getTableId().value(),
                order.getState(),
                order.getPlacedAt(),
                order.getSubmittedAt(),
                order.getServedAt(),
                order.getCompletedAt(),
                order.getCancelledAt(),
                order.getCancellationReason(),
                order.getCancellationNote(),
                order.getCancelledBy() == null ? null : order.getCancelledBy().value(),
                order.isVisibleToKitchen(),
                items);
    }
}
