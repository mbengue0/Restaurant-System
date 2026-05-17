package com.daust.restaurant.domain;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public class Configuration {

    private BigDecimal taxRate;
    private BigDecimal serviceChargeRate;
    private BigDecimal coverChargeAmount;
    private boolean splitMergePolicyEnabled;
    private EnumSet<PaymentMethod> acceptedPaymentMethods;

    public Configuration(
            BigDecimal taxRate,
            BigDecimal serviceChargeRate,
            BigDecimal coverChargeAmount,
            boolean splitMergePolicyEnabled,
            Set<PaymentMethod> acceptedPaymentMethods) {
        this.taxRate = requireRateBetweenZeroAndOne(taxRate, "taxRate");
        this.serviceChargeRate = requireRateBetweenZeroAndOne(serviceChargeRate, "serviceChargeRate");
        this.coverChargeAmount = requireNonNegative(coverChargeAmount, "coverChargeAmount");
        this.splitMergePolicyEnabled = splitMergePolicyEnabled;
        this.acceptedPaymentMethods = copyAndRequireNonEmpty(acceptedPaymentMethods);
    }

    public static Configuration reconstitute(
            BigDecimal taxRate,
            BigDecimal serviceChargeRate,
            BigDecimal coverChargeAmount,
            boolean splitMergePolicyEnabled,
            Set<PaymentMethod> acceptedPaymentMethods) {
        return new Configuration(
                taxRate,
                serviceChargeRate,
                coverChargeAmount,
                splitMergePolicyEnabled,
                acceptedPaymentMethods);
    }

    public void updateTaxRate(BigDecimal newTaxRate) {
        this.taxRate = requireRateBetweenZeroAndOne(newTaxRate, "taxRate");
    }

    public void updateServiceChargeRate(BigDecimal newServiceChargeRate) {
        this.serviceChargeRate = requireRateBetweenZeroAndOne(newServiceChargeRate, "serviceChargeRate");
    }

    public void updateCoverChargeAmount(BigDecimal newCoverChargeAmount) {
        this.coverChargeAmount = requireNonNegative(newCoverChargeAmount, "coverChargeAmount");
    }

    public void enableSplitMergePolicy() {
        this.splitMergePolicyEnabled = true;
    }

    public void disableSplitMergePolicy() {
        this.splitMergePolicyEnabled = false;
    }

    public void setAcceptedPaymentMethods(Set<PaymentMethod> newAcceptedPaymentMethods) {
        this.acceptedPaymentMethods = copyAndRequireNonEmpty(newAcceptedPaymentMethods);
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public BigDecimal getServiceChargeRate() {
        return serviceChargeRate;
    }

    public BigDecimal getCoverChargeAmount() {
        return coverChargeAmount;
    }

    public boolean isSplitMergePolicyEnabled() {
        return splitMergePolicyEnabled;
    }

    public Set<PaymentMethod> getAcceptedPaymentMethods() {
        return Set.copyOf(acceptedPaymentMethods);
    }

    private static BigDecimal requireRateBetweenZeroAndOne(BigDecimal value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1 (inclusive)");
        }
        return value;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
        return value;
    }

    private static EnumSet<PaymentMethod> copyAndRequireNonEmpty(Set<PaymentMethod> methods) {
        Objects.requireNonNull(methods, "acceptedPaymentMethods must not be null");
        if (methods.isEmpty()) {
            throw new IllegalArgumentException("acceptedPaymentMethods must not be empty");
        }
        return EnumSet.copyOf(methods);
    }
}
