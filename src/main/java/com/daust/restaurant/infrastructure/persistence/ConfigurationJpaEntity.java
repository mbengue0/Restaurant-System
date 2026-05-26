package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "configuration")
@Check(name = "ck_configuration_singleton", constraints = "id = 1")
@Check(name = "ck_configuration_tax_rate_range", constraints = "tax_rate >= 0 AND tax_rate <= 1")
@Check(
        name = "ck_configuration_service_rate_range",
        constraints = "service_charge_rate >= 0 AND service_charge_rate <= 1")
@Check(name = "ck_configuration_cover_nonneg", constraints = "cover_charge_amount >= 0")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
class ConfigurationJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "service_charge_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal serviceChargeRate;

    @Column(name = "cover_charge_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal coverChargeAmount;

    @Column(name = "split_merge_policy_enabled", nullable = false)
    private boolean splitMergePolicyEnabled;

    @Convert(converter = PaymentMethodSetConverter.class)
    @Column(name = "accepted_payment_methods", nullable = false, columnDefinition = "TEXT")
    private Set<PaymentMethod> acceptedPaymentMethods;
}
