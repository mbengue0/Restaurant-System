package com.daust.restaurant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CategoryTest {

    @Test
    void newCategory_isActiveWithGeneratedId() {
        Category category = new Category("Appetizers", 1);

        assertThat(category.getId()).isNotNull();
        assertThat(category.getId().value()).isNotNull();
        assertThat(category.getName()).isEqualTo("Appetizers");
        assertThat(category.getDisplayOrder()).isEqualTo(1);
        assertThat(category.isActive()).isTrue();
    }

    @Test
    void updateName_replacesName() {
        Category category = new Category("Drinks", 2);

        category.updateName("Beverages");

        assertThat(category.getName()).isEqualTo("Beverages");
    }

    @Test
    void updateName_trimsWhitespace() {
        Category category = new Category("Drinks", 2);

        category.updateName("  Beverages  ");

        assertThat(category.getName()).isEqualTo("Beverages");
    }

    @Test
    void updateDisplayOrder_replacesOrder() {
        Category category = new Category("Drinks", 2);

        category.updateDisplayOrder(5);

        assertThat(category.getDisplayOrder()).isEqualTo(5);
    }

    @Test
    void deactivate_setsActiveFalse() {
        Category category = new Category("Drinks", 2);

        category.deactivate();

        assertThat(category.isActive()).isFalse();
    }

    @Test
    void constructor_rejectsBlankName() {
        assertThatThrownBy(() -> new Category("", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Category("   ", 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNullName() {
        assertThatThrownBy(() -> new Category(null, 0)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsNameLongerThan50Chars() {
        String tooLong = "x".repeat(51);

        assertThatThrownBy(() -> new Category(tooLong, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNegativeDisplayOrder() {
        assertThatThrownBy(() -> new Category("Drinks", -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateDisplayOrder_rejectsNegative() {
        Category category = new Category("Drinks", 2);

        assertThatThrownBy(() -> category.updateDisplayOrder(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void newCategories_haveDistinctIds() {
        Category a = new Category("Appetizers", 1);
        Category b = new Category("Mains", 2);

        assertThat(a.getId()).isNotEqualTo(b.getId());
    }
}
