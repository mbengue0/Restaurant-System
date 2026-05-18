package com.daust.restaurant.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daust.restaurant.application.GenerateBillService;
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.ConfigurationRepository;
import com.daust.restaurant.domain.MenuItemRepository;
import com.daust.restaurant.domain.OrderRepository;
import com.daust.restaurant.infrastructure.security.JpaUserDetailsService;
import com.daust.restaurant.infrastructure.security.RoleBasedAuthenticationSuccessHandler;
import com.daust.restaurant.infrastructure.security.SecurityConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BillController.class)
@Import(SecurityConfig.class)
class BillControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private OrderRepository orderRepository;
    @MockitoBean private BillRepository billRepository;
    @MockitoBean private ConfigurationRepository configurationRepository;
    @MockitoBean private MenuItemRepository menuItemRepository;
    @MockitoBean private GenerateBillService generateBillService;
    @MockitoBean private CurrentUserHelper currentUserHelper;
    @MockitoBean private RoleBasedAuthenticationSuccessHandler successHandler;
    @MockitoBean private JpaUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "WAITER")
    void getBillsNew_returns403_forWaiter() throws Exception {
        mockMvc.perform(get("/bills/new").param("orderId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "KITCHEN_STAFF")
    void getBillById_returns403_forKitchenStaff() throws Exception {
        mockMvc.perform(get("/bills/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
