package com.daust.restaurant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daust.restaurant.domain.PasswordHasher;
import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final String HASH = "$2a$10$dummyHashForTestsNotRealBcrypt..............";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void authenticate_returnsSuccess_whenCredentialsAreValid() {
        User user = new User("alice", HASH, "Alice Diop", Role.WAITER, false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("secret", HASH)).thenReturn(true);

        AuthenticationResult result = authenticationService.authenticate("alice", "secret");

        assertThat(result).isInstanceOf(AuthenticationResult.Success.class);
        AuthenticationResult.Success success = (AuthenticationResult.Success) result;
        assertThat(success.userId()).isEqualTo(user.getId());
        assertThat(success.role()).isEqualTo(Role.WAITER);
        assertThat(success.mustChangePassword()).isFalse();
    }

    @Test
    void authenticate_returnsInvalidCredentials_whenUsernameNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        AuthenticationResult result = authenticationService.authenticate("ghost", "whatever");

        assertThat(result).isInstanceOf(AuthenticationResult.InvalidCredentials.class);
        verify(passwordHasher, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_returnsInvalidCredentials_whenPasswordWrong() {
        User user = new User("alice", HASH, "Alice", Role.WAITER, false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong", HASH)).thenReturn(false);

        AuthenticationResult result = authenticationService.authenticate("alice", "wrong");

        assertThat(result).isInstanceOf(AuthenticationResult.InvalidCredentials.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_returnsAccountDeactivated_whenUserInactive_andDoesNotCheckPassword() {
        User user = new User("alice", HASH, "Alice", Role.WAITER, false);
        user.deactivate();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        AuthenticationResult result = authenticationService.authenticate("alice", "secret");

        assertThat(result).isInstanceOf(AuthenticationResult.AccountDeactivated.class);
        verify(passwordHasher, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_recordsLastLoginAndSaves_onSuccess() {
        User user = new User("alice", HASH, "Alice", Role.MANAGER, false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("secret", HASH)).thenReturn(true);

        authenticationService.authenticate("alice", "secret");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getLastLoginAt()).isNotNull();
    }

    @Test
    void authenticate_propagatesMustChangePasswordFlag() {
        User user = new User("alice", HASH, "Alice", Role.ADMIN, true);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordHasher.matches(eq("secret"), eq(HASH))).thenReturn(true);

        AuthenticationResult result = authenticationService.authenticate("alice", "secret");

        assertThat(result).isInstanceOf(AuthenticationResult.Success.class);
        AuthenticationResult.Success success = (AuthenticationResult.Success) result;
        assertThat(success.mustChangePassword()).isTrue();
        assertThat(success.role()).isEqualTo(Role.ADMIN);
    }
}
