package com.daust.restaurant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderItemTest {

    private static final MenuItemId MI = MenuItemId.generate();

    @Test
    void newOrderItem_hasGeneratedIdAndHoldsAttributes() {
        OrderItem item = new OrderItem(MI, 3, new BigDecimal("4.50"));

        assertThat(item.getId()).isNotNull();
        assertThat(item.getId().value()).isNotNull();
        assertThat(item.getMenuItemId()).isEqualTo(MI);
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getRecordedUnitPrice()).isEqualByComparingTo("4.50");
    }

    @Test
    void newOrderItem_orderIdIsNullUntilAttached() {
        OrderItem item = new OrderItem(MI, 1, new BigDecimal("2.00"));

        assertThat(item.getOrderId()).isNull();
    }

    @Test
    void newOrderItems_haveDistinctIds() {
        OrderItem a = new OrderItem(MI, 1, new BigDecimal("2.00"));
        OrderItem b = new OrderItem(MI, 1, new BigDecimal("2.00"));

        assertThat(a.getId()).isNotEqualTo(b.getId());
    }

    @Test
    void constructor_rejectsNullMenuItemId() {
        assertThatThrownBy(() -> new OrderItem(null, 1, new BigDecimal("2.00")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsZeroQuantity() {
        assertThatThrownBy(() -> new OrderItem(MI, 0, new BigDecimal("2.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNegativeQuantity() {
        assertThatThrownBy(() -> new OrderItem(MI, -1, new BigDecimal("2.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNullPrice() {
        assertThatThrownBy(() -> new OrderItem(MI, 1, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsZeroPrice() {
        assertThatThrownBy(() -> new OrderItem(MI, 1, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNegativePrice() {
        assertThatThrownBy(() -> new OrderItem(MI, 1, new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lineTotal_multipliesPriceByQuantity() {
        OrderItem item = new OrderItem(MI, 3, new BigDecimal("4.50"));

        assertThat(item.lineTotal()).isEqualByComparingTo("13.50");
    }

    @Test
    void lineTotal_handlesUnitQuantity() {
        OrderItem item = new OrderItem(MI, 1, new BigDecimal("9.99"));

        assertThat(item.lineTotal()).isEqualByComparingTo("9.99");
    }
}
