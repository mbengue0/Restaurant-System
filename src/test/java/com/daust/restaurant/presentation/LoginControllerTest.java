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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LoginController.class)
@Import(SecurityConfig.class)
class LoginControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RoleBasedAuthenticationSuccessHandler successHandler;
    @MockitoBean private JpaUserDetailsService userDetailsService;

    @Test
    void getLogin_returns200_evenUnauthenticated() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }
}
