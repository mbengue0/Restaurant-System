package com.daust.restaurant.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.daust.restaurant.application.AuditLogQueryService;
import com.daust.restaurant.infrastructure.security.JpaUserDetailsService;
import com.daust.restaurant.infrastructure.security.AuditingAuthenticationFailureHandler;
import com.daust.restaurant.infrastructure.security.AuditingLogoutSuccessHandler;
import com.daust.restaurant.infrastructure.security.RoleBasedAuthenticationSuccessHandler;
import com.daust.restaurant.infrastructure.security.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditLogController.class)
@Import(SecurityConfig.class)
class AuditLogControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuditLogQueryService auditLogQueryService;
    @MockitoBean private CurrentUserHelper currentUserHelper;
    @MockitoBean private RoleBasedAuthenticationSuccessHandler successHandler;
    @MockitoBean private AuditingAuthenticationFailureHandler failureHandler;
    @MockitoBean private AuditingLogoutSuccessHandler logoutSuccessHandler;
    @MockitoBean private JpaUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAudit_returns200_forAdmin_andRendersList() throws Exception {
        when(auditLogQueryService.recentEntries(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit/list"))
                .andExpect(model().attributeExists("entries", "eventTypes"));
        verify(auditLogQueryService).recentEntries(100);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAudit_withEventType_filtersByEventType() throws Exception {
        when(auditLogQueryService.entriesByEventType("PAYMENT_RECORDED")).thenReturn(List.of());

        mockMvc.perform(get("/admin/audit").param("eventType", "PAYMENT_RECORDED"))
                .andExpect(status().isOk());
        verify(auditLogQueryService).entriesByEventType("PAYMENT_RECORDED");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAudit_withDateRange_filtersByDateRange() throws Exception {
        when(auditLogQueryService.entriesBetween(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/admin/audit")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk());
        verify(auditLogQueryService).entriesBetween(any(), any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getAudit_returns403_forManager() throws Exception {
        mockMvc.perform(get("/admin/audit")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void getAudit_returns403_forWaiter() throws Exception {
        mockMvc.perform(get("/admin/audit")).andExpect(status().isForbidden());
    }
}
