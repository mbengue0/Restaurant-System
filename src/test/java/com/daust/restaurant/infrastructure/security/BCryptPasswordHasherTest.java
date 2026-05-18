package com.daust.restaurant.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void hash_producesBcryptFormattedString() {
        String hashed = hasher.hash("password123");

        assertThat(hashed).startsWith("$2");
        assertThat(hashed).hasSizeGreaterThanOrEqualTo(60);
    }

    @Test
    void matches_returnsTrueForCorrectPassword() {
        String hashed = hasher.hash("password123");

        assertThat(hasher.matches("password123", hashed)).isTrue();
    }

    @Test
    void matches_returnsFalseForWrongPassword() {
        String hashed = hasher.hash("password123");

        assertThat(hasher.matches("wrong-password", hashed)).isFalse();
    }

    @Test
    void hash_isSaltedSoSameInputProducesDifferentOutput() {
        String first = hasher.hash("password123");
        String second = hasher.hash("password123");

        assertThat(first).isNotEqualTo(second);
        assertThat(hasher.matches("password123", first)).isTrue();
        assertThat(hasher.matches("password123", second)).isTrue();
    }
}
