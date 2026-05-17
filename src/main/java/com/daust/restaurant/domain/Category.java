package com.daust.restaurant.domain;

import java.util.Objects;

public class Category {

    private final CategoryId id;
    private String name;
    private int displayOrder;
    private boolean active;

    public Category(String name, int displayOrder) {
        this.id = CategoryId.generate();
        this.name = normalizeName(name);
        this.displayOrder = requireNonNegative(displayOrder);
        this.active = true;
    }

    public static Category reconstitute(CategoryId id, String name, int displayOrder, boolean active) {
        return new Category(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(name, "name"),
                displayOrder,
                active);
    }

    private Category(CategoryId id, String name, int displayOrder, boolean active) {
        this.id = id;
        this.name = name;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public void updateName(String newName) {
        this.name = normalizeName(newName);
    }

    public void updateDisplayOrder(int newOrder) {
        this.displayOrder = requireNonNegative(newOrder);
    }

    public void deactivate() {
        this.active = false;
    }

    public CategoryId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    private static String normalizeName(String value) {
        Objects.requireNonNull(value, "name must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (trimmed.length() > 50) {
            throw new IllegalArgumentException("name must be 50 characters or fewer");
        }
        return trimmed;
    }

    private static int requireNonNegative(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("displayOrder must be non-negative");
        }
        return value;
    }
}
