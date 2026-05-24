package com.daust.restaurant.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daust.restaurant.application.UserAccountService;
import com.daust.restaurant.domain.UserRepository;
import com.daust.restaurant.infrastructure.security.JpaUserDetailsService;
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

@WebMvcTest(UserManagementController.class)
@Import(SecurityConfig.class)
class UserManagementControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserAccountService userAccountService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CurrentUserHelper currentUserHelper;
    @MockitoBean private RoleBasedAuthenticationSuccessHandler successHandler;
    @MockitoBean private JpaUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsers_returns200_forAdmin() throws Exception {
        when(userAccountService.listUsers()).thenReturn(List.of());
        mockMvc.perform(get("/admin/users")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getUsers_returns403_forManager() throws Exception {
        mockMvc.perform(get("/admin/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void getUsers_returns403_forWaiter() throws Exception {
        mockMvc.perform(get("/admin/users")).andExpect(status().isForbidden());
    }
}
