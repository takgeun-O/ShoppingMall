package io.github.takgeun.shop.member;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.global.security.session.MemberSessionService;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@Rollback
class AuthenticationMigrationIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = "pw12341234!";

    @Autowired
    private SessionRegistry sessionRegistry;
    @Autowired
    private MemberSessionService memberSessionService;

    /**
     * 비로그인 사용자가 인증이 필요한 화면에 접근하면
     * ViewAuthenticationEntryPoint가 로그인 화면으로 리다이렉트한다.
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
     * 비로그인 사용자가 관리자 화면에 접근하면
     * ViewAuthenticationEntryPoint가 로그인 화면으로 리다이렉트한다.
     */
    @Test
    void 비로그인_사용자가_관리자_페이지에_접근하면_로그인_페이지로_리다이렉트된다() throws Exception {

        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?next=/admin&reason=LOGIN_REQUIRED"
                ));
    }

    @Test
    void 비로그인_사용자가_관리자_API에_접근하면_JSON_401을_반환한다()
            throws Exception {

        /**
         * MockMvc가 요청 생성 (별도의 로그인 세션이나 인증 정보 안 넣었으니 비로그인 요청)
         * -> Spring Security 필터 체인 진입 (이 시점에 아직 Controller 매핑 찾지 않음)
         * -> 인증 정보 확인
         *      Spring Security는 현재 요청의 SecurityContext에서 Authentication을 확인함
         *      -> 테스트 요청에 로그인 세션이 없음
         *      -> 따라서 권한 조건을 만족하지 않음. (ROLE_ADMIN 불만족)
         * -> hasRole("ADMIN") 검사 실패
         * -> Spring Security 내부에서 AccessDeniedException 발생
         * -> ExceptionTranslationFilter가 AccessDeniedException 예외를 처리한다.
         *      - ExceptionTranslationFilter가 현재 사용자의 로그인 상태를 확인한다
         *          - 비로그인 사용자라면 -> 인증이 필요 -> AuthenticationEntryPoint 실행
         *          - 로그인했지만 권한이 부족하면 -> AccessDeniedHandler 실행
         * -> /api/** 요청으로 들어왔으니 SecurityCOnfig에서 API용 EntryPoint 선택
         *      - 만약 요청이 /admin이었다면 기본 처리기인 viewAuthenticationEntryPoint가 선택됐을 것.
         */
        mockMvc.perform(get("/api/v1/admin/test"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status")
                        .value(401))
                .andExpect(jsonPath("$.code")
                        .value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message")
                        .value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/admin/test"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isEmpty());
    }

    /**
     * ROLE_ADMIN이 없는 인증 사용자가 관리자 화면에 접근하면
     * ViewAccessDeniedHandler가 403 오류 화면 경로로 포워드한다.
     */
    @Test
    void 일반_회원이_관리자_페이지에_접근하면_403을_반환한다() throws Exception {

        /**
         * MockMvc
         *   → GET /admin 요청
         *
         * Spring Security FilterChain
         *   → /admin/** 규칙 확인
         *   → hasRole("ADMIN") 검사
         *   → 로그인은 했지만 ROLE_ADMIN 없음
         *   → AccessDeniedException 발생
         *
         * ExceptionTranslationFilter
         *   → 사용자가 인증된 상태인지 확인
         *   → 인증은 되었으나 권한 부족
         *   → ViewAccessDeniedHandler 실행
         *
         * ViewAccessDeniedHandler
         *   → HTTP 상태를 403으로 설정
         *   → 현재 HttpServletRequest에 오류 정보 저장
         *       securityErrorMessage
         *       securityErrorPath
         *   → /security/forbidden으로 내부 forward 요청
         *
         *   이 시점에서 첫 번째 MockMvc.perform()이 관찰한 결과는 아래와 같다.
         *   status       = 403
         *   forwardedUrl = /security/forbidden
         *   request attribute:
         *     securityErrorMessage = 접근 권한이 없습니다.
         *     securityErrorPath    = /admin
         */
        // given
        String email = uniqueEmail("user");

        memberService.signup(
                email,
                PASSWORD,
                "일반회원",
                "010-1111-2222"
        );

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        // when & then
        mockMvc.perform(get("/admin")
                        .session(session))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/security/forbidden"))
                .andExpect(request().attribute(
                        "securityErrorMessage",
                        "접근 권한이 없습니다."
                ))
                .andExpect(request().attribute("securityErrorPath", "/admin"));
    }

    @Test
    void 일반_회원이_관리자_API에_접근하면_JSON_403을_반환한다()
            throws Exception {

        // given
        String email = uniqueEmail("api-user");

        memberService.signup(
                email,
                PASSWORD,
                "API 일반회원",
                "010-2222-3333"
        );

        MockHttpSession session =
                loginAndGetSession(email, PASSWORD);

        // when & then
        mockMvc.perform(get("/api/v1/admin/test")
                        .session(session))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status")
                        .value(403))
                .andExpect(jsonPath("$.code")
                        .value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message")
                        .value("접근 권한이 없습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/admin/test"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isEmpty());
    }

    /**
     * ROLE_ADMIN 권한을 가진 인증 사용자는
     * 관리자 화면에 접근할 수 있다.
     */
    @Test
    void 활성_관리자는_관리자_페이지에_접근할_수_있다()
            throws Exception {

        // given
        String email = uniqueEmail("admin");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "관리자",
                "010-1111-2222"
        );

        memberService.changeRole(memberId, MemberRole.ADMIN);

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        mockMvc.perform(get("/admin")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("dashboard"));
    }

    /**
     * 마이그레이션 단계의 로그인 성공 결과
     * <p>
     * 신규 Spring Security 인증 정보와 기존 Interceptor 호환용 세션 속성이 한 세션에 함께 저장되는지 검증
     * <p>
     * 전체 흐름
     * * POST /login
     * * -> AuthenticationManager가 인증
     * * -> DaoAuthenticationProvider가 회원, 비밀번호, 상태 검사
     * * -> 인증 성공 Authentication 생성
     * * -> SecurityContext에 Authentication 저장
     * * -> SecurityContext를 HttpSession에 저장
     * * -> 기존 인터셉터용 세션 정보도 저장
     * * -> 테스트에서 두 인증 정보 모두 검증
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
     * <p>
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
     * <p>
     * 세션에는 관리자 정보가 남아 있어도 DB에서 비활성화되었다면
     * 세션을 폐기하고 로그인 페이지로 이동해야 한다.
     * <p>
     * TODO: Security 전환 완료 후에는 Principal 갱신 및 비활성화 정책 테스트로 교체한다.
     */
    @Test
    void 로그인_후_비활성화된_관리자가_접근하면_세션을_무효화한다()
            throws Exception {

        /**
         * 1. ACTIVE + ADMIN 회원 생성
         * 2. 로그인 성공
         * 3. 세션의 SecurityContext에 ROLE_ADMIN 저장
         * 4. DB 회원 상태를 INACTIVE로 변경
         * 5. GET /admin 요청
         * 6. Spring Security는 기존 ROLE_ADMIN을 보고 접근 허용
         * 7. AdminAuthInterceptor가 DB 상태 재조회
         * 8. INACTIVE 확인
         * 9. 세션 무효화
         * 10. /login?...reason=INACTIVE_ACCOUNT 리다이렉트
         */
        // given
        String email = uniqueEmail("inactive-admin");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "비활성관리자",
                "010-1111-2222"
        );

        memberService.changeRole(memberId, MemberRole.ADMIN);

        // 로그인 시점에는 ACTIVE + ADMIN이므로
        // SecurityContext에는 ROLE_ADMIN 인증 정보가 저장된다.
        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        SecurityContext securityContext = getSecurityContext(session);

        assertThat(securityContext
                .getAuthentication()
                .isAuthenticated()
        ).isTrue();

        assertThat(
                securityContext
                        .getAuthentication()
                        .getAuthorities()
        )
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");

        /**
         * 로그인 이후 DB 회원 상태 변경
         *
         * 기존 SecurityContext의 ROLE_ADMIN 권한은
         * 여기서 자동으로 갱신되지 않는다.
         */
        memberService.deactivate(memberId);

        // when & then
        mockMvc.perform(get("/admin")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?next=/admin&reason=INACTIVE_ACCOUNT"
                ));

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void 권한부족_오류정보를_403_화면으로_변환한다()
            throws Exception {

        /**
         * 테스트 코드
         * → securityErrorMessage에 "접근 권한이 없습니다." 직접 저장
         * → GET /security/forbidden
         * → Controller가 request attribute를 읽음
         * → Model의 message로 복사
         * → error/403 화면 반환
         */

        mockMvc.perform(get("/security/forbidden")
                        .requestAttr(
                                "securityErrorMessage",
                                "접근 권한이 없습니다."
                        )
                        .requestAttr(
                                "securityErrorPath",
                                "/admin"
                        ))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"))
                .andExpect(model().attribute(
                        "status",
                        403
                ))
                .andExpect(model().attribute(
                        "error",
                        "Forbidden"
                ))
                .andExpect(model().attribute(
                        "message",
                        "접근 권한이 없습니다."
                ))
                .andExpect(model().attribute(
                        "path",
                        "/admin"
                ));
    }

    @Test
    void 로그인에_성공하면_SessionRegistry에_세션이_등록된다() throws Exception {

        /**
         * 테스트 회원 생성
         * → 실제 로그인 요청
         * → 인증 성공
         * → 세션 ID 변경
         * → SessionRegistry 등록
         * → 로그인 응답에서 세션 획득
         * → 회원의 Principal 조회
         * → Principal에 연결된 세션 목록 조회
         * → 실제 로그인 세션 ID가 포함됐는지 검증
         * → 테스트에서 등록한 세션 정보 제거
         */

        // given
        String email = uniqueEmail("session-registry");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "세션회원",
                "010-1111-2222"
        );

        // when
        MockHttpSession session = null;

        try {
            /**
             * 실제 로그인 요청 실행
             *
             * POST /login
             * → AuthViewController.login()
             * → AuthenticationManager.authenticate()
             * → DaoAuthenticationProvider
             * → ShopUserDetailsService
             * → DB에서 이메일로 회원 조회
             * → PasswordEncoder.matches()
             * → 인증된 Authentication 반환
             */
            session = loginAndGetSession(
                    email,
                    PASSWORD
            );

            // then
            ShopUserPrincipal principal =
                    sessionRegistry.getAllPrincipals()
                            .stream()
                            .filter(ShopUserPrincipal.class::isInstance)
                            .map(ShopUserPrincipal.class::cast)
                            .filter(principalItem ->
                                    principalItem.getMemberId().equals(memberId))
                            .findFirst()
                            .orElseThrow();

            assertThat(
                    sessionRegistry.getAllSessions(
                            principal, false
                    )
            )
                    .extracting(SessionInformation::getSessionId)
                    .contains(session.getId());
        } finally {
            /**
             * 세션 정리 코드
             *
             * DB 데이터는 @Transactional과 @Rollback으로 정리할 수 있지만
             * SessionRegistry는 DB가 아니라 메모리에 저장된 Singleton Bean이므로 트랜잭션 롤백 대상이 아님.
             * --> 즉, 다른 테스트의 세션 정보가 누적될 수 있음.
             *
             * 따라서 테스트 종료 후 세션을 정리해준다.
             */
            if (session != null) {
                sessionRegistry.removeSessionInformation(session.getId());
            }
        }
    }

    @Test
    void 회원_ID로_등록된_모든_세션을_만료시킨다() throws Exception {

        // given
        String email = uniqueEmail("expire-session");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "만료회원",
                "010-2222-3333"
        );

        loginAndGetSession(email, PASSWORD);

        ShopUserPrincipal principal = sessionRegistry.getAllPrincipals()
                .stream()
                .filter(ShopUserPrincipal.class::isInstance)
                .map(ShopUserPrincipal.class::cast)
                .filter(p ->
                        memberId.equals(p.getMemberId())
                )
                .findFirst()
                .orElseThrow();

        SessionInformation sessionInformation = sessionRegistry.getAllSessions(
                principal,
                false
        ).getFirst();

        assertThat(sessionInformation.isExpired()).isFalse();

        // when
        memberSessionService.expireAllByMemberId(memberId);

        // then
        assertThat(sessionInformation.isExpired()).isTrue();
    }

    private String uniqueEmail(String prefix) {
        return prefix + System.nanoTime() + "@test.com";
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

        if (next != null) {
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

    private MockHttpSession loginAndGetSession(
            String email, String password
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/login")
                                .param("email", email)
                                .param("password", password)
                )
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        assertThat(session).isNotNull();

        return session;
    }
}
