package io.github.takgeun.shop.global.security.handler;

/**
 * | 클래스                            | 처리 상황                    |
 * | ------------------------------ | ------------------------ |
 * | `ApiAuthenticationEntryPoint`  | API 비로그인 접근 → JSON `401` |
 * | `ApiAccessDeniedHandler`       | API 권한 부족 → JSON `403`   |
 * | `ViewAuthenticationEntryPoint` | 화면 비로그인 접근 → 로그인 페이지     |
 * | `ViewAccessDeniedHandler`      | 화면 권한 부족 → `error/403`   |
 */

import io.github.takgeun.shop.global.error.code.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * API 권한 부족 처리기
 * JSON을 직접 작성
 */
@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiSecurityErrorWriter errorWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {

        errorWriter.write(
                response,
                ErrorCode.ACCESS_DENIED,
                request.getRequestURI()
        );
    }
}
