package com.daust.restaurant.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daust.restaurant.application.SplitOrderService;
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.Configuration;
import com.daust.restaurant.domain.ConfigurationRepository;
import com.daust.restaurant.domain.MenuItemRepository;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.infrastructure.security.JpaUserDetailsService;
import com.daust.restaurant.infrastructure.security.RoleBasedAuthenticationSuccessHandler;
import com.daust.restaurant.infrastructure.security.SecurityConfig;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SplitController.class)
@Import(SecurityConfig.class)
class SplitControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private OrderRepository orderRepository;
    @MockitoBean private BillRepository billRepository;
    @MockitoBean private ConfigurationRepository configurationRepository;
    @MockitoBean private MenuItemRepository menuItemRepository;
    @MockitoBean private SplitOrderService splitOrderService;
    @MockitoBean private CurrentUserHelper currentUserHelper;
    @MockitoBean private RoleBasedAuthenticationSuccessHandler successHandler;
    @MockitoBean private JpaUserDetailsService userDetailsService;

    private static Configuration splitEnabledConfig() {
        return new Configuration(
                new BigDecimal("0.18"),
                new BigDecimal("0.10"),
                new BigDecimal("600.00"),
                true,
                EnumSet.of(PaymentMethod.CASH));
    }

    private static Order servedOrderNoItems() {
        com.daust.restaurant.domain.Table t = new com.daust.restaurant.domain.Table(4);
        t.seatCustomers();
        return com.daust.restaurant.domain.Order.reconstitute(
                com.daust.restaurant.domain.OrderId.generate(),
                TableId.of(UUID.randomUUID()),
                com.daust.restaurant.domain.OrderState.SERVED,
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                true,
                List.of());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getSplit_returns200_forManager() throws Exception {
        UUID orderId = UUID.randomUUID();
        Order order = servedOrderNoItems();
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(configurationRepository.load()).thenReturn(Optional.of(splitEnabledConfig()));
        when(billRepository.findByOrderId(any())).thenReturn(List.of());

        mockMvc.perform(get("/orders/" + orderId + "/split"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSplit_returns200_forAdmin() throws Exception {
        UUID orderId = UUID.randomUUID();
        Order order = servedOrderNoItems();
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(configurationRepository.load()).thenReturn(Optional.of(splitEnabledConfig()));
        when(billRepository.findByOrderId(any())).thenReturn(List.of());

        mockMvc.perform(get("/orders/" + orderId + "/split"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void getSplit_returns403_forWaiter() throws Exception {
        mockMvc.perform(get("/orders/" + UUID.randomUUID() + "/split"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "KITCHEN_STAFF")
    void getSplit_returns403_forKitchen() throws Exception {
        mockMvc.perform(get("/orders/" + UUID.randomUUID() + "/split"))
                .andExpect(status().isForbidden());
    }
}
