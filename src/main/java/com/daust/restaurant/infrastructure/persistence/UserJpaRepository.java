package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByUsername(String username);

    List<UserJpaEntity> findByActiveTrueAndRole(Role role);
}
