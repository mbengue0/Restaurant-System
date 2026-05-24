package com.daust.restaurant.application;

import com.daust.restaurant.domain.UserId;

/**
 * Returned from {@link UserAccountService#createUser} so the controller can display the
 * one-time temporary password to the admin. The password is never persisted, logged, or sent
 * anywhere else — show once, then it is gone (NFR-SEC-1: passwords are stored as BCrypt hashes
 * only).
 */
public record CreatedUserResult(UserId userId, String temporaryPassword) {}
