package com.daust.restaurant.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.daust.restaurant.application.TableManagementService;
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

@WebMvcTest(TableManagementController.class)
@Import(SecurityConfig.class)
class TableManagementControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TableManagementService tableManagementService;
    @MockitoBean private CurrentUserHelper currentUserHelper;
    @MockitoBean private RoleBasedAuthenticationSuccessHandler successHandler;
    @MockitoBean private AuditingAuthenticationFailureHandler failureHandler;
    @MockitoBean private AuditingLogoutSuccessHandler logoutSuccessHandler;
    @MockitoBean private JpaUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTables_returns200_forAdmin() throws Exception {
        when(tableManagementService.listTables()).thenReturn(List.of());

        mockMvc.perform(get("/admin/tables"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tables/list"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getNewTableForm_returns200_forAdmin() throws Exception {
        mockMvc.perform(get("/admin/tables/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tables/new"));
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void getTables_returns403_forWaiter() throws Exception {
        mockMvc.perform(get("/admin/tables")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getTables_returns403_forManager() throws Exception {
        mockMvc.perform(get("/admin/tables")).andExpect(status().isForbidden());
    }
}
