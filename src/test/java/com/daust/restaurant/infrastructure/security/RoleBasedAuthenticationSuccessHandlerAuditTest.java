package com.daust.restaurant.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class RoleBasedAuthenticationSuccessHandlerAuditTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @Test
    void onAuthenticationSuccess_emitsUserLoginAuditEntry() throws Exception {
        RoleBasedAuthenticationSuccessHandler handler =
                new RoleBasedAuthenticationSuccessHandler(userRepository, auditLogRepository);

        User waiter = new User("alice", HASH, "Alice Waiter", Role.WAITER, false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(waiter));

        HttpServletRequest req = new MockHttpServletRequest();
        HttpServletResponse res = new MockHttpServletResponse();
        var auth = new UsernamePasswordAuthenticationToken(
                "alice", "n/a", java.util.List.of(new SimpleGrantedAuthority("ROLE_WAITER")));

        handler.onAuthenticationSuccess(req, res, auth);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntry entry = captor.getValue();
        assertThat(entry.getEventType()).isEqualTo("USER_LOGIN");
        assertThat(entry.getAffectedEntityType()).isEqualTo("User");
        assertThat(entry.getUserId()).isEqualTo(waiter.getId());
        assertThat(entry.getUserRoleAtTime()).isEqualTo(Role.WAITER);
        assertThat(entry.getAfterValue()).contains("username=alice");
    }

    @Test
    void onAuthenticationSuccess_doesNotAudit_whenUserDeactivated() throws Exception {
        RoleBasedAuthenticationSuccessHandler handler =
                new RoleBasedAuthenticationSuccessHandler(userRepository, auditLogRepository);

        User deactivated = new User("ghost", HASH, "Ghost", Role.WAITER, false);
        deactivated.deactivate();
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.of(deactivated));

        var auth = new UsernamePasswordAuthenticationToken(
                "ghost", "n/a", java.util.List.of(new SimpleGrantedAuthority("ROLE_WAITER")));
        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(), new MockHttpServletResponse(), auth);

        verify(auditLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
