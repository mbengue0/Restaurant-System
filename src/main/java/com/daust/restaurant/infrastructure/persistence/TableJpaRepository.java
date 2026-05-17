package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.TableStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TableJpaRepository extends JpaRepository<TableJpaEntity, UUID> {

    List<TableJpaEntity> findByActiveTrueAndStatus(TableStatus status);
}
