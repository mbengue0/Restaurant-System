package com.daust.restaurant.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MenuItemJpaRepository extends JpaRepository<MenuItemJpaEntity, UUID> {

    List<MenuItemJpaEntity> findByActiveTrueAndCategoryId(UUID categoryId);
}
