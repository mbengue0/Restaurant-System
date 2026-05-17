package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.Configuration;

final class ConfigurationMapper {

    static final int SINGLETON_ID = 1;

    private ConfigurationMapper() {
    }

    static Configuration toDomain(ConfigurationJpaEntity entity) {
        return Configuration.reconstitute(
                entity.getTaxRate(),
                entity.getServiceChargeRate(),
                entity.getCoverChargeAmount(),
                entity.isSplitMergePolicyEnabled(),
                entity.getAcceptedPaymentMethods());
    }

    static ConfigurationJpaEntity toEntity(Configuration configuration) {
        return new ConfigurationJpaEntity(
                SINGLETON_ID,
                configuration.getTaxRate(),
                configuration.getServiceChargeRate(),
                configuration.getCoverChargeAmount(),
                configuration.isSplitMergePolicyEnabled(),
                configuration.getAcceptedPaymentMethods());
    }
}
