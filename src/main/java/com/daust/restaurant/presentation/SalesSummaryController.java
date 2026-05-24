package com.daust.restaurant.presentation;

import com.daust.restaurant.application.DailySummary;
import com.daust.restaurant.application.SalesSummaryService;
import com.daust.restaurant.domain.PaymentMethod;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * UC19 — Daily Sales Summary. Manager + Admin (gated by {@code /reports/**} rule in
 * {@link com.daust.restaurant.infrastructure.security.SecurityConfig}).
 */
@Controller
public class SalesSummaryController {

    private final SalesSummaryService salesSummaryService;

    public SalesSummaryController(SalesSummaryService salesSummaryService) {
        this.salesSummaryService = salesSummaryService;
    }

    @GetMapping("/reports/daily")
    public String daily(
            @RequestParam(value = "date", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        LocalDate day = date != null ? date : LocalDate.now();
        DailySummary summary = salesSummaryService.summaryForDay(day);

        model.addAttribute("summary", summary);
        model.addAttribute("date", day);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "reports/daily";
    }
}
