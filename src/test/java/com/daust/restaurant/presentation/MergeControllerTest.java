package com.daust.restaurant.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daust.restaurant.application.MergeOrdersService;
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.ConfigurationRepository;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.OrderState;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.infrastructure.security.JpaUserDetailsService;
import com.daust.restaurant.infrastructure.security.RoleBasedAuthenticationSuccessHandler;
import com.daust.restaurant.infrastructure.security.SecurityConfig;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MergeController.class)
@Import(SecurityConfig.class)
class MergeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private OrderRepository orderRepository;
    @MockitoBean private BillRepository billRepository;
    @MockitoBean private ConfigurationRepository configurationRepository;
    @MockitoBean private TableRepository tableRepository;
    @MockitoBean private MergeOrdersService mergeOrdersService;
    @MockitoBean private CurrentUserHelper currentUserHelper;
    @MockitoBean private RoleBasedAuthenticationSuccessHandler successHandler;
    @MockitoBean private JpaUserDetailsService userDetailsService;

    private static Configuration mergeEnabled() {
        return new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("500.00"),
                true,
                EnumSet.of(PaymentMethod.CASH));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getMerge_returns200_forManager() throws Exception {
        when(configurationRepository.load()).thenReturn(Optional.of(mergeEnabled()));
        when(orderRepository.findByState(OrderState.SERVED)).thenReturn(List.of());

        mockMvc.perform(get("/orders/merge")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMerge_returns200_forAdmin() throws Exception {
        when(configurationRepository.load()).thenReturn(Optional.of(mergeEnabled()));
        when(orderRepository.findByState(OrderState.SERVED)).thenReturn(List.of());

        mockMvc.perform(get("/orders/merge")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void getMerge_returns403_forWaiter() throws Exception {
        mockMvc.perform(get("/orders/merge")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "KITCHEN_STAFF")
    void getMerge_returns403_forKitchen() throws Exception {
        mockMvc.perform(get("/orders/merge")).andExpect(status().isForbidden());
    }
}
