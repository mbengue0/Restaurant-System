package com.daust.restaurant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BillLineSnapshotTest {

    @Test
    void newSnapshot_storesFields() {
        BillLineSnapshot snap = new BillLineSnapshot("Pizza", 2, new BigDecimal("9.00"));

        assertThat(snap.menuItemName()).isEqualTo("Pizza");
        assertThat(snap.quantity()).isEqualTo(2);
        assertThat(snap.recordedUnitPrice()).isEqualByComparingTo("9.00");
    }

    @Test
    void equalsAndHashCode_areValueBased() {
        BillLineSnapshot a = new BillLineSnapshot("Pizza", 2, new BigDecimal("9.00"));
        BillLineSnapshot b = new BillLineSnapshot("Pizza", 2, new BigDecimal("9.00"));
        BillLineSnapshot different = new BillLineSnapshot("Pizza", 3, new BigDecimal("9.00"));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(different);
    }

    @Test
    void constructor_rejectsNullName() {
        assertThatThrownBy(() -> new BillLineSnapshot(null, 1, new BigDecimal("1.00")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsBlankName() {
        assertThatThrownBy(() -> new BillLineSnapshot("", 1, new BigDecimal("1.00")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BillLineSnapshot("   ", 1, new BigDecimal("1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsZeroQuantity() {
        assertThatThrownBy(() -> new BillLineSnapshot("Pizza", 0, new BigDecimal("1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNegativeQuantity() {
        assertThatThrownBy(() -> new BillLineSnapshot("Pizza", -1, new BigDecimal("1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNullPrice() {
        assertThatThrownBy(() -> new BillLineSnapshot("Pizza", 1, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsZeroPrice() {
        assertThatThrownBy(() -> new BillLineSnapshot("Pizza", 1, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNegativePrice() {
        assertThatThrownBy(() -> new BillLineSnapshot("Pizza", 1, new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
