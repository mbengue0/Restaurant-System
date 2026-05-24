package com.daust.restaurant.application;

import com.daust.restaurant.application.DailySummary.MethodBreakdown;
import com.daust.restaurant.domain.Bill;
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.Payment;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.domain.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC19 — Generate Daily Sales Summary. Manager + Admin reporting.
 *
 * <p>The day's revenue is computed off {@link Payment#getAmountDue()} (which is snapshotted from
 * {@link Bill#getTotal()} at payment time), so the figure remains stable even if later
 * configuration changes alter how new bills would be calculated. Tax/service totals are looked up
 * on the underlying Bills (these are also snapshot values from {@link Bill}'s constructor).
 *
 * <p>NFR-PERF-3 (reporting) is satisfied by the index-friendly {@code findByRecordedAtBetween}
 * query and a single per-bill lookup per payment — N+1 is fine at the volume in scope.
 */
@Service
@Transactional(readOnly = true)
public class SalesSummaryService {

    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;

    public SalesSummaryService(
            BillRepository billRepository, PaymentRepository paymentRepository) {
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
    }

    public DailySummary summaryForDay(LocalDate day) {
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.atTime(LocalTime.MAX);

        List<Payment> payments = paymentRepository.findByRecordedAtBetween(from, to);

        Map<PaymentMethod, Integer> methodCounts = new EnumMap<>(PaymentMethod.class);
        Map<PaymentMethod, BigDecimal> methodTotals = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod m : PaymentMethod.values()) {
            methodCounts.put(m, 0);
            methodTotals.put(m, ZERO_MONEY);
        }

        BigDecimal totalRevenue = ZERO_MONEY;
        BigDecimal totalTax = ZERO_MONEY;
        BigDecimal totalService = ZERO_MONEY;

        for (Payment p : payments) {
            BigDecimal due = p.getAmountDue();
            totalRevenue = totalRevenue.add(due);
            methodCounts.merge(p.getMethod(), 1, Integer::sum);
            methodTotals.merge(p.getMethod(), due, BigDecimal::add);

            Bill bill = billRepository
                    .findById(p.getBillId())
                    .orElseThrow(() -> new BillNotFoundException(
                            "Bill not found for payment " + p.getId() + ": " + p.getBillId()));
            totalTax = totalTax.add(bill.getTaxAmount());
            totalService = totalService.add(bill.getServiceChargeAmount());
        }

        Map<PaymentMethod, MethodBreakdown> byMethod = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod m : PaymentMethod.values()) {
            byMethod.put(m, new MethodBreakdown(methodCounts.get(m), methodTotals.get(m)));
        }

        return new DailySummary(day, payments.size(), totalRevenue, byMethod, totalTax, totalService);
    }
}
