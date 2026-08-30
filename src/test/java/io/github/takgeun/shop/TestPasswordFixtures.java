package io.github.takgeun.shop;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class TestPasswordFixtures {
    private static final String RAW_PASSWORD = "test-only-password";
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);

    public static final String BCRYPT_PASSWORD =
            PASSWORD_ENCODER.encode(RAW_PASSWORD);

    private TestPasswordFixtures() {
    }

    public static boolean matchesTestPassword(String encodedPassword) {
        return PASSWORD_ENCODER.matches(RAW_PASSWORD, encodedPassword);
    }
}
