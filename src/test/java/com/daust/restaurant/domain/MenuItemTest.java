package com.daust.restaurant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MenuItemTest {

    private static final CategoryId CATEGORY = CategoryId.generate();

    @Test
    void newMenuItem_isActiveWithGeneratedId() {
        MenuItem item = new MenuItem("Grilled Salmon", "Atlantic salmon with lemon", new BigDecimal("12.50"), CATEGORY);

        assertThat(item.getId()).isNotNull();
        assertThat(item.getId().value()).isNotNull();
        assertThat(item.getName()).isEqualTo("Grilled Salmon");
        assertThat(item.getDescription()).isEqualTo("Atlantic salmon with lemon");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("12.50");
        assertThat(item.getCategoryId()).isEqualTo(CATEGORY);
        assertThat(item.isActive()).isTrue();
    }

    @Test
    void newMenuItem_trimsNameAndDescription() {
        MenuItem item = new MenuItem("  Pizza  ", "  Wood-fired  ", new BigDecimal("9.00"), CATEGORY);

        assertThat(item.getName()).isEqualTo("Pizza");
        assertThat(item.getDescription()).isEqualTo("Wood-fired");
    }

    @Test
    void newMenuItem_acceptsNullDescription() {
        MenuItem item = new MenuItem("Bread", null, new BigDecimal("2.00"), CATEGORY);

        assertThat(item.getDescription()).isNull();
    }

    @Test
    void newMenuItem_normalizesBlankDescriptionToNull() {
        MenuItem item = new MenuItem("Bread", "   ", new BigDecimal("2.00"), CATEGORY);

        assertThat(item.getDescription()).isNull();
    }

    @Test
    void changePrice_updatesPrice() {
        MenuItem item = new MenuItem("Coffee", null, new BigDecimal("3.00"), CATEGORY);

        item.changePrice(new BigDecimal("3.50"));

        assertThat(item.getUnitPrice()).isEqualByComparingTo("3.50");
    }

    @Test
    void changePrice_rejectsZero() {
        MenuItem item = new MenuItem("Coffee", null, new BigDecimal("3.00"), CATEGORY);

        assertThatThrownBy(() -> item.changePrice(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changePrice_rejectsNegative() {
        MenuItem item = new MenuItem("Coffee", null, new BigDecimal("3.00"), CATEGORY);

        assertThatThrownBy(() -> item.changePrice(new BigDecimal("-1.00"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changePrice_rejectsNull() {
        MenuItem item = new MenuItem("Coffee", null, new BigDecimal("3.00"), CATEGORY);

        assertThatThrownBy(() -> item.changePrice(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void deactivate_setsActiveFalse() {
        MenuItem item = new MenuItem("Tea", null, new BigDecimal("2.00"), CATEGORY);

        item.deactivate();

        assertThat(item.isActive()).isFalse();
    }

    @Test
    void constructor_rejectsBlankName() {
        assertThatThrownBy(() -> new MenuItem("", null, new BigDecimal("1.00"), CATEGORY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MenuItem("   ", null, new BigDecimal("1.00"), CATEGORY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNullName() {
        assertThatThrownBy(() -> new MenuItem(null, null, new BigDecimal("1.00"), CATEGORY))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsNameLongerThan100Chars() {
        String tooLong = "x".repeat(101);

        assertThatThrownBy(() -> new MenuItem(tooLong, null, new BigDecimal("1.00"), CATEGORY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNullCategoryId() {
        assertThatThrownBy(() -> new MenuItem("Pizza", null, new BigDecimal("9.00"), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsNullPrice() {
        assertThatThrownBy(() -> new MenuItem("Pizza", null, null, CATEGORY))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsZeroPrice() {
        assertThatThrownBy(() -> new MenuItem("Pizza", null, BigDecimal.ZERO, CATEGORY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNegativePrice() {
        assertThatThrownBy(() -> new MenuItem("Pizza", null, new BigDecimal("-1.00"), CATEGORY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void newMenuItems_haveDistinctIds() {
        MenuItem a = new MenuItem("Pizza", null, new BigDecimal("9.00"), CATEGORY);
        MenuItem b = new MenuItem("Pasta", null, new BigDecimal("8.00"), CATEGORY);

        assertThat(a.getId()).isNotEqualTo(b.getId());
    }
}
