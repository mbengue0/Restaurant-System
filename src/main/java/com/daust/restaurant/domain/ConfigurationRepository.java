package com.daust.restaurant.domain;

import java.util.Optional;

public interface ConfigurationRepository {

    Optional<Configuration> load();

    void save(Configuration configuration);
}
