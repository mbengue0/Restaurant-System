package com.daust.restaurant.application;

import com.daust.restaurant.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * UC19 — Daily sales summary for {@link #date}. All money values are CFA at scale 2.
 *
 * @param date the report day
 * @param billCount number of bills with at least one payment recorded on {@code date}
 * @param totalRevenue Σ payment.amountDue across the day's payments (= Σ bill.total for paid
 *     bills, with split rounding already absorbed into each bill)
 * @param byMethod count and total revenue grouped by {@link PaymentMethod} — every method present
 *     in the enum is included, even when zero, so the template can iterate predictably
 * @param totalTax Σ bill.taxAmount for bills paid on {@code date}
 * @param totalServiceCharge Σ bill.serviceChargeAmount for bills paid on {@code date}
 */
public record DailySummary(
        LocalDate date,
        int billCount,
        BigDecimal totalRevenue,
        Map<PaymentMethod, MethodBreakdown> byMethod,
        BigDecimal totalTax,
        BigDecimal totalServiceCharge) {

    public record MethodBreakdown(int count, BigDecimal total) {}
}
