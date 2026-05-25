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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * NFR-AUD-1 — emits {@code FAILED_LOGIN} on authentication failure, then delegates to the
 * standard redirect-to-/login?error flow.
 *
 * <p>The submitted password is never read or persisted. The attempted username is recorded in
 * the entry's {@code afterValue}.
 *
 * <p>{@link AuditLogEntry} requires non-null {@code userId} and {@code userRoleAtTime}, so when
 * the attempted username does not match any account (no real principal to attach), this handler
 * logs the attempt at WARN via SLF4J only and writes no audit row. Failed logins against real
 * accounts (the higher-value forensic case — credential probing of a known user) are audited
 * with that user's id and current role.
 */
@Component
public class AuditingAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log =
            LoggerFactory.getLogger(AuditingAuthenticationFailureHandler.class);

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public AuditingAuthenticationFailureHandler(
            UserRepository userRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        setDefaultFailureUrl("/login?error");
    }

    @Override
    @Transactional
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {

        String attemptedUsername = request.getParameter("username");
        if (attemptedUsername != null && !attemptedUsername.isBlank()) {
            Optional<User> maybeUser = userRepository.findByUsername(attemptedUsername);
            if (maybeUser.isPresent()) {
                User user = maybeUser.get();
                auditLogRepository.save(new AuditLogEntry(
                        user.getId(),
                        user.getRole(),
                        "FAILED_LOGIN",
                        "User",
                        user.getId().value().toString(),
                        null,
                        "username=" + attemptedUsername));
            } else {
                log.warn("FAILED_LOGIN for unknown username '{}'", attemptedUsername);
            }
        } else {
            log.warn("FAILED_LOGIN with no username submitted");
        }

        super.onAuthenticationFailure(request, response, exception);
    }
}
