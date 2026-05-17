package com.daust.restaurant.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.ConfigurationRepository;
import com.daust.restaurant.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({ConfigurationRepositoryImpl.class, PaymentMethodSetConverter.class})
class ConfigurationRepositoryImplTest {

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Test
    void load_returnsEmptyWhenNotSeeded() {
        Optional<Configuration> loaded = configurationRepository.load();

        assertThat(loaded).isEmpty();
    }

    @Test
    void saveAndLoad_roundTripsAllFields() {
        Configuration cfg = new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("500.00"),
                true,
                EnumSet.of(PaymentMethod.CASH, PaymentMethod.CARD, PaymentMethod.MOBILE_MONEY));

        configurationRepository.save(cfg);

        Configuration reloaded = configurationRepository.load().orElseThrow();
        assertThat(reloaded.getTaxRate()).isEqualByComparingTo("0.18");
        assertThat(reloaded.getServiceChargeRate()).isEqualByComparingTo("0.10");
        assertThat(reloaded.getCoverChargeAmount()).isEqualByComparingTo("500.00");
        assertThat(reloaded.isSplitMergePolicyEnabled()).isTrue();
        assertThat(reloaded.getAcceptedPaymentMethods())
                .containsExactlyInAnyOrder(PaymentMethod.CASH, PaymentMethod.CARD, PaymentMethod.MOBILE_MONEY);
    }

    @Test
    void save_overwritesExistingSingleton() {
        configurationRepository.save(new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("500.00"),
                false,
                EnumSet.of(PaymentMethod.CASH)));

        Configuration loaded = configurationRepository.load().orElseThrow();
        loaded.updateTaxRate(new BigDecimal("0.20"));
        loaded.updateServiceChargeRate(new BigDecimal("0.12"));
        loaded.updateCoverChargeAmount(new BigDecimal("750.00"));
        loaded.enableSplitMergePolicy();
        loaded.setAcceptedPaymentMethods(EnumSet.of(PaymentMethod.CARD, PaymentMethod.MOBILE_MONEY));
        configurationRepository.save(loaded);

        Configuration reloaded = configurationRepository.load().orElseThrow();
        assertThat(reloaded.getTaxRate()).isEqualByComparingTo("0.20");
        assertThat(reloaded.getServiceChargeRate()).isEqualByComparingTo("0.12");
        assertThat(reloaded.getCoverChargeAmount()).isEqualByComparingTo("750.00");
        assertThat(reloaded.isSplitMergePolicyEnabled()).isTrue();
        assertThat(reloaded.getAcceptedPaymentMethods())
                .containsExactlyInAnyOrder(PaymentMethod.CARD, PaymentMethod.MOBILE_MONEY);
    }

    @Test
    void save_persistsSinglePaymentMethod() {
        configurationRepository.save(new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("500.00"),
                false,
                EnumSet.of(PaymentMethod.MOBILE_MONEY)));

        Configuration reloaded = configurationRepository.load().orElseThrow();
        assertThat(reloaded.getAcceptedPaymentMethods()).containsExactly(PaymentMethod.MOBILE_MONEY);
    }
}
