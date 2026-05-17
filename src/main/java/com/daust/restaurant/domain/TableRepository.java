package com.daust.restaurant.domain;

import java.util.List;
import java.util.Optional;

public interface TableRepository {

    void save(Table table);

    Optional<Table> findById(TableId id);

    List<Table> findAll();

    List<Table> findActiveByStatus(TableStatus status);
}
