package io.github.takgeun.shop.global.security.handler;

/**
 * | 클래스                            | 처리 상황                    |
 * | ------------------------------ | ------------------------ |
 * | `ApiAuthenticationEntryPoint`  | API 비로그인 접근 → JSON `401` |
 * | `ApiAccessDeniedHandler`       | API 권한 부족 → JSON `403`   |
 * | `ViewAuthenticationEntryPoint` | 화면 비로그인 접근 → 로그인 페이지     |
 * | `ViewAccessDeniedHandler`      | 화면 권한 부족 → `error/403`   |
 */

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 화면 비로그인 처리기
 * 기존 인터셉터의 next, reason 계약 유지
 */
@Component
public class ViewAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        String uri = request.getRequestURI();
        String query = request.getQueryString();

        String next = query == null
                ? uri
                : uri + "?" + query;

        String loginUrl = UriComponentsBuilder
                .fromPath("/login")
                .queryParam("next", next)
                .queryParam("reason", "LOGIN_REQUIRED")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        response.sendRedirect(loginUrl);
    }
}
