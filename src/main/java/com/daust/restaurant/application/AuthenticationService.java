package com.daust.restaurant.application;

import com.daust.restaurant.domain.PasswordHasher;
import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthenticationService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public AuthenticationResult authenticate(String username, String rawPassword) {
        Optional<User> maybeUser = userRepository.findByUsername(username);
        if (maybeUser.isEmpty()) {
            return new AuthenticationResult.InvalidCredentials();
        }

        User user = maybeUser.get();
        if (!user.isActive()) {
            return new AuthenticationResult.AccountDeactivated();
        }

        if (!passwordHasher.matches(rawPassword, user.getHashedPassword())) {
            return new AuthenticationResult.InvalidCredentials();
        }

        user.recordLogin(Instant.now());
        userRepository.save(user);

        return new AuthenticationResult.Success(user.getId(), user.getRole(), user.isMustChangePassword());
    }
}
