package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.Category;
import com.daust.restaurant.domain.CategoryId;

final class CategoryMapper {

    private CategoryMapper() {
    }

    static Category toDomain(CategoryJpaEntity entity) {
        return Category.reconstitute(
                CategoryId.of(entity.getId()),
                entity.getName(),
                entity.getDisplayOrder(),
                entity.isActive());
    }

    static CategoryJpaEntity toEntity(Category category) {
        return new CategoryJpaEntity(
                category.getId().value(),
                category.getName(),
                category.getDisplayOrder(),
                category.isActive());
    }
}
