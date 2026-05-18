package com.daust.restaurant.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daust.restaurant.infrastructure.security.JpaUserDetailsService;
import com.daust.restaurant.infrastructure.security.RoleBasedAuthenticationSuccessHandler;
import com.daust.restaurant.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RoleBasedAuthenticationSuccessHandler successHandler;
    @MockitoBean private JpaUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "MANAGER")
    void getDashboard_returns200_forManager() throws Exception {
        mockMvc.perform(get("/dashboard")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void getDashboard_returns403_forWaiter() throws Exception {
        mockMvc.perform(get("/dashboard")).andExpect(status().isForbidden());
    }
}
