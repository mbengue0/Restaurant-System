package com.daust.restaurant.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daust.restaurant.application.OrderLifecycleService;
import com.daust.restaurant.domain.MenuItemRepository;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.OrderState;
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

@WebMvcTest(KitchenController.class)
@Import(SecurityConfig.class)
class KitchenControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private OrderRepository orderRepository;
    @MockitoBean private MenuItemRepository menuItemRepository;
    @MockitoBean private OrderLifecycleService lifecycleService;
    @MockitoBean private CurrentUserHelper currentUserHelper;
    @MockitoBean private RoleBasedAuthenticationSuccessHandler successHandler;
    @MockitoBean private AuditingAuthenticationFailureHandler failureHandler;
    @MockitoBean private AuditingLogoutSuccessHandler logoutSuccessHandler;
    @MockitoBean private JpaUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "KITCHEN_STAFF")
    void getKitchen_returns200_forKitchenStaff() throws Exception {
        when(orderRepository.findByState(OrderState.PLACED)).thenReturn(List.of());
        when(orderRepository.findByState(OrderState.IN_PREPARATION)).thenReturn(List.of());
        when(orderRepository.findByState(OrderState.READY)).thenReturn(List.of());
        mockMvc.perform(get("/kitchen")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void getKitchen_returns403_forWaiter() throws Exception {
        mockMvc.perform(get("/kitchen")).andExpect(status().isForbidden());
    }
}
