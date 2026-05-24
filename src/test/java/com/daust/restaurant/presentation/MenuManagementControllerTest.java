package com.daust.restaurant.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daust.restaurant.application.MenuManagementService;
import com.daust.restaurant.domain.CategoryRepository;
import com.daust.restaurant.domain.MenuItemRepository;
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

@WebMvcTest(MenuManagementController.class)
@Import(SecurityConfig.class)
class MenuManagementControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MenuManagementService menuService;
    @MockitoBean private CategoryRepository categoryRepository;
    @MockitoBean private MenuItemRepository menuItemRepository;
    @MockitoBean private CurrentUserHelper currentUserHelper;
    @MockitoBean private RoleBasedAuthenticationSuccessHandler successHandler;
    @MockitoBean private JpaUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMenu_returns200_forAdmin() throws Exception {
        when(menuService.listCategories()).thenReturn(List.of());
        when(menuService.listMenuItems()).thenReturn(List.of());

        mockMvc.perform(get("/admin/menu")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getMenu_returns200_forManager() throws Exception {
        when(menuService.listCategories()).thenReturn(List.of());
        when(menuService.listMenuItems()).thenReturn(List.of());

        mockMvc.perform(get("/admin/menu")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void getMenu_returns403_forWaiter() throws Exception {
        mockMvc.perform(get("/admin/menu")).andExpect(status().isForbidden());
    }
}
