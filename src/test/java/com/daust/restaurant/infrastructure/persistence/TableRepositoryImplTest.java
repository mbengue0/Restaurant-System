package com.daust.restaurant.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.domain.TableStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(TableRepositoryImpl.class)
class TableRepositoryImplTest {

    @Autowired
    private TableRepository tableRepository;

    @Test
    void saveAndFindById_roundTripsThroughMapper() {
        Table original = new Table(4);

        tableRepository.save(original);

        Optional<Table> loaded = tableRepository.findById(original.getId());
        assertThat(loaded).isPresent();
        Table found = loaded.get();
        assertThat(found.getId()).isEqualTo(original.getId());
        assertThat(found.getCapacity()).isEqualTo(4);
        assertThat(found.getStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(found.isActive()).isTrue();
    }

    @Test
    void save_persistsStatusChangeAfterSeating() {
        Table table = new Table(2);
        table.seatCustomers();

        tableRepository.save(table);

        Table reloaded = tableRepository.findById(table.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TableStatus.OCCUPIED);
    }

    @Test
    void findAll_returnsAllSavedTables() {
        tableRepository.save(new Table(2));
        tableRepository.save(new Table(4));
        tableRepository.save(new Table(6));

        List<Table> all = tableRepository.findAll();

        assertThat(all).hasSize(3);
    }

    @Test
    void findActiveByStatus_excludesInactiveTables() {
        Table activeAvail = new Table(2);
        Table occupied = new Table(4);
        occupied.seatCustomers();
        Table inactive = new Table(2);
        inactive.deactivate();

        tableRepository.save(activeAvail);
        tableRepository.save(occupied);
        tableRepository.save(inactive);

        List<Table> available = tableRepository.findActiveByStatus(TableStatus.AVAILABLE);
        List<Table> occupiedList = tableRepository.findActiveByStatus(TableStatus.OCCUPIED);

        assertThat(available).extracting(Table::getId).containsExactly(activeAvail.getId());
        assertThat(occupiedList).extracting(Table::getId).containsExactly(occupied.getId());
    }
}
