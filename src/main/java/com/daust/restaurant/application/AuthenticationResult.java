package com.daust.restaurant.application;

import com.daust.restaurant.domain.Role;
import com.daust.restaurant.domain.UserId;

public sealed interface AuthenticationResult
        permits AuthenticationResult.Success,
                AuthenticationResult.InvalidCredentials,
                AuthenticationResult.AccountDeactivated {

    record Success(UserId userId, Role role, boolean mustChangePassword) implements AuthenticationResult {}

    record InvalidCredentials() implements AuthenticationResult {}

    record AccountDeactivated() implements AuthenticationResult {}
}
