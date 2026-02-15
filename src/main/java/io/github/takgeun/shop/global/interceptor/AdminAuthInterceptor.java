package io.github.takgeun.shop.global.interceptor;

import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.MemberRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final String LOGIN_PATH = "/login";
    private static final String FORBIDDEN_PATH = "/forbidden";
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        // 로그인 여부 체크
        if(session == null || session.getAttribute(SessionConst.LOGIN_MEMBER_ID) == null) {
            redirectToLogin(request, response);
            return false;
        }

        // 관리자 권한 체크 (role을 세션에 따로 저장)
        Object roleObj = session.getAttribute(SessionConst.LOGIN_ROLE);
        if(!(roleObj instanceof MemberRole role) || role != MemberRole.ADMIN) {
            redirectToForbidden(request, response);
            return false;
        }

        return true;
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 로그인 후 원래 페이지로 돌아오도록 next 파라미터 붙이기
        String uri = request.getRequestURI();
        String query = request.getQueryString();    // a=1&b=2
        String next = (query == null) ? uri : (uri + "?" + query);

        String redirectUrl = UriComponentsBuilder.fromPath(LOGIN_PATH)
                .queryParam("next", next)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private void redirectToForbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(FORBIDDEN_PATH);
    }
}
