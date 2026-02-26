package io.github.takgeun.shop.global.interceptor;

import io.github.takgeun.shop.global.session.SessionConst;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * /orders/** 진입 시 로그인 강제 + next 처리
 */
@Component
public class UserAuthInterceptor implements HandlerInterceptor {

    private static final String LOGIN_PATH = "/login";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        HttpSession session = request.getSession(false);        // 기존 세션 존재 시 반환하고, 존재하지 않으면 null 반환

        // 로그인 여부 체크
        if(session == null || session.getAttribute(SessionConst.LOGIN_MEMBER_ID) == null) {
            redirectToLoginWithNext(request, response);
            return false;
        }

        return true;
    }

    private void redirectToLoginWithNext(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String next = (query == null) ? uri : (uri + "?" + query);

        // 리다이렉트 시 리다이렉트 사유도 넘기기
        String reason = "LOGIN_REQUIRED";

        String loginUrl = UriComponentsBuilder.fromPath(LOGIN_PATH)       // 기본 경로를 /login 으로 시작 (이 때 loginUrl은 '/login')
                .queryParam("next", next)                        // 쿼리 파라미터 추가 ("orders/3" 이면 '/login?orders/3')
                .queryParam("reason", reason)
                .build()                                                // 지금까지 설정한 URI 구성 요소들을 객체로 조립하기 (아직 문자열이 아님)
                .encode(StandardCharsets.UTF_8)                         // URL에 특수문자가 있을 경우 안전하게 인코딩 (/orders/3?sort=price desc 처럼 공백이 들어올 경우 %2Forders%2F3%3Fsort%3Dprice%20desc 으로 인코딩)
                .toUriString(); // 최종 -> /login?next=%2Forders%2F3%3Fsort%3Dprice%20desc

        response.sendRedirect(loginUrl);
    }
}
