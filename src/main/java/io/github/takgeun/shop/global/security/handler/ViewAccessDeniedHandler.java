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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 화면 권한 부족 처리기
 *
 * Security 필터에서 발생한 예외는 ViewGlobalExceptionHandler가 처리하지 못함.
 * ViewGlobalExceptionHandler는 Spring MVC의 DispatcherServlet이 처리 중인 예외만 전달 받는 객체임.
 * 즉, 필터에서 발생한 예외는 Spring MVC의 예외 처리 범위 바깥이기 때문에 처리 못하는 것.
 *
 * 전체 요청 흐름)
 * 클라이언트 요청
 *     ↓
 * Servlet Filter
 *     ↓
 * Spring Security FilterChain
 *     ↓
 * DispatcherServlet    <-- 여기부터가 MVC 영역
 *     ↓
 * Controller
 *     ↓
 * Service
 *
 * 화면용 처리기는 오류 페이지로 포워드
 */
@Component
public class ViewAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        request.setAttribute(
                "securityErrorMessage",
                "접근 권한이 없습니다."
        );
        request.setAttribute(
                "securityErrorPath",
                request.getRequestURI()
        );

        request.getRequestDispatcher("/security/forbidden")
                .forward(request, response);
    }
}
