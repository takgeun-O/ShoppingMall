package io.github.takgeun.shop.global.interceptor;

import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final String LOGIN_PATH = "/login";
    private static final String FORBIDDEN_PATH = "/forbidden";

    private final MemberService memberService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession(false);

        // 로그인 여부 체크
        if(session == null || session.getAttribute(SessionConst.LOGIN_MEMBER_ID) == null) {
            redirectToLoginWithNext(request, response, "LOGIN_REQUIRED");
            return false;
        }

        // 로그인은 되었는데 세션의 회원 아이디가 비정상일 때
        Object loginMemberIdObj = session.getAttribute(SessionConst.LOGIN_MEMBER_ID);
        if(!(loginMemberIdObj instanceof Long memberId)) {
            session.invalidate();   // 즉시 로그아웃 처리
            redirectToLoginWithNext(request, response, "LOGIN_REQUIRED");
            return false;
        }

        // 관리자 권한 체크 (role을 세션에 따로 저장)
        Object roleObj = session.getAttribute(SessionConst.LOGIN_ROLE);
        if(!(roleObj instanceof MemberRole role) || role != MemberRole.ADMIN) {
            redirectToForbidden(response);
            return false;
        }

        // DB 기준 현재 회원 상태 재확인
        Member loginMember = memberService.findById(memberId);
        if(loginMember.getStatus() != MemberStatus.ACTIVE) {
            session.invalidate();   // 즉시 로그아웃 처리
            redirectToLoginWithNext(request, response, "INACTIVE_ACCOUNT");
            return false;
        }

        return true;
    }

    private void redirectToLoginWithNext(HttpServletRequest request,
                                         HttpServletResponse response,
                                         String reason) throws IOException {

        // next 경로 조립하기. (uri + query 활용)
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String next = (query == null) ? uri : (uri + "?" + query);  // query가 null이면 문자열에 그냥 null이 붙어버림..

        String loginUrl = UriComponentsBuilder.fromPath(LOGIN_PATH)       // 기본 경로를 /login 으로 시작 (이 때 loginUrl은 '/login')
                .queryParam("next", next)                        // 쿼리 파라미터 추가 ("orders/3" 이면 '/login?orders/3')
                .queryParam("reason", reason)
                .build()                                                // 지금까지 설정한 URI 구성 요소들을 객체로 조립하기 (아직 문자열이 아님)
                .encode(StandardCharsets.UTF_8)                         // URL에 특수문자가 있을 경우 안전하게 인코딩 (/orders/3?sort=price desc 처럼 공백이 들어올 경우 %2Forders%2F3%3Fsort%3Dprice%20desc 으로 인코딩)
                .toUriString(); // 최종 -> /login?next=%2Forders%2F3%3Fsort%3Dprice%20desc

        response.sendRedirect(loginUrl);
    }

    private void redirectToForbidden(HttpServletResponse response) throws IOException {
        response.sendRedirect(FORBIDDEN_PATH);
    }
}
