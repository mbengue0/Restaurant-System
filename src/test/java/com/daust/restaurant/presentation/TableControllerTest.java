package com.daust.restaurant.presentation;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daust.restaurant.application.SeatCustomersService;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.infrastructure.security.JpaUserDetailsService;
import com.daust.restaurant.infrastructure.security.AuditingAuthenticationFailureHandler;
import com.daust.restaurant.infrastructure.security.AuditingLogoutSuccessHandler;
import com.daust.restaurant.infrastructure.security.RoleBasedAuthenticationSuccessHandler;
import com.daust.restaurant.infrastructure.security.SecurityConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TableController.class)
@Import(SecurityConfig.class)
class TableControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TableRepository tableRepository;
    @MockitoBean private OrderRepository orderRepository;
    @MockitoBean private SeatCustomersService seatCustomersService;
    @MockitoBean private CurrentUserHelper currentUserHelper;
    @MockitoBean private RoleBasedAuthenticationSuccessHandler successHandler;
    @MockitoBean private AuditingAuthenticationFailureHandler failureHandler;
    @MockitoBean private AuditingLogoutSuccessHandler logoutSuccessHandler;
    @MockitoBean private JpaUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "WAITER")
    void getTables_returns200_forWaiter() throws Exception {
        when(tableRepository.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/tables")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "KITCHEN_STAFF")
    void getTables_returns403_forKitchenStaff() throws Exception {
        mockMvc.perform(get("/tables")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void postRelease_redirectsToTables_forManager() throws Exception {
        UUID tableId = UUID.randomUUID();

        mockMvc.perform(post("/tables/" + tableId + "/release")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tables"));

        verify(seatCustomersService).releaseTable(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void postRelease_returns403_forWaiter_FR8() throws Exception {
        UUID tableId = UUID.randomUUID();

        mockMvc.perform(post("/tables/" + tableId + "/release")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "KITCHEN_STAFF")
    void postRelease_returns403_forKitchenStaff() throws Exception {
        UUID tableId = UUID.randomUUID();

        mockMvc.perform(post("/tables/" + tableId + "/release")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }
}
