package com.daust.restaurant.domain;

import java.util.Objects;
import java.util.UUID;

public record MenuItemId(UUID value) {

    public MenuItemId {
        Objects.requireNonNull(value, "MenuItemId value must not be null");
    }

    public static MenuItemId generate() {
        return new MenuItemId(UUID.randomUUID());
    }

    public static MenuItemId of(UUID value) {
        return new MenuItemId(value);
    }
}
