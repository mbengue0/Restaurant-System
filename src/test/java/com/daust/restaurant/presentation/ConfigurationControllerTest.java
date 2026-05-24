package com.daust.restaurant.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daust.restaurant.application.ConfigurationService;
import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.infrastructure.security.JpaUserDetailsService;
import com.daust.restaurant.infrastructure.security.RoleBasedAuthenticationSuccessHandler;
import com.daust.restaurant.infrastructure.security.SecurityConfig;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConfigurationController.class)
@Import(SecurityConfig.class)
class ConfigurationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ConfigurationService configurationService;
    @MockitoBean private CurrentUserHelper currentUserHelper;
    @MockitoBean private RoleBasedAuthenticationSuccessHandler successHandler;
    @MockitoBean private JpaUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getConfiguration_returns200_forAdmin() throws Exception {
        Set<PaymentMethod> methods = EnumSet.of(PaymentMethod.CASH, PaymentMethod.CARD);
        Configuration config = new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("500"),
                false,
                methods);
        when(configurationService.getConfiguration()).thenReturn(config);

        mockMvc.perform(get("/admin/configuration")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getConfiguration_returns403_forManager() throws Exception {
        mockMvc.perform(get("/admin/configuration")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void getConfiguration_returns403_forWaiter() throws Exception {
        mockMvc.perform(get("/admin/configuration")).andExpect(status().isForbidden());
    }
}
