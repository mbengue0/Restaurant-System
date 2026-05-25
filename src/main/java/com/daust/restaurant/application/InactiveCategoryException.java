package com.daust.restaurant.application;

/**
 * FR9 / BR2 — items in an inactive {@link com.daust.restaurant.domain.Category} cannot be added
 * to an Order. Thrown by {@link PlaceOrderService#addItemToOrder} when the menu item's category
 * is currently inactive, even if the item itself is active.
 *
 * <p>The check sits in the Application layer (not Domain) because {@code Order} does not know
 * about {@code Category} — it's the deferred-guard pattern that {@code CLAUDE.md} establishes
 * for cross-aggregate rules.
 */
public class InactiveCategoryException extends RuntimeException {
    public InactiveCategoryException(String message) {
        super(message);
    }
}
