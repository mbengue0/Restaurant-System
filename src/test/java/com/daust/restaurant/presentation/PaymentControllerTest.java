package com.daust.restaurant.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daust.restaurant.application.RecordPaymentService;
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.ConfigurationRepository;
import com.daust.restaurant.domain.PaymentRepository;
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

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private BillRepository billRepository;
    @MockitoBean private PaymentRepository paymentRepository;
    @MockitoBean private ConfigurationRepository configurationRepository;
    @MockitoBean private RecordPaymentService recordPaymentService;
    @MockitoBean private CurrentUserHelper currentUserHelper;
    @MockitoBean private RoleBasedAuthenticationSuccessHandler successHandler;
    @MockitoBean private JpaUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "WAITER")
    void getPaymentsNew_returns403_forWaiter() throws Exception {
        mockMvc.perform(get("/payments/new").param("billId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "KITCHEN_STAFF")
    void getPaymentConfirmation_returns403_forKitchenStaff() throws Exception {
        mockMvc.perform(get("/payments/confirmation/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
