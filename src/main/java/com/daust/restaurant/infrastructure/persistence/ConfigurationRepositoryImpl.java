package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.ConfigurationRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ConfigurationRepositoryImpl implements ConfigurationRepository {

    private final ConfigurationJpaRepository jpaRepository;

    ConfigurationRepositoryImpl(ConfigurationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Configuration> load() {
        return jpaRepository.findById(ConfigurationMapper.SINGLETON_ID).map(ConfigurationMapper::toDomain);
    }

    @Override
    public void save(Configuration configuration) {
        jpaRepository.save(ConfigurationMapper.toEntity(configuration));
    }
}
