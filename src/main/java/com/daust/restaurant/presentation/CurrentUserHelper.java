package com.daust.restaurant.presentation;

import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;
import com.daust.restaurant.domain.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserHelper {

    private final UserRepository userRepository;

    public CurrentUserHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User loadCurrent(Authentication authentication) {
        return userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in repository: " + authentication.getName()));
    }

    public UserId currentUserId(Authentication authentication) {
        return loadCurrent(authentication).getId();
    }
}
