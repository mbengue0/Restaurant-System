package com.daust.restaurant.infrastructure.security;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RoleBasedAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public RoleBasedAuthenticationSuccessHandler(
            UserRepository userRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        recordLoginAndAudit(authentication.getName());
        String target = targetForRole(roleOf(authentication));
        getRedirectStrategy().sendRedirect(request, response, target);
    }

    private void recordLoginAndAudit(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.isActive()) {
                user.recordLogin(Instant.now());
                userRepository.save(user);
                // NFR-AUD-1: authentication events are audited.
                auditLogRepository.save(new AuditLogEntry(
                        user.getId(),
                        user.getRole(),
                        "USER_LOGIN",
                        "User",
                        user.getId().value().toString(),
                        null,
                        "username=" + user.getUsername()));
            }
        });
    }

    private String roleOf(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role != null && role.startsWith("ROLE_")) {
                return role;
            }
        }
        return "";
    }

    private String targetForRole(String role) {
        return switch (role) {
            case "ROLE_ADMIN", "ROLE_MANAGER" -> "/dashboard";
            case "ROLE_WAITER" -> "/tables";
            case "ROLE_KITCHEN_STAFF" -> "/kitchen";
            default -> "/login";
        };
    }

}
