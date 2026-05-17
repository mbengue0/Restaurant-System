package com.daust.restaurant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TableTest {

    @Test
    void newTable_isAvailableAndActiveWithGeneratedId() {
        Table table = new Table(4);

        assertThat(table.getId()).isNotNull();
        assertThat(table.getId().value()).isNotNull();
        assertThat(table.getCapacity()).isEqualTo(4);
        assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(table.isActive()).isTrue();
    }

    @Test
    void seatCustomers_transitionsAvailableToOccupied() {
        Table table = new Table(2);

        table.seatCustomers();

        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
    }

    @Test
    void markAvailable_transitionsOccupiedToAvailable() {
        Table table = new Table(2);
        table.seatCustomers();

        table.markAvailable();

        assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
    }

    @Test
    void deactivate_setsActiveFalse() {
        Table table = new Table(6);

        table.deactivate();

        assertThat(table.isActive()).isFalse();
    }

    @Test
    void seatCustomers_rejectedWhenAlreadyOccupied() {
        Table table = new Table(2);
        table.seatCustomers();

        assertThatThrownBy(table::seatCustomers).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void seatCustomers_rejectedWhenInactive() {
        Table table = new Table(2);
        table.deactivate();

        assertThatThrownBy(table::seatCustomers).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_rejectsNonPositiveCapacity() {
        assertThatThrownBy(() -> new Table(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Table(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void newTables_haveDistinctIds() {
        Table a = new Table(2);
        Table b = new Table(2);

        assertThat(a.getId()).isNotEqualTo(b.getId());
    }
}
