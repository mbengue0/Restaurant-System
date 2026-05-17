package com.daust.restaurant.domain;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    void save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByUsername(String username);

    List<User> findAll();

    List<User> findActiveByRole(Role role);
}
