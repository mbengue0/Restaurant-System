package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.CategoryId;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.MenuItemId;
import com.daust.restaurant.domain.MenuItemRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MenuItemRepositoryImpl implements MenuItemRepository {

    private final MenuItemJpaRepository jpaRepository;

    MenuItemRepositoryImpl(MenuItemJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(MenuItem menuItem) {
        jpaRepository.save(MenuItemMapper.toEntity(menuItem));
    }

    @Override
    public Optional<MenuItem> findById(MenuItemId id) {
        return jpaRepository.findById(id.value()).map(MenuItemMapper::toDomain);
    }

    @Override
    public List<MenuItem> findAll() {
        return jpaRepository.findAll().stream().map(MenuItemMapper::toDomain).toList();
    }

    @Override
    public List<MenuItem> findActiveByCategoryId(CategoryId categoryId) {
        return jpaRepository.findByActiveTrueAndCategoryId(categoryId.value()).stream()
                .map(MenuItemMapper::toDomain)
                .toList();
    }
}
