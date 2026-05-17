package com.daust.restaurant.domain;

import java.math.BigDecimal;
import java.util.Objects;

public class MenuItem {

    private final MenuItemId id;
    private String name;
    private String description;
    private BigDecimal unitPrice;
    private CategoryId categoryId;
    private boolean active;

    public MenuItem(String name, String description, BigDecimal unitPrice, CategoryId categoryId) {
        this.id = MenuItemId.generate();
        this.name = normalizeName(name);
        this.description = normalizeDescription(description);
        this.unitPrice = requirePositive(unitPrice);
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
        this.active = true;
    }

    public static MenuItem reconstitute(
            MenuItemId id,
            String name,
            String description,
            BigDecimal unitPrice,
            CategoryId categoryId,
            boolean active) {
        return new MenuItem(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(name, "name"),
                description,
                Objects.requireNonNull(unitPrice, "unitPrice"),
                Objects.requireNonNull(categoryId, "categoryId"),
                active);
    }

    private MenuItem(
            MenuItemId id,
            String name,
            String description,
            BigDecimal unitPrice,
            CategoryId categoryId,
            boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.unitPrice = unitPrice;
        this.categoryId = categoryId;
        this.active = active;
    }

    public void changePrice(BigDecimal newPrice) {
        this.unitPrice = requirePositive(newPrice);
    }

    public void deactivate() {
        this.active = false;
    }

    public MenuItemId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public CategoryId getCategoryId() {
        return categoryId;
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
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("name must be 100 characters or fewer");
        }
        return trimmed;
    }

    private static String normalizeDescription(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BigDecimal requirePositive(BigDecimal value) {
        Objects.requireNonNull(value, "unitPrice must not be null");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("unitPrice must be positive");
        }
        return value;
    }
}
