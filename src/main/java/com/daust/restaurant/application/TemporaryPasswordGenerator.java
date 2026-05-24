package com.daust.restaurant.application;

import java.security.SecureRandom;

/**
 * Generates 12-character alphanumeric temporary passwords with at least one letter and one digit.
 * The character set deliberately excludes ambiguous glyphs (0/O, 1/l/I) so admins can read the
 * value back to a user over voice without confusion.
 */
final class TemporaryPasswordGenerator {

    private static final char[] LETTERS =
            "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final char[] DIGITS = "23456789".toCharArray();
    private static final char[] ALPHABET = (new String(LETTERS) + new String(DIGITS)).toCharArray();
    private static final int LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    String generate() {
        char[] out = new char[LENGTH];
        // Seed: one guaranteed letter + one guaranteed digit, then fill, then shuffle.
        out[0] = LETTERS[random.nextInt(LETTERS.length)];
        out[1] = DIGITS[random.nextInt(DIGITS.length)];
        for (int i = 2; i < LENGTH; i++) {
            out[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        for (int i = LENGTH - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = out[i];
            out[i] = out[j];
            out[j] = tmp;
        }
        return new String(out);
    }
}
