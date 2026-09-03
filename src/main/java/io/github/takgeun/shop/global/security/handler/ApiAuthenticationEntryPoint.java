package io.github.takgeun.shop.global.security.handler;

import io.github.takgeun.shop.global.error.code.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * | 클래스                            | 처리 상황                    |
 * | ------------------------------ | ------------------------ |
 * | `ApiAuthenticationEntryPoint`  | API 비로그인 접근 → JSON `401` |
 * | `ApiAccessDeniedHandler`       | API 권한 부족 → JSON `403`   |
 * | `ViewAuthenticationEntryPoint` | 화면 비로그인 접근 → 로그인 페이지     |
 * | `ViewAccessDeniedHandler`      | 화면 권한 부족 → `error/403`   |
 */

/**
 * API 비로그인 처리기
 */
@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiSecurityErrorWriter errorWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        /**
         * {
         *   "status": 401,
         *   "code": "AUTHENTICATION_REQUIRED",
         *   "message": "로그인이 필요합니다.",
         *   "path": "/api/v1/admin/products",
         *   "fieldErrors": []
         * }
         */
        errorWriter.write(
                response,
                ErrorCode.AUTHENTICATION_REQUIRED,
                request.getRequestURI()
        );
    }
}
