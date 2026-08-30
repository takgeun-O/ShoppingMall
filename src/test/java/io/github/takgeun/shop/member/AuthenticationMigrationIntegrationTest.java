package io.github.takgeun.shop.member;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@Rollback
public class AuthenticationMigrationIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = "pw12341234!";

    /**
     * 기존 UserAuthInterceptor 기준선 즉, 기존 세션 인증 동작을 기준선으로 고정하기 위해 만든 테스트
     *
     * 이후 Spring Security가 보호 경로를 완전히 담당한 이후에는
     * Security의 인증 진입점 테스트로 전환한다.
     */
    @Test
    void 비로그인_사용자가_주문_페이지에_접근하면_로그인_페이지로_리다이렉트된다() throws Exception {

        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?next=/orders&reason=LOGIN_REQUIRED"
                ));
    }

    /**
     * 기존 AdminAuthInterceptor 기준선
     */
    @Test
    void 비로그인_사용자가_관리자_페이지에_접근하면_로그인_페이지로_리다이렉트된다() throws Exception {

        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?next=/admin&reason=LOGIN_REQUIRED"
                ));
    }

    /**
     * 기존 AdminAuthInterceptor의 권한 검사 기준선
     *
     * Security 전환 완료 후에는 ROLE_ADMIN 인가 테스트로 교체한다.
     */
    @Test
    void 일반_회원이_관리자_페이지에_접근하면_403을_반환한다() throws Exception {

        Long memberId = createMember("user");
        Member member = memberService.findById(memberId);

        MockHttpSession session = legacyAuthenticatedSession(member);

        mockMvc.perform(get("/admin")
                .session(session))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"))
                .andExpect(model().attribute("status", 403))
                .andExpect(model().attribute(
                        "message",
                        "관리자 권한이 필요합니다."
                ))
                .andExpect(model().attribute("path", "/admin"));
    }

    /**
     * 기존 AdminAuthInterceptor의 관리자 접근 기준선.
     */
    @Test
    void 활성_관리자는_관리자_페이지에_접근할_수_있다()
            throws Exception {

        Long memberId = createMember("admin");
        memberService.changeRole(memberId, MemberRole.ADMIN);

        Member admin = memberService.findById(memberId);
        MockHttpSession session = legacyAuthenticatedSession(admin);

        mockMvc.perform(get("/admin")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("dashboard"));
    }

    /**
     * 로그인 성공 시 기존 세션 인증 정보가 어떻게 저장되는지 고정한다.
     *
     * Spring Security 전환 후에는 이 세션 속성들이
     * SecurityContext 기반 인증으로 대체될 수 있다.
     */
//    @Test
//    void 로그인에_성공하면_회원정보를_세션에_저장한다()
//            throws Exception {
//
//        String email = uniqueEmail("login");
//        Long memberId = memberService.signup(
//                email,
//                PASSWORD,
//                "로그인회원",
//                "010-1111-2222"
//        );
//
//        mockMvc.perform(post("/login")
//                        .param("email", email)
//                        .param("password", PASSWORD))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/"))
//                .andExpect(flash().attribute(
//                        "success",
//                        "로그인되었습니다."
//                ))
//                .andExpect(request().sessionAttribute(
//                        SessionConst.LOGIN_MEMBER_ID,
//                        memberId
//                ))
//                .andExpect(request().sessionAttribute(
//                        SessionConst.LOGIN_ROLE,
//                        MemberRole.USER
//                ))
//                .andExpect(request().sessionAttribute(
//                        SessionConst.LOGIN_MEMBER_NAME,
//                        "로그인회원"
//                ));
//    }
    /**
     * 마이그레이션 단계의 로그인 성공 결과
     *
     * 신규 Spring Security 인증 정보와 기존 Interceptor 호환용 세션 속성이 한 세션에 함꼐 저장되는지 검증
     *
     * 전체 흐름
     *      * POST /login
     *      * -> AuthenticationManager가 인증
     *      * -> DaoAuthenticationProvider가 회원, 비밀번호, 상태 검사
     *      * -> 인증 성공 Authentication 생성
     *      * -> SecurityContext에 Authentication 저장
     *      * -> SecurityContext를 HttpSession에 저장
     *      * -> 기존 인터셉터용 세션 정보도 저장
     *      * -> 테스트에서 두 인증 정보 모두 검증
     */
    @Test
    void 로그인에_성공하면_SecurityContext와_기존_호환_세션정보를_저장한다() throws Exception {

        // given
        String email = uniqueEmail("login");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "로그인회원",
                "010-1111-2222"
        );

        // when
        MvcResult result = performSuccessfulLogin(
                email,
                PASSWORD,
                null,
                "/"
        );

        /**
         * 여기서 사용되는 HttpSession의 구조
         * HttpSession
         * ├── SPRING_SECURITY_CONTEXT
         * │   └── SecurityContext
         * │       └── Authentication
         * │           └── ShopUserPrincipal
         * ├── loginMemberId
         * ├── loginRole
         * └── loginMemberName
         */
        MockHttpSession session = getSession(result);

        // then: Spring Security 인증 정보
        SecurityContext securityContext = getSecurityContext(session);

        Authentication authentication = securityContext.getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated())
                .isTrue();

        assertThat(authentication.getPrincipal())
                .isInstanceOf(ShopUserPrincipal.class);

        ShopUserPrincipal principal = (ShopUserPrincipal) authentication.getPrincipal();

        assertThat(principal.getMemberId())
                .isEqualTo(memberId);

        assertThat(principal.getUsername())
                .isEqualTo(email);

        assertThat(principal.getName())
                .isEqualTo("로그인회원");

        assertThat(principal.getRole())
                .isEqualTo(MemberRole.USER);

        assertThat(principal.getAuthorities())
                .extracting(
                        GrantedAuthority::getAuthority
                )
                .containsExactly("ROLE_USER");

        // then: 기존 Interceptor 호환 세션 정보
        assertLegacySession(
                session,
                memberId,
                MemberRole.USER,
                "로그인회원"
        );
    }

    /**
     * 인증 필터/인터셉터에서 전달한 next 경로 복귀 기준선
     */
    @Test
    void 로그인에_성공하면_안전한_next_경로로_리다이렉트된다()
            throws Exception {

        String email = uniqueEmail("next");

        memberService.signup(
                email,
                PASSWORD,
                "복귀경로회원",
                "010-2222-3333"
        );

        performSuccessfulLogin(
                email,
                PASSWORD,
                "/products/1?categoryId=10&sort=latest",
                "/products/1?categoryId=10&sort=latest"
        );
    }

    /**
     * 외부 URL로 리다이렉트되는 Open Redirect 방지 기준선
     */
    @Test
    void 외부_URL이_next로_전달되면_기본_페이지로_이동한다()
            throws Exception {

        String email = uniqueEmail("unsafe-next");

        memberService.signup(
                email,
                PASSWORD,
                "외부경로회원",
                "010-3333-4444"
        );

        performSuccessfulLogin(
                email,
                PASSWORD,
                "https://evil.example.com",
                "/"
        );
    }

    /**
     * //로 시작하는 protocol-relative URL도 외부 URL로
     * 해석될 수 있으므로 차단한다.
     */
    @Test
    void 이중_슬래시_URL이_next로_전달되면_기본_페이지로_이동한다()
            throws Exception {

        String email =
                uniqueEmail("double-slash-next");

        memberService.signup(
                email,
                PASSWORD,
                "이중슬래시회원",
                "010-3333-5555"
        );

        performSuccessfulLogin(
                email,
                PASSWORD,
                "//evil.example.com",
                "/"
        );
    }

    /**
     * 비밀번호 불일치는 Spring Security의
     * AuthenticationException 계열로 처리한다.
     */
    @Test
    void 비밀번호가_일치하지_않으면_로그인_폼을_다시_보여주고_인증정보를_저장하지_않는다()
            throws Exception {

        String email = uniqueEmail("wrong-password");

        memberService.signup(
                email,
                PASSWORD,
                "비밀번호실패회원",
                "010-4444-5555"
        );

        mockMvc.perform(post("/login")
                        .param("email", email)
                        .param("password", "wrong-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeHasErrors("form"))
                .andExpect(request().sessionAttributeDoesNotExist(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        SessionConst.LOGIN_MEMBER_ID,
                        SessionConst.LOGIN_ROLE,
                        SessionConst.LOGIN_MEMBER_NAME
                ));
    }

    /**
     * 존재하지 않는 이메일도 비밀번호 불일치와 같은 공개 메시지로 처리한다.
     */
    @Test
    void 존재하지_않는_이메일이면_로그인_폼을_다시_보여주고_인증정보를_저장하지_않는다()
            throws Exception {

        mockMvc.perform(post("/login")
                        .param(
                                "email",
                                uniqueEmail("unknown")
                        )
                        .param(
                                "password",
                                PASSWORD
                        ))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "auth/login"
                ))
                .andExpect(model().attributeHasErrors(
                        "form"
                ))
                .andExpect(request().sessionAttributeDoesNotExist(
                        HttpSessionSecurityContextRepository
                                .SPRING_SECURITY_CONTEXT_KEY,
                        SessionConst.LOGIN_MEMBER_ID,
                        SessionConst.LOGIN_ROLE,
                        SessionConst.LOGIN_MEMBER_NAME
                ));
    }

    /**
     * 비활성 회원은 비밀번호가 맞더라도 로그인할 수 없다.
     */
    @Test
    void 비활성_회원은_로그인할_수_없고_인증정보도_저장되지_않는다()
            throws Exception {

        String email = uniqueEmail("inactive");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "비활성회원",
                "010-5555-6666"
        );

        memberService.deactivate(memberId);

        mockMvc.perform(post("/login")
                        .param("email", email)
                        .param("password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeHasErrors("form"))
                .andExpect(request().sessionAttributeDoesNotExist(
                        HttpSessionSecurityContextRepository
                                .SPRING_SECURITY_CONTEXT_KEY,
                        SessionConst.LOGIN_MEMBER_ID,
                        SessionConst.LOGIN_ROLE,
                        SessionConst.LOGIN_MEMBER_NAME
                ));
    }

    /**
     * 실제 로그인으로 생성한 세션을 로그아웃 요청에 전달한다.
     *
     * 현재 구현은 세션 전체를 무효화하며,
     * Security logout으로 완전히 전환할 때 다시 조정한다.
     */
    @Test
    void 로그아웃하면_SecurityContext가_저장된_세션을_무효화한다()
            throws Exception {

        String email =
                uniqueEmail("logout");

        memberService.signup(
                email,
                PASSWORD,
                "로그아웃회원",
                "010-6666-7777"
        );

        MvcResult loginResult =
                performSuccessfulLogin(
                        email,
                        PASSWORD,
                        null,
                        "/"
                );

        MockHttpSession loginSession =
                getSession(loginResult);

        assertThat(getSecurityContext(loginSession))
                .isNotNull();

        mockMvc.perform(post("/logout")
                        .session(loginSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute(
                        "success",
                        "로그아웃되었습니다."
                ));

        assertThat(loginSession.isInvalid())
                .isTrue();
    }

    /**
     * 기존에 AdminAuthInterceptor가 DB 상태를 다시 확인하는지 보는 기준선
     *
     * 세션에는 관리자 정보가 남아 있어도 DB에서 비활성화되었다면
     * 세션을 폐기하고 로그인 페이지로 이동해야 한다.
     *
     * Security 전환 완료 후에는 Principal 갱신 및 비활성화 정책 테스트로 교체한다.
     */
    @Test
    void 로그인_후_비활성화된_관리자가_접근하면_세션을_무효화한다()
            throws Exception {

        Long memberId = createMember("inactive-admin");

        memberService.changeRole(memberId, MemberRole.ADMIN);

        Member admin = memberService.findById(memberId);

        MockHttpSession session = legacyAuthenticatedSession(admin);

        memberService.deactivate(memberId);

        mockMvc.perform(get("/admin")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?next=/admin&reason=INACTIVE_ACCOUNT"
                ));

        assertThat(session.isInvalid()).isTrue();
    }

    private Long createMember(String prefix) {
        return memberService.signup(
                uniqueEmail(prefix),
                PASSWORD,
                prefix + "회원",
                "010-1234-5678"
        );
    }

    private String uniqueEmail(String prefix) {
        return prefix + System.nanoTime() + "@test.com";
    }

    private MockHttpSession legacyAuthenticatedSession(Member member) {
        MockHttpSession session = new MockHttpSession();

        session.setAttribute(
                SessionConst.LOGIN_MEMBER_ID,
                member.getId()
        );
        session.setAttribute(
                SessionConst.LOGIN_ROLE,
                member.getRole()
        );
        session.setAttribute(
                SessionConst.LOGIN_MEMBER_NAME,
                member.getName()
        );

        return session;
    }

    private MvcResult performSuccessfulLogin(
            String email,
            String password,
            String next,
            String expectedRedirect
    ) throws Exception {

        var requestBuilder = post("/login")
                .param("email", email)
                .param("password", password);

        if(next != null) {
            requestBuilder.param("next", next);
        }

        return mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedRedirect))
                .andExpect(flash().attribute(
                        "success",
                        "로그인되었습니다."
                ))
                .andReturn();
    }

    private MockHttpSession getSession(MvcResult result) {
        Object session = result.getRequest().getSession(false);

        assertThat(session)
                .isInstanceOf(MockHttpSession.class);

        return (MockHttpSession) session;
    }

    private SecurityContext getSecurityContext(MockHttpSession session) {
        Object value = session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );

        assertThat(value)
                .isInstanceOf(SecurityContext.class);

        return (SecurityContext) value;
    }

    private void assertLegacySession(MockHttpSession session, Long memberId, MemberRole role, String memberName) {
        assertThat(session.getAttribute(
                SessionConst.LOGIN_MEMBER_ID
        )).isEqualTo(memberId);

        assertThat(session.getAttribute(
                SessionConst.LOGIN_ROLE
        )).isEqualTo(role);

        assertThat(session.getAttribute(
                SessionConst.LOGIN_MEMBER_NAME
        )).isEqualTo(memberName);
    }
}
