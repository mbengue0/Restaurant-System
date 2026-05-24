package com.daust.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daust.restaurant.domain.AuditLogEntry;
import com.daust.restaurant.domain.AuditLogRepository;
import com.daust.restaurant.domain.PasswordHasher;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private AuditLogRepository auditLogRepository;

    private UserAccountService userAccountService;

    @BeforeEach
    void setUp() {
        userAccountService =
                new UserAccountService(userRepository, passwordHasher, auditLogRepository);
    }

    @Test
    void createUser_hashesPassword_setsMustChangePassword_andReturnsTempPassword() {
        User admin = newAdmin();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordHasher.hash(anyString())).thenReturn(HASH);

        CreatedUserResult result = userAccountService.createUser(
                "alice", "Alice Diop", Role.WAITER, admin.getId());

        assertThat(result.userId()).isNotNull();
        assertThat(result.temporaryPassword()).isNotBlank();
        assertThat(result.temporaryPassword().length()).isEqualTo(12);

        // The exact temp password generated is what gets hashed.
        verify(passwordHasher).hash(result.temporaryPassword());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getFullName()).isEqualTo("Alice Diop");
        assertThat(saved.getRole()).isEqualTo(Role.WAITER);
        assertThat(saved.getHashedPassword()).isEqualTo(HASH);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.isMustChangePassword()).isTrue();

        ArgumentCaptor<AuditLogEntry> entryCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(entryCaptor.capture());
        AuditLogEntry entry = entryCaptor.getValue();
        assertThat(entry.getEventType()).isEqualTo("USER_CREATED");
        assertThat(entry.getAfterValue()).contains("username=alice", "role=WAITER");
        // NFR-SEC-1: temp password must never appear in audit.
        assertThat(entry.getAfterValue()).doesNotContain(result.temporaryPassword());
    }

    @Test
    void createUser_throwsUsernameTaken_whenUsernameExists() {
        User admin = newAdmin();
        User existing = new User("alice", HASH, "Old Alice", Role.WAITER, false);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userAccountService.createUser(
                        "alice", "New Alice", Role.WAITER, admin.getId()))
                .isInstanceOf(UsernameTakenException.class)
                .hasMessageContaining("alice");

        verify(userRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
        verify(passwordHasher, never()).hash(anyString());
    }

    @Test
    void updateUser_changesFullNameAndRole_andAudits() {
        User admin = newAdmin();
        User target = new User("bob", HASH, "Bob Sow", Role.WAITER, false);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        userAccountService.updateUser(target.getId(), "Robert Sow", Role.MANAGER, admin.getId());

        assertThat(target.getFullName()).isEqualTo("Robert Sow");
        assertThat(target.getRole()).isEqualTo(Role.MANAGER);
        verify(userRepository).save(target);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntry entry = captor.getValue();
        assertThat(entry.getEventType()).isEqualTo("USER_UPDATED");
        assertThat(entry.getBeforeValue()).contains("fullName=Bob Sow", "role=WAITER");
        assertThat(entry.getAfterValue()).contains("fullName=Robert Sow", "role=MANAGER");
    }

    @Test
    void deactivateUser_throwsLastActiveAdmin_whenTargetIsOnlyAdmin() {
        User onlyAdmin = newAdmin();
        when(userRepository.findById(onlyAdmin.getId())).thenReturn(Optional.of(onlyAdmin));
        when(userRepository.findActiveByRole(Role.ADMIN)).thenReturn(List.of(onlyAdmin));

        assertThatThrownBy(() -> userAccountService.deactivateUser(
                        onlyAdmin.getId(), onlyAdmin.getId()))
                .isInstanceOf(LastActiveAdminException.class)
                .hasMessageContaining("FR14");

        assertThat(onlyAdmin.isActive()).isTrue();
        verify(userRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void deactivateUser_succeeds_whenAnotherActiveAdminExists() {
        User actingAdmin = newAdmin("root", "Root Admin");
        User otherAdmin = newAdmin("second", "Second Admin");
        when(userRepository.findById(actingAdmin.getId())).thenReturn(Optional.of(actingAdmin));
        when(userRepository.findById(otherAdmin.getId())).thenReturn(Optional.of(otherAdmin));
        when(userRepository.findActiveByRole(Role.ADMIN))
                .thenReturn(List.of(actingAdmin, otherAdmin));

        userAccountService.deactivateUser(otherAdmin.getId(), actingAdmin.getId());

        assertThat(otherAdmin.isActive()).isFalse();
        verify(userRepository).save(otherAdmin);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("USER_DEACTIVATED");
    }

    @Test
    void deactivateUser_succeedsImmediately_whenTargetIsNotAdmin() {
        User admin = newAdmin();
        User waiter = new User("alice", HASH, "Alice", Role.WAITER, false);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(waiter.getId())).thenReturn(Optional.of(waiter));

        userAccountService.deactivateUser(waiter.getId(), admin.getId());

        assertThat(waiter.isActive()).isFalse();
        verify(userRepository).save(waiter);
        // Non-Admin path must not query the last-admin set.
        verify(userRepository, never()).findActiveByRole(any());
    }

    @Test
    void resetPassword_setsNewHash_forcesMustChangePassword_andReturnsTempPassword() {
        User admin = newAdmin();
        User target = new User("alice", HASH, "Alice", Role.WAITER, false);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(passwordHasher.hash(anyString())).thenReturn("$2a$10$newAdminResetHash..");

        String returned = userAccountService.resetPassword(target.getId(), admin.getId());

        assertThat(returned).isNotBlank().hasSize(12);
        verify(passwordHasher).hash(returned);
        assertThat(target.getHashedPassword()).isEqualTo("$2a$10$newAdminResetHash..");
        assertThat(target.isMustChangePassword()).isTrue();
        verify(userRepository).save(target);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLogEntry entry = captor.getValue();
        assertThat(entry.getEventType()).isEqualTo("USER_PASSWORD_RESET");
        // Plaintext never appears in audit.
        assertThat(entry.getAfterValue()).doesNotContain(returned);
        assertThat(entry.getAfterValue()).contains("mustChangePassword=true");
    }

    private static User newAdmin() {
        return newAdmin("root", "Root Admin");
    }

    private static User newAdmin(String username, String fullName) {
        return new User(username, HASH, fullName, Role.ADMIN, false);
    }
}
