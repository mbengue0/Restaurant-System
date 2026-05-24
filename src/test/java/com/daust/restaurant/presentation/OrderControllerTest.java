package com.daust.restaurant.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daust.restaurant.application.CancelOrderService;
import com.daust.restaurant.application.OrderLifecycleService;
import com.daust.restaurant.application.PlaceOrderService;
import com.daust.restaurant.domain.CategoryRepository;
import com.daust.restaurant.domain.MenuItemRepository;
import com.daust.restaurant.domain.Order;
import com.daust.restaurant.domain.OrderId;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.domain.OrderState;
import com.daust.restaurant.domain.TableId;
import com.daust.restaurant.domain.TableRepository;
import com.daust.restaurant.infrastructure.security.JpaUserDetailsService;
import com.daust.restaurant.infrastructure.security.RoleBasedAuthenticationSuccessHandler;
import com.daust.restaurant.infrastructure.security.SecurityConfig;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private OrderRepository orderRepository;
    @MockitoBean private MenuItemRepository menuItemRepository;
    @MockitoBean private CategoryRepository categoryRepository;
    @MockitoBean private TableRepository tableRepository;
    @MockitoBean private PlaceOrderService placeOrderService;
    @MockitoBean private OrderLifecycleService lifecycleService;
    @MockitoBean private CancelOrderService cancelOrderService;
    @MockitoBean private CurrentUserHelper currentUserHelper;
    @MockitoBean private RoleBasedAuthenticationSuccessHandler successHandler;
    @MockitoBean private JpaUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "WAITER")
    void getOrdersNew_returns200_forWaiter() throws Exception {
        when(categoryRepository.findAllActiveOrderedByDisplayOrder()).thenReturn(List.of());
        mockMvc.perform(get("/orders/new").param("tableId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "KITCHEN_STAFF")
    void getOrdersNew_returns403_forKitchenStaff() throws Exception {
        mockMvc.perform(get("/orders/new").param("tableId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void getOrdersActive_returns200_forWaiter() throws Exception {
        when(orderRepository.findByState(OrderState.PLACED)).thenReturn(List.of());
        when(orderRepository.findByState(OrderState.IN_PREPARATION)).thenReturn(List.of());
        when(orderRepository.findByState(OrderState.READY)).thenReturn(List.of());
        when(orderRepository.findByState(OrderState.SERVED)).thenReturn(List.of());
        mockMvc.perform(get("/orders/active")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "KITCHEN_STAFF")
    void getOrdersActive_returns403_forKitchenStaff() throws Exception {
        mockMvc.perform(get("/orders/active")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void getOrderCancel_returns200_forWaiter() throws Exception {
        Order order = Order.reconstitute(
                OrderId.generate(),
                TableId.generate(),
                OrderState.PLACED,
                LocalDateTime.now(),
                null, null, null, null, null, null, null,
                false,
                new ArrayList<>());
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(tableRepository.findById(order.getTableId())).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/{id}/cancel", order.getId().value()))
                .andExpect(status().isOk());
    }
}
