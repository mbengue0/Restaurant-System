package com.daust.restaurant.infrastructure.security;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * NFR-AUD-1 — emits {@code USER_LOGOUT} on successful logout, then delegates to the standard
 * redirect-to-login flow.
 */
@Component
public class AuditingLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public AuditingLogoutSuccessHandler(
            UserRepository userRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        setDefaultTargetUrl("/login?logout");
    }

    @Override
    @Transactional
    public void onLogoutSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if (authentication != null && authentication.getName() != null) {
            Optional<User> maybeUser = userRepository.findByUsername(authentication.getName());
            maybeUser.ifPresent(user -> auditLogRepository.save(new AuditLogEntry(
                    user.getId(),
                    user.getRole(),
                    "USER_LOGOUT",
                    "User",
                    user.getId().value().toString(),
                    null,
                    "username=" + user.getUsername())));
        }
        super.onLogoutSuccess(request, response, authentication);
    }
}
