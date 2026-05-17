package com.daust.restaurant.domain;

import java.util.Objects;

public class Table {

    private final TableId id;
    private int capacity;
    private TableStatus status;
    private boolean active;

    public Table(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.id = TableId.generate();
        this.capacity = capacity;
        this.status = TableStatus.AVAILABLE;
        this.active = true;
    }

    public static Table reconstitute(TableId id, int capacity, TableStatus status, boolean active) {
        return new Table(
                Objects.requireNonNull(id, "id"),
                capacity,
                Objects.requireNonNull(status, "status"),
                active);
    }

    private Table(TableId id, int capacity, TableStatus status, boolean active) {
        this.id = id;
        this.capacity = capacity;
        this.status = status;
        this.active = active;
    }

    public void seatCustomers() {
        if (!active) {
            throw new IllegalStateException("Cannot seat customers at an inactive table");
        }
        if (status == TableStatus.OCCUPIED) {
            throw new IllegalStateException("Table is already occupied");
        }
        this.status = TableStatus.OCCUPIED;
    }

    public void markAvailable() {
        this.status = TableStatus.AVAILABLE;
    }

    public void deactivate() {
        this.active = false;
    }

    public TableId getId() {
        return id;
    }

    public int getCapacity() {
        return capacity;
    }

    public TableStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return active;
    }
}
