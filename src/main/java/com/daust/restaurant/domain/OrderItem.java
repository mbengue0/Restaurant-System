package com.daust.restaurant.domain;

import java.math.BigDecimal;
import java.util.Objects;

public class OrderItem {

    private final OrderItemId id;
    private OrderId orderId;
    private final MenuItemId menuItemId;
    private final int quantity;
    private final BigDecimal recordedUnitPrice;

    public OrderItem(MenuItemId menuItemId, int quantity, BigDecimal recordedUnitPrice) {
        this.id = OrderItemId.generate();
        this.orderId = null;
        this.menuItemId = Objects.requireNonNull(menuItemId, "menuItemId must not be null");
        this.quantity = requirePositiveQuantity(quantity);
        this.recordedUnitPrice = requirePositivePrice(recordedUnitPrice);
    }

    public static OrderItem reconstitute(
            OrderItemId id,
            OrderId orderId,
            MenuItemId menuItemId,
            int quantity,
            BigDecimal recordedUnitPrice) {
        return new OrderItem(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(orderId, "orderId"),
                Objects.requireNonNull(menuItemId, "menuItemId"),
                quantity,
                Objects.requireNonNull(recordedUnitPrice, "recordedUnitPrice"));
    }

    private OrderItem(
            OrderItemId id,
            OrderId orderId,
            MenuItemId menuItemId,
            int quantity,
            BigDecimal recordedUnitPrice) {
        this.id = id;
        this.orderId = orderId;
        this.menuItemId = menuItemId;
        this.quantity = quantity;
        this.recordedUnitPrice = recordedUnitPrice;
    }

    void attachTo(OrderId orderId) {
        if (this.orderId != null) {
            throw new IllegalStateException("OrderItem is already attached to an Order");
        }
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
    }

    public BigDecimal lineTotal() {
        return recordedUnitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public OrderItemId getId() {
        return id;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public MenuItemId getMenuItemId() {
        return menuItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getRecordedUnitPrice() {
        return recordedUnitPrice;
    }

    private static int requirePositiveQuantity(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("quantity must be >= 1");
        }
        return value;
    }

    private static BigDecimal requirePositivePrice(BigDecimal value) {
        Objects.requireNonNull(value, "recordedUnitPrice must not be null");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("recordedUnitPrice must be > 0");
        }
        return value;
    }
}
