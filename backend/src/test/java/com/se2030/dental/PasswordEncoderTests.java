package com.se2030.dental;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTests {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void encode_transformsPlaintextToHash() {
        String plaintext = "SecretPassword123";
        String hash = passwordEncoder.encode(plaintext);

        assertNotNull(hash);
        assertNotEquals(plaintext, hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }

    @Test
    void matches_succeedsWithCorrectPassword() {
        String plaintext = "MySecurePassword!";
        String hash = passwordEncoder.encode(plaintext);

        assertTrue(passwordEncoder.matches(plaintext, hash));
    }

    @Test
    void matches_failsWithIncorrectPassword() {
        String plaintext = "MySecurePassword!";
        String wrongPlaintext = "NotMySecurePassword!";
        String hash = passwordEncoder.encode(plaintext);

        assertFalse(passwordEncoder.matches(wrongPlaintext, hash));
    }
}
