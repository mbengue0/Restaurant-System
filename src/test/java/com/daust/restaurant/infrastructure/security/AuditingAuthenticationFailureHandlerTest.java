package com.daust.restaurant.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

@ExtendWith(MockitoExtension.class)
class AuditingAuthenticationFailureHandlerTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @Test
    void onAuthenticationFailure_emitsFailedLogin_whenUsernameMatchesRealUser() throws Exception {
        AuditingAuthenticationFailureHandler handler =
                new AuditingAuthenticationFailureHandler(userRepository, auditLogRepository);

        User mgr = new User("mgr", HASH, "Manager", Role.MANAGER, false);
        when(userRepository.findByUsername("mgr")).thenReturn(Optional.of(mgr));

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("username", "mgr");
        req.setParameter("password", "should-never-be-touched");
        MockHttpServletResponse res = new MockHttpServletResponse();
        AuthenticationException ex = new BadCredentialsException("bad creds");

        handler.onAuthenticationFailure(req, res, ex);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntry entry = captor.getValue();
        assertThat(entry.getEventType()).isEqualTo("FAILED_LOGIN");
        assertThat(entry.getAffectedEntityType()).isEqualTo("User");
        assertThat(entry.getUserId()).isEqualTo(mgr.getId());
        assertThat(entry.getUserRoleAtTime()).isEqualTo(Role.MANAGER);
        assertThat(entry.getAfterValue()).isEqualTo("username=mgr");
        // The password value submitted in the form must not be in the audit entry.
        assertThat(entry.getBeforeValue()).isNull();
        assertThat(entry.getAfterValue()).doesNotContain("should-never-be-touched");
    }

    @Test
    void onAuthenticationFailure_doesNotEmit_whenUsernameUnknown() throws Exception {
        AuditingAuthenticationFailureHandler handler =
                new AuditingAuthenticationFailureHandler(userRepository, auditLogRepository);

        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("username", "nobody");
        req.setParameter("password", "irrelevant");
        MockHttpServletResponse res = new MockHttpServletResponse();
        AuthenticationException ex = new BadCredentialsException("bad creds");

        handler.onAuthenticationFailure(req, res, ex);

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void onAuthenticationFailure_doesNotEmit_whenUsernameBlank() throws Exception {
        AuditingAuthenticationFailureHandler handler =
                new AuditingAuthenticationFailureHandler(userRepository, auditLogRepository);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setParameter("username", "");
        MockHttpServletResponse res = new MockHttpServletResponse();
        AuthenticationException ex = new BadCredentialsException("bad creds");

        handler.onAuthenticationFailure(req, res, ex);

        verify(auditLogRepository, never()).save(any());
    }
}
