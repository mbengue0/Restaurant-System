package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.Category;
import com.daust.restaurant.domain.CategoryId;
import com.daust.restaurant.domain.CategoryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository jpaRepository;

    CategoryRepositoryImpl(CategoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Category category) {
        jpaRepository.save(CategoryMapper.toEntity(category));
    }

    @Override
    public Optional<Category> findById(CategoryId id) {
        return jpaRepository.findById(id.value()).map(CategoryMapper::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return jpaRepository.findAll().stream().map(CategoryMapper::toDomain).toList();
    }

    @Override
    public List<Category> findAllActiveOrderedByDisplayOrder() {
        return jpaRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(CategoryMapper::toDomain)
                .toList();
    }
}
