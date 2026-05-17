package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.CategoryId;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.MenuItemId;

final class MenuItemMapper {

    private MenuItemMapper() {
    }

    static MenuItem toDomain(MenuItemJpaEntity entity) {
        return MenuItem.reconstitute(
                MenuItemId.of(entity.getId()),
                entity.getName(),
                entity.getDescription(),
                entity.getUnitPrice(),
                CategoryId.of(entity.getCategoryId()),
                entity.isActive());
    }

    static MenuItemJpaEntity toEntity(MenuItem menuItem) {
        return new MenuItemJpaEntity(
                menuItem.getId().value(),
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getUnitPrice(),
                menuItem.getCategoryId().value(),
                menuItem.isActive());
    }
}
