package com.daust.restaurant.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.daust.restaurant.application.DailySummary;
import com.daust.restaurant.application.DailySummary.MethodBreakdown;
import com.daust.restaurant.application.SalesSummaryService;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.infrastructure.security.JpaUserDetailsService;
import com.daust.restaurant.infrastructure.security.AuditingAuthenticationFailureHandler;
import com.daust.restaurant.infrastructure.security.AuditingLogoutSuccessHandler;
import com.daust.restaurant.infrastructure.security.RoleBasedAuthenticationSuccessHandler;
import com.daust.restaurant.infrastructure.security.SecurityConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SalesSummaryController.class)
@Import(SecurityConfig.class)
class SalesSummaryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SalesSummaryService salesSummaryService;
    @MockitoBean private CurrentUserHelper currentUserHelper;
    @MockitoBean private RoleBasedAuthenticationSuccessHandler successHandler;
    @MockitoBean private AuditingAuthenticationFailureHandler failureHandler;
    @MockitoBean private AuditingLogoutSuccessHandler logoutSuccessHandler;
    @MockitoBean private JpaUserDetailsService userDetailsService;

    private static DailySummary emptySummary(LocalDate day) {
        Map<PaymentMethod, MethodBreakdown> byMethod = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod m : PaymentMethod.values()) {
            byMethod.put(m, new MethodBreakdown(0, new BigDecimal("0.00")));
        }
        return new DailySummary(
                day,
                0,
                new BigDecimal("0.00"),
                byMethod,
                new BigDecimal("0.00"),
                new BigDecimal("0.00"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getDaily_returns200_forManager_andDefaultsToToday() throws Exception {
        when(salesSummaryService.summaryForDay(any()))
                .thenReturn(emptySummary(LocalDate.now()));

        mockMvc.perform(get("/reports/daily"))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/daily"))
                .andExpect(model().attributeExists("summary", "date", "paymentMethods"));
        verify(salesSummaryService).summaryForDay(LocalDate.now());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDaily_returns200_forAdmin_withExplicitDate() throws Exception {
        LocalDate day = LocalDate.of(2026, 5, 24);
        when(salesSummaryService.summaryForDay(day)).thenReturn(emptySummary(day));

        mockMvc.perform(get("/reports/daily").param("date", "2026-05-24"))
                .andExpect(status().isOk());
        verify(salesSummaryService).summaryForDay(eq(day));
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void getDaily_returns403_forWaiter() throws Exception {
        mockMvc.perform(get("/reports/daily")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "KITCHEN_STAFF")
    void getDaily_returns403_forKitchen() throws Exception {
        mockMvc.perform(get("/reports/daily")).andExpect(status().isForbidden());
    }
}
