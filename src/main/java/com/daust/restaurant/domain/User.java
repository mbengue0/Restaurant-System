package com.daust.restaurant.domain;

import java.time.Instant;
import java.util.Objects;

public class User {

    private final UserId id;
    private final String username;
    private String hashedPassword;
    private String fullName;
    private Role role;
    private boolean active;
    private final Instant createdAt;
    private Instant lastLoginAt;
    private boolean mustChangePassword;

    public User(
            String username,
            String hashedPassword,
            String fullName,
            Role role,
            boolean mustChangePassword) {
        this.id = UserId.generate();
        this.username = normalizeUsername(username);
        this.hashedPassword = requireNonBlankHash(hashedPassword);
        this.fullName = normalizeFullName(fullName);
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.active = true;
        this.createdAt = Instant.now();
        this.lastLoginAt = null;
        this.mustChangePassword = mustChangePassword;
    }

    public static User reconstitute(
            UserId id,
            String username,
            String hashedPassword,
            String fullName,
            Role role,
            boolean active,
            Instant createdAt,
            Instant lastLoginAt,
            boolean mustChangePassword) {
        return new User(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(username, "username"),
                Objects.requireNonNull(hashedPassword, "hashedPassword"),
                Objects.requireNonNull(fullName, "fullName"),
                Objects.requireNonNull(role, "role"),
                active,
                Objects.requireNonNull(createdAt, "createdAt"),
                lastLoginAt,
                mustChangePassword);
    }

    private User(
            UserId id,
            String username,
            String hashedPassword,
            String fullName,
            Role role,
            boolean active,
            Instant createdAt,
            Instant lastLoginAt,
            boolean mustChangePassword) {
        this.id = id;
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.mustChangePassword = mustChangePassword;
    }

    public void changeHashedPassword(String newHashedPassword) {
        this.hashedPassword = requireNonBlankHash(newHashedPassword);
        this.mustChangePassword = false;
    }

    /**
     * Admin-initiated password reset (UC17d). Sets the new hash and forces the user to change it
     * on next login (NFR-SEC-2). Distinct from {@link #changeHashedPassword(String)}, which the
     * user themselves invokes after entering the temp password.
     */
    public void resetPasswordByAdmin(String newHashedPassword) {
        if (!this.active) {
            throw new IllegalStateException("Cannot reset password for inactive user");
        }
        this.hashedPassword = requireNonBlankHash(newHashedPassword);
        this.mustChangePassword = true;
    }

    public void updateFullName(String newFullName) {
        this.fullName = normalizeFullName(newFullName);
    }

    public void updateRole(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "role must not be null");
    }

    public void deactivate() {
        this.active = false;
    }

    public void recordLogin(Instant when) {
        Objects.requireNonNull(when, "when must not be null");
        if (!this.active) {
            throw new IllegalStateException("Cannot record login for inactive user");
        }
        this.lastLoginAt = when;
    }

    public UserId getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    private static String normalizeUsername(String value) {
        Objects.requireNonNull(value, "username must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (trimmed.length() > 50) {
            throw new IllegalArgumentException("username must be 50 characters or fewer");
        }
        return trimmed;
    }

    private static String normalizeFullName(String value) {
        Objects.requireNonNull(value, "fullName must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("fullName must not be blank");
        }
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("fullName must be 100 characters or fewer");
        }
        return trimmed;
    }

    private static String requireNonBlankHash(String value) {
        Objects.requireNonNull(value, "hashedPassword must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("hashedPassword must not be blank");
        }
        return value;
    }
}
