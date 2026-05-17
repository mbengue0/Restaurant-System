package com.daust.restaurant.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface ConfigurationJpaRepository extends JpaRepository<ConfigurationJpaEntity, Integer> {
}
