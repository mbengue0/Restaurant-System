package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.Table;
import com.daust.restaurant.domain.TableId;

final class TableMapper {

    private TableMapper() {
    }

    static Table toDomain(TableJpaEntity entity) {
        return Table.reconstitute(
                TableId.of(entity.getId()),
                entity.getCapacity(),
                entity.getStatus(),
                entity.isActive());
    }

    static TableJpaEntity toEntity(Table table) {
        return new TableJpaEntity(
                table.getId().value(),
                table.getCapacity(),
                table.getStatus(),
                table.isActive());
    }
}
