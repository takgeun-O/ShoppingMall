package io.github.takgeun.shop.member;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * 회원 상태나 권한 변경 트랜잭션이
 * 커밋된 뒤 기존 로그인 세션이 실제로 만료되는지 검증
 * <p>
 * 실제 커밋이 발생해야 하기 때문에
 * 테스트에 @Transactional, @Rollback을 붙이지 않음
 *
 * @TransactionalEventListener(AFTER_COMMIT)은 실제 커밋이 발생해야 세션 만료 처리가 실행되기 때문
 */
public class MemberSessionExpirationIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = "pw12341234!";
    private static final String NEW_PASSWORD = "newPassword1234!";

    @Autowired
    private SessionRegistry sessionRegistry;
    @Autowired
    private MemberService memberService;

    /**
     * SessionRegistry는 DB 트랜잭션 롤백 대상이 아닌
     * 메모리 기반 저장소이므로 테스트가 끝난 후 직접 정리해야 한다.
     */
    @AfterEach
    void clearSessionRegistry() {

        sessionRegistry.getAllPrincipals()
                .forEach(principal ->
                        sessionRegistry.getAllSessions(principal, true)
                                .forEach(session ->
                                        sessionRegistry.removeSessionInformation(
                                                session.getSessionId()
                                        )
                                )
                );
    }

    @Test
    void 회원이_비활성화되면_기존_로그인_세션이_만료된다() throws Exception {

        // given
        String email = uniqueEmail("inactive-session");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "비활성화회원",
                "010-1111-2222"
        );

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        SessionInformation sessionInformation = getSessionInformation(session);

        // when
        // 별도 서비스 트랜잭션 실행
        // 트랜잭션 커밋 후 MemberSessionExpirationEvent Listener 동작
        memberService.changeStatus(
                memberId,
                MemberStatus.INACTIVE
        );
        // 여기서 세션이 만료된다.

        // then
        assertThat(sessionInformation.isExpired()).isTrue();

        assertExpiredSessionIsRejected(
                session,
                "/members/me"
        );
    }

    @Test
    void 회원이_탈퇴하면_기존_로그인_세션이_만료된다() throws Exception {

        // given
        String email = uniqueEmail("withdraw-session");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "탈퇴회원",
                "010-2222-3333"
        );

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        SessionInformation sessionInformation = getSessionInformation(session);

        assertThat(sessionInformation.isExpired()).isFalse();

        // when
        memberService.deactivate(memberId); // 이 때 세션 만료

        // then
        assertThat(sessionInformation.isExpired()).isTrue();

        assertExpiredSessionIsRejected(
                session,
                "/members/me"
        );
    }

    @Test
    void 관리자_권한이_회수되면_기존_관리자_세션이_만료된다() throws Exception {

        // given
        String email = uniqueEmail("admin-role-session");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "권한변경관리자",
                "010-1111-2222"
        );

        // 로그인 전에 관리자 권한 부여
        memberService.changeRole(
                memberId,
                MemberRole.ADMIN
        );

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        ShopUserPrincipal principal = getPrincipal(session);

        assertThat(principal.getMemberId())
                .isEqualTo(memberId);

        assertThat(principal.getAuthorities())
                .extracting("authority")
                .contains("ROLE_ADMIN");

        SessionInformation sessionInformation = getSessionInformation(session);

        assertThat(sessionInformation.isExpired()).isFalse();

        // when
        memberService.changeRole(
                memberId,
                MemberRole.USER
        );

        // then
        assertThat(sessionInformation.isExpired()).isTrue();

        assertExpiredSessionIsRejected(
                session,
                "/admin"
        );
    }

    private ShopUserPrincipal getPrincipal(MockHttpSession session) {

        SecurityContext securityContext = getSecurityContext(session);

        assertThat(securityContext.getAuthentication())
                .isNotNull();

        assertThat(securityContext
                .getAuthentication()
                .getPrincipal()
        ).isInstanceOf(ShopUserPrincipal.class);

        return (ShopUserPrincipal) securityContext
                .getAuthentication()
                .getPrincipal();
    }

    private SecurityContext getSecurityContext(MockHttpSession session) {
        Object value = session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );

        assertThat(value)
                .isInstanceOf(SecurityContext.class);

        return (SecurityContext) value;
    }

    private MockHttpSession loginAndGetSession(String email, String password) throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("email", email)
                                .param("password", password)
                )
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result
                .getRequest()
                .getSession(false);

        assertThat(session)
                .as("로그인 성공 후 세션이 생성되어야 한다.")   // 검증문에 설명을 붙이는 메서드
                .isNotNull();

        return session;
    }

    private SessionInformation getSessionInformation(MockHttpSession session) {
        SessionInformation sessionInformation = sessionRegistry.getSessionInformation(session.getId());

        assertThat(sessionInformation)
                .as("로그인 세션이 SessionRegistry에 등록되어야 한다.")
                .isNotNull();

        return sessionInformation;
    }

    /**
     * expireNow()는 HttpSession을 그 자리에서 바로 무효화하지 않고,
     * SessionInformation을 expired 상태로 표시한다.
     *
     * 이후 해당 세션으로 요청할 때 ConcurrentSessionFilter가
     * 1. 만료 상태 확인
     * 2. 로그아웃 처리
     * 3. HttpSession 무효화
     * 4. 로그인 화면으로 리다이렉트
     */
    private void assertExpiredSessionIsRejected(MockHttpSession session, String requestPath) throws Exception {

        mockMvc.perform(
                get(requestPath)
                        .session(session)
        )
                .andExpect(status().is3xxRedirection())

                /**
                 * header().string() : MockMvc 응답의 특정 HTTP 헤더가 예상한 문자열 조건을 만족하는지 검증
                 * 응답의 Location 헤더가 /login으로 시작하는지 확인
                 *
                 * Location 헤더 : 서버가 리다이렉트 응답을 반환할 때 브라우저에 이동할 주소를 알려주는 표준 HTTP 응답 헤더
                 * MockMvc는 브라우저처럼 리다이렉트를 자동으로 따라가지 않기 때문에 Location 헤더를 검사해 이동 목적지를 확인하는 것
                 * 전체 URL을 알아야 하는 redirectUrl()과 달리 정확한 URL을 모르고 /login 로 시작한다는 것만 알아도 됨.
                 *
                 * HTTP/1.1 302 Found
                 * Location: /login?reason=SESSION_EXPIRED
                 */
//                .andExpect(header().string(
//                        "Location",
//                        startsWith("/login")

                // 위 기존 검증은 reason이 다른 응답도 통과시킴
                // 예를 들어 /login?next=/admin&reason=LOGIN_REQUIRED
                // 그냥 세션 만료는 아래와 같이 엄격하게 검증
                .andExpect(redirectedUrl(
                        "/login?reason=SESSION_EXPIRED"
                ));

        assertThat(session.isInvalid()).isTrue();
    }

    private String uniqueEmail(String prefix) {
        return prefix
                + "-"
                + System.nanoTime()
                + "@test.com";
    }
}
