package io.github.takgeun.shop.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // TODO(security-migration): SecurityContext 전환 후 CSRF, logout, URL 권한 규칙을 활성화한다.
                // 기존 세션/Interceptor 기반 POST 화면 흐름을 Security 전환 중에도 유지한다.
                .csrf(csrf -> csrf.disable())
                // 기존 AuthViewController가 세션 무효화와 성공 메시지를 계속 담당한다.
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/login",
                                "/signup",
                                "/products/**",
                                "/api/v1/categories/**",
                                "/api/v1/products/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/assets/**",
                                "/webjars/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/error",
                                "/favicon.ico"
                        ).permitAll()
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
