package com.daust.restaurant.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Order {

    private final OrderId id;
    private final TableId tableId;
    private OrderState state;
    private final LocalDateTime placedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime servedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private CancellationReason cancellationReason;
    private String cancellationNote;
    private UserId cancelledBy;
    private boolean visibleToKitchen;
    private final List<OrderItem> items;

    public Order(TableId tableId) {
        this(
                OrderId.generate(),
                Objects.requireNonNull(tableId, "tableId must not be null"),
                OrderState.PLACED,
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                new ArrayList<>());
    }

    public static Order reconstitute(
            OrderId id,
            TableId tableId,
            OrderState state,
            LocalDateTime placedAt,
            LocalDateTime submittedAt,
            LocalDateTime servedAt,
            LocalDateTime completedAt,
            LocalDateTime cancelledAt,
            CancellationReason cancellationReason,
            String cancellationNote,
            UserId cancelledBy,
            boolean visibleToKitchen,
            List<OrderItem> items) {
        return new Order(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(tableId, "tableId"),
                Objects.requireNonNull(state, "state"),
                Objects.requireNonNull(placedAt, "placedAt"),
                submittedAt,
                servedAt,
                completedAt,
                cancelledAt,
                cancellationReason,
                cancellationNote,
                cancelledBy,
                visibleToKitchen,
                Objects.requireNonNull(items, "items"));
    }

    private Order(
            OrderId id,
            TableId tableId,
            OrderState state,
            LocalDateTime placedAt,
            LocalDateTime submittedAt,
            LocalDateTime servedAt,
            LocalDateTime completedAt,
            LocalDateTime cancelledAt,
            CancellationReason cancellationReason,
            String cancellationNote,
            UserId cancelledBy,
            boolean visibleToKitchen,
            List<OrderItem> items) {
        this.id = id;
        this.tableId = tableId;
        this.state = state;
        this.placedAt = placedAt;
        this.submittedAt = submittedAt;
        this.servedAt = servedAt;
        this.completedAt = completedAt;
        this.cancelledAt = cancelledAt;
        this.cancellationReason = cancellationReason;
        this.cancellationNote = cancellationNote;
        this.cancelledBy = cancelledBy;
        this.visibleToKitchen = visibleToKitchen;
        this.items = new ArrayList<>(items);
    }

    public void addItem(MenuItem menuItem, int quantity) {
        Objects.requireNonNull(menuItem, "menuItem must not be null");
        requireState(OrderState.PLACED, "addItem");
        if (!menuItem.isActive()) {
            throw new IllegalStateException(
                    "cannot add inactive menu item to order (BR2/FR9): " + menuItem.getName());
        }
        OrderItem item = new OrderItem(menuItem.getId(), quantity, menuItem.getUnitPrice());
        item.attachTo(this.id);
        this.items.add(item);
    }

    public void removeItem(OrderItem orderItem) {
        Objects.requireNonNull(orderItem, "orderItem must not be null");
        requireState(OrderState.PLACED, "removeItem");
        if (!this.items.remove(orderItem)) {
            throw new IllegalArgumentException("orderItem is not part of this Order");
        }
    }

    public void submit() {
        requireState(OrderState.PLACED, "submit");
        this.visibleToKitchen = true;
        this.submittedAt = LocalDateTime.now();
    }

    public void startPreparation() {
        requireState(OrderState.PLACED, "startPreparation");
        this.state = OrderState.IN_PREPARATION;
    }

    public void markReady() {
        requireState(OrderState.IN_PREPARATION, "markReady");
        this.state = OrderState.READY;
    }

    public void markServed() {
        requireState(OrderState.READY, "markServed");
        this.state = OrderState.SERVED;
        this.servedAt = LocalDateTime.now();
    }

    public void cancel(CancellationReason reason, String note, User byUser) {
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(byUser, "byUser must not be null");
        if (this.state == OrderState.COMPLETED || this.state == OrderState.CANCELLED) {
            throw new IllegalStateException("cannot cancel order in terminal state " + this.state);
        }
        if (this.state != OrderState.PLACED && byUser.getRole() != Role.MANAGER) {
            throw new UnauthorizedException(
                    "only MANAGER can cancel an order in state " + this.state + " (BR5); was "
                            + byUser.getRole());
        }
        this.state = OrderState.CANCELLED;
        this.cancellationReason = reason;
        this.cancellationNote = note;
        this.cancelledBy = byUser.getId();
        this.cancelledAt = LocalDateTime.now();
    }

    public void markCompleted() {
        requireState(OrderState.SERVED, "markCompleted");
        this.state = OrderState.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public OrderId getId() {
        return id;
    }

    public TableId getTableId() {
        return tableId;
    }

    public OrderState getState() {
        return state;
    }

    public LocalDateTime getPlacedAt() {
        return placedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getServedAt() {
        return servedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public CancellationReason getCancellationReason() {
        return cancellationReason;
    }

    public String getCancellationNote() {
        return cancellationNote;
    }

    public UserId getCancelledBy() {
        return cancelledBy;
    }

    public boolean isVisibleToKitchen() {
        return visibleToKitchen;
    }

    private void requireState(OrderState required, String operation) {
        if (this.state != required) {
            throw new IllegalStateException(
                    operation + " requires state " + required + " but was " + this.state);
        }
    }
}
