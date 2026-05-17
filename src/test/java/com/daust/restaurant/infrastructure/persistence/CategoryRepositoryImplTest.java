package com.daust.restaurant.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.daust.restaurant.domain.Category;
import com.daust.restaurant.domain.CategoryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(CategoryRepositoryImpl.class)
class CategoryRepositoryImplTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void saveAndFindById_roundTripsThroughMapper() {
        Category original = new Category("Appetizers", 1);

        categoryRepository.save(original);

        Optional<Category> loaded = categoryRepository.findById(original.getId());
        assertThat(loaded).isPresent();
        Category found = loaded.get();
        assertThat(found.getId()).isEqualTo(original.getId());
        assertThat(found.getName()).isEqualTo("Appetizers");
        assertThat(found.getDisplayOrder()).isEqualTo(1);
        assertThat(found.isActive()).isTrue();
    }

    @Test
    void save_persistsRenameAndReorder() {
        Category category = new Category("Drinks", 3);
        category.updateName("Beverages");
        category.updateDisplayOrder(7);

        categoryRepository.save(category);

        Category reloaded = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Beverages");
        assertThat(reloaded.getDisplayOrder()).isEqualTo(7);
    }

    @Test
    void findAll_returnsAllSavedCategories() {
        categoryRepository.save(new Category("Appetizers", 1));
        categoryRepository.save(new Category("Mains", 2));
        categoryRepository.save(new Category("Desserts", 3));

        List<Category> all = categoryRepository.findAll();

        assertThat(all).hasSize(3);
    }

    @Test
    void findAllActiveOrderedByDisplayOrder_excludesInactiveAndSorts() {
        Category mains = new Category("Mains", 2);
        Category drinks = new Category("Drinks", 4);
        Category appetizers = new Category("Appetizers", 1);
        Category retired = new Category("Specials", 3);
        retired.deactivate();

        categoryRepository.save(mains);
        categoryRepository.save(drinks);
        categoryRepository.save(appetizers);
        categoryRepository.save(retired);

        List<Category> ordered = categoryRepository.findAllActiveOrderedByDisplayOrder();

        assertThat(ordered)
                .extracting(Category::getId)
                .containsExactly(appetizers.getId(), mains.getId(), drinks.getId());
    }
}
