package com.daust.restaurant.presentation;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }
        for (GrantedAuthority a : authentication.getAuthorities()) {
            String role = a.getAuthority();
            if ("ROLE_KITCHEN_STAFF".equals(role)) {
                return "redirect:/kitchen";
            }
            if ("ROLE_WAITER".equals(role)) {
                return "redirect:/tables";
            }
        }
        return "redirect:/dashboard";
    }
}
