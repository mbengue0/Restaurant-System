package com.daust.restaurant.domain;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    void save(Category category);

    Optional<Category> findById(CategoryId id);

    List<Category> findAll();

    List<Category> findAllActiveOrderedByDisplayOrder();
}
