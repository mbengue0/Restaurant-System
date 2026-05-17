package com.daust.restaurant.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.daust.restaurant.domain.CategoryId;
import com.daust.restaurant.domain.MenuItem;
import com.daust.restaurant.domain.MenuItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(MenuItemRepositoryImpl.class)
class MenuItemRepositoryImplTest {

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Test
    void saveAndFindById_roundTripsThroughMapper() {
        CategoryId category = CategoryId.generate();
        MenuItem original = new MenuItem("Grilled Salmon", "Atlantic salmon", new BigDecimal("12.50"), category);

        menuItemRepository.save(original);

        Optional<MenuItem> loaded = menuItemRepository.findById(original.getId());
        assertThat(loaded).isPresent();
        MenuItem found = loaded.get();
        assertThat(found.getId()).isEqualTo(original.getId());
        assertThat(found.getName()).isEqualTo("Grilled Salmon");
        assertThat(found.getDescription()).isEqualTo("Atlantic salmon");
        assertThat(found.getUnitPrice()).isEqualByComparingTo("12.50");
        assertThat(found.getCategoryId()).isEqualTo(category);
        assertThat(found.isActive()).isTrue();
    }

    @Test
    void save_persistsPriceChange() {
        CategoryId category = CategoryId.generate();
        MenuItem item = new MenuItem("Coffee", null, new BigDecimal("3.00"), category);
        item.changePrice(new BigDecimal("3.50"));

        menuItemRepository.save(item);

        MenuItem reloaded = menuItemRepository.findById(item.getId()).orElseThrow();
        assertThat(reloaded.getUnitPrice()).isEqualByComparingTo("3.50");
    }

    @Test
    void save_persistsNullDescription() {
        CategoryId category = CategoryId.generate();
        MenuItem item = new MenuItem("Bread", null, new BigDecimal("2.00"), category);

        menuItemRepository.save(item);

        MenuItem reloaded = menuItemRepository.findById(item.getId()).orElseThrow();
        assertThat(reloaded.getDescription()).isNull();
    }

    @Test
    void findAll_returnsAllSavedItems() {
        CategoryId category = CategoryId.generate();
        menuItemRepository.save(new MenuItem("Salmon", null, new BigDecimal("12.50"), category));
        menuItemRepository.save(new MenuItem("Coffee", null, new BigDecimal("3.00"), category));
        menuItemRepository.save(new MenuItem("Tea", null, new BigDecimal("2.50"), category));

        List<MenuItem> all = menuItemRepository.findAll();

        assertThat(all).hasSize(3);
    }

    @Test
    void findActiveByCategoryId_excludesInactiveAndOtherCategories() {
        CategoryId drinks = CategoryId.generate();
        CategoryId mains = CategoryId.generate();
        MenuItem coffee = new MenuItem("Coffee", null, new BigDecimal("3.00"), drinks);
        MenuItem tea = new MenuItem("Tea", null, new BigDecimal("2.50"), drinks);
        MenuItem retiredJuice = new MenuItem("Juice", null, new BigDecimal("4.00"), drinks);
        retiredJuice.deactivate();
        MenuItem pizza = new MenuItem("Pizza", null, new BigDecimal("9.00"), mains);

        menuItemRepository.save(coffee);
        menuItemRepository.save(tea);
        menuItemRepository.save(retiredJuice);
        menuItemRepository.save(pizza);

        List<MenuItem> drinksMenu = menuItemRepository.findActiveByCategoryId(drinks);

        assertThat(drinksMenu)
                .extracting(MenuItem::getId)
                .containsExactlyInAnyOrder(coffee.getId(), tea.getId());
    }
}
