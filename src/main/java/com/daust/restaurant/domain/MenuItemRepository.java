package com.daust.restaurant.domain;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository {

    void save(MenuItem menuItem);

    Optional<MenuItem> findById(MenuItemId id);

    List<MenuItem> findAll();

    List<MenuItem> findActiveByCategoryId(CategoryId categoryId);
}
