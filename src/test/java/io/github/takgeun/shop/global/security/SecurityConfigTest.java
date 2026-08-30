package io.github.takgeun.shop.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void BCrypt_PasswordEncoder를_제공한다() {
        PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();
        String rawPassword = "test-only-password";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertInstanceOf(BCryptPasswordEncoder.class, passwordEncoder);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }
}
