package com.daust.restaurant.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RoleBasedAuthenticationSuccessHandler successHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/webjars/**", "/error")
                        .permitAll()
                        .requestMatchers("/dashboard").hasAnyRole("ADMIN", "MANAGER")
                        // Menu management (UC04–UC07) is allowed for both Admin and Manager,
                        // even though it lives under /admin/. More-specific rule first.
                        .requestMatchers("/admin/menu/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/manager/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/kitchen/**").hasAnyRole("KITCHEN_STAFF", "ADMIN")
                        // Split/merge entry points sit under /orders/ but are Manager/Admin
                        // only (UC15/UC16). More-specific rule first.
                        .requestMatchers("/orders/*/split", "/orders/*/split/**")
                        .hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/waiter/**", "/tables/**", "/orders/**")
                        .hasAnyRole("WAITER", "MANAGER", "ADMIN")
                        .requestMatchers("/bills/**", "/payments/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/reports/**").hasAnyRole("MANAGER", "ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());
        return http.build();
    }
}
