package com.daust.restaurant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfigurationTest {

    private static Configuration sampleConfig() {
        return new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("500.00"),
                false,
                EnumSet.of(PaymentMethod.CASH, PaymentMethod.CARD));
    }

    @Test
    void newConfiguration_storesProvidedValues() {
        Configuration cfg = sampleConfig();

        assertThat(cfg.getTaxRate()).isEqualByComparingTo("0.18");
        assertThat(cfg.getServiceChargeRate()).isEqualByComparingTo("0.10");
        assertThat(cfg.getCoverChargeAmount()).isEqualByComparingTo("500.00");
        assertThat(cfg.isSplitMergePolicyEnabled()).isFalse();
        assertThat(cfg.getAcceptedPaymentMethods()).containsExactlyInAnyOrder(PaymentMethod.CASH, PaymentMethod.CARD);
    }

    @Test
    void constructor_acceptsTaxRateZeroAndOneInclusive() {
        Configuration zero = new Configuration(
                BigDecimal.ZERO, new BigDecimal("0.10"), BigDecimal.ZERO, false, EnumSet.of(PaymentMethod.CASH));
        Configuration one = new Configuration(
                BigDecimal.ONE, new BigDecimal("0.10"), BigDecimal.ZERO, false, EnumSet.of(PaymentMethod.CASH));

        assertThat(zero.getTaxRate()).isEqualByComparingTo("0");
        assertThat(one.getTaxRate()).isEqualByComparingTo("1");
    }

    @Test
    void constructor_rejectsNegativeTaxRate() {
        assertThatThrownBy(() -> new Configuration(
                        new BigDecimal("-0.01"),
                        new BigDecimal("0.10"),
                        BigDecimal.ZERO,
                        false,
                        EnumSet.of(PaymentMethod.CASH)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsTaxRateAboveOne() {
        assertThatThrownBy(() -> new Configuration(
                        new BigDecimal("1.01"),
                        new BigDecimal("0.10"),
                        BigDecimal.ZERO,
                        false,
                        EnumSet.of(PaymentMethod.CASH)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNullTaxRate() {
        assertThatThrownBy(() -> new Configuration(
                        null, new BigDecimal("0.10"), BigDecimal.ZERO, false, EnumSet.of(PaymentMethod.CASH)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsInvalidServiceChargeRate() {
        assertThatThrownBy(() -> new Configuration(
                        new BigDecimal("0.18"),
                        new BigDecimal("-0.01"),
                        BigDecimal.ZERO,
                        false,
                        EnumSet.of(PaymentMethod.CASH)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Configuration(
                        new BigDecimal("0.18"),
                        new BigDecimal("1.01"),
                        BigDecimal.ZERO,
                        false,
                        EnumSet.of(PaymentMethod.CASH)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Configuration(
                        new BigDecimal("0.18"), null, BigDecimal.ZERO, false, EnumSet.of(PaymentMethod.CASH)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_acceptsCoverChargeZero() {
        Configuration cfg = new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                BigDecimal.ZERO,
                false,
                EnumSet.of(PaymentMethod.CASH));

        assertThat(cfg.getCoverChargeAmount()).isEqualByComparingTo("0");
    }

    @Test
    void constructor_rejectsNegativeCoverCharge() {
        assertThatThrownBy(() -> new Configuration(
                        new BigDecimal("0.18"),
                        new BigDecimal("0.10"),
                        new BigDecimal("-1.00"),
                        false,
                        EnumSet.of(PaymentMethod.CASH)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNullCoverCharge() {
        assertThatThrownBy(() -> new Configuration(
                        new BigDecimal("0.18"),
                        new BigDecimal("0.10"),
                        null,
                        false,
                        EnumSet.of(PaymentMethod.CASH)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsEmptyPaymentMethods() {
        assertThatThrownBy(() -> new Configuration(
                        new BigDecimal("0.18"),
                        new BigDecimal("0.10"),
                        BigDecimal.ZERO,
                        false,
                        EnumSet.noneOf(PaymentMethod.class)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNullPaymentMethods() {
        assertThatThrownBy(() -> new Configuration(
                        new BigDecimal("0.18"), new BigDecimal("0.10"), BigDecimal.ZERO, false, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void updateTaxRate_replacesValue() {
        Configuration cfg = sampleConfig();

        cfg.updateTaxRate(new BigDecimal("0.20"));

        assertThat(cfg.getTaxRate()).isEqualByComparingTo("0.20");
    }

    @Test
    void updateTaxRate_rejectsInvalid() {
        Configuration cfg = sampleConfig();

        assertThatThrownBy(() -> cfg.updateTaxRate(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cfg.updateTaxRate(new BigDecimal("1.01")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cfg.updateTaxRate(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void updateServiceChargeRate_replacesValue() {
        Configuration cfg = sampleConfig();

        cfg.updateServiceChargeRate(new BigDecimal("0.15"));

        assertThat(cfg.getServiceChargeRate()).isEqualByComparingTo("0.15");
    }

    @Test
    void updateCoverChargeAmount_replacesValue() {
        Configuration cfg = sampleConfig();

        cfg.updateCoverChargeAmount(new BigDecimal("750.00"));

        assertThat(cfg.getCoverChargeAmount()).isEqualByComparingTo("750.00");
    }

    @Test
    void updateCoverChargeAmount_rejectsNegative() {
        Configuration cfg = sampleConfig();

        assertThatThrownBy(() -> cfg.updateCoverChargeAmount(new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enableAndDisableSplitMergePolicy() {
        Configuration cfg = sampleConfig();

        cfg.enableSplitMergePolicy();
        assertThat(cfg.isSplitMergePolicyEnabled()).isTrue();

        cfg.disableSplitMergePolicy();
        assertThat(cfg.isSplitMergePolicyEnabled()).isFalse();
    }

    @Test
    void setAcceptedPaymentMethods_replacesAndDefensivelyCopies() {
        Configuration cfg = sampleConfig();
        Set<PaymentMethod> input = new HashSet<>();
        input.add(PaymentMethod.MOBILE_MONEY);

        cfg.setAcceptedPaymentMethods(input);

        // Mutating the caller's set must not leak into Configuration's state.
        input.add(PaymentMethod.CARD);
        assertThat(cfg.getAcceptedPaymentMethods()).containsExactly(PaymentMethod.MOBILE_MONEY);
    }

    @Test
    void setAcceptedPaymentMethods_rejectsEmpty() {
        Configuration cfg = sampleConfig();

        assertThatThrownBy(() -> cfg.setAcceptedPaymentMethods(EnumSet.noneOf(PaymentMethod.class)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setAcceptedPaymentMethods_rejectsNull() {
        Configuration cfg = sampleConfig();

        assertThatThrownBy(() -> cfg.setAcceptedPaymentMethods(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void getAcceptedPaymentMethods_returnsImmutableView() {
        Configuration cfg = sampleConfig();
        Set<PaymentMethod> snapshot = cfg.getAcceptedPaymentMethods();

        assertThatThrownBy(() -> snapshot.add(PaymentMethod.MOBILE_MONEY))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
