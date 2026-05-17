package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.domain.TableStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TableRepositoryImpl implements TableRepository {

    private final TableJpaRepository jpaRepository;

    TableRepositoryImpl(TableJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Table table) {
        jpaRepository.save(TableMapper.toEntity(table));
    }

    @Override
    public Optional<Table> findById(TableId id) {
        return jpaRepository.findById(id.value()).map(TableMapper::toDomain);
    }

    @Override
    public List<Table> findAll() {
        return jpaRepository.findAll().stream().map(TableMapper::toDomain).toList();
    }

    @Override
    public List<Table> findActiveByStatus(TableStatus status) {
        return jpaRepository.findByActiveTrueAndStatus(status).stream()
                .map(TableMapper::toDomain)
                .toList();
    }
}
