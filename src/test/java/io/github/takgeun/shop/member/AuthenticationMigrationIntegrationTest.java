package io.github.takgeun.shop.member;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.global.security.session.MemberSessionService;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.MemberRole;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.transaction.annotation.Propagation;
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
    @Autowired
    private MemberService memberService;

    /**
     * SessionRegistry는 트랜잭션 롤백 대상이 아님.
     * 로그인 관련 여러 테스트가 세션을 계속 등록할 수 있으니
     * 각 테스트가 끝날 때 메모리의 세션 등록 정보를 비운다.
     */
    @AfterEach
    void clearSessionRegistry() {
        sessionRegistry.getAllPrincipals()
                .forEach(principal ->
                        sessionRegistry
                                .getAllSessions(principal, true)
                                .forEach(session ->
                                        sessionRegistry
                                                .removeSessionInformation(session.getSessionId()
                                                )
                                )
                );
    }

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

    @Test
    void Spring_Security_인증만_있는_회원도_주문_페이지에_접근할_수_있다() throws Exception {

        // given
        String email = uniqueEmail("security-only-order");

        memberService.signup(
                email,
                PASSWORD,
                "security인증회원",
                "010-1111-2222"
        );

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        // SecurityContext는 살아있는지 확인
        assertThat(session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        )).isNotNull();

        // when & then
        mockMvc.perform(get("/orders/checkout")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))      // 장바구니에 아무것도 안 담았으니까
                .andExpect(flash().attribute(
                        "error",
                        "장바구니가 비어있습니다."
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
     * 로그인 성공 시 인증 정보가 SecurityContext를 통해
     * HttpSession에 저장되는지 검증한다.
     *
     * <p>전체 흐름</p>
     *
     * <ol>
     *     <li>POST /login 요청</li>
     *     <li>AuthenticationManager가 인증 요청을 처리</li>
     *     <li>DaoAuthenticationProvider가 UserDetailsService를 통해 회원 조회</li>
     *     <li>PasswordEncoder를 통해 비밀번호 검증</li>
     *     <li>인증에 성공한 Authentication 반환</li>
     *     <li>Authentication을 SecurityContext에 저장</li>
     *     <li>SecurityContextRepository가 SecurityContext를 HttpSession에 저장</li>
     *     <li>세션에 저장된 ShopUserPrincipal의 회원 정보와 권한 검증</li>
     * </ol>
     */
    @Test
    void 로그인에_성공하면_SecurityContext에_인증정보를_저장한다()
            throws Exception {

        // given
        String email = uniqueEmail("login");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "로그인회원",
                "010-1111-2222"
        );

        // when: 실제 로그인 요청을 보내고 성공 결과를 받는다.
        MvcResult result = performSuccessfulLogin(
                email,
                PASSWORD,
                null,
                "/"
        );

        /*
         * 로그인 성공 후 세션 구조
         *
         * HttpSession
         * └── SPRING_SECURITY_CONTEXT
         *     └── SecurityContext
         *         └── Authentication
         *             └── ShopUserPrincipal
         */
        MockHttpSession session = getSession(result);

        // then: 세션에 저장된 Spring Security 인증 정보를 조회한다.
        SecurityContext securityContext =
                getSecurityContext(session);

        Authentication authentication =
                securityContext.getAuthentication();

        // 인증이 완료된 Authentication인지 검증한다.
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated())
                .isTrue();

        // 인증 주체가 애플리케이션의 사용자 객체인지 검증한다.
        assertThat(authentication.getPrincipal())
                .isInstanceOf(ShopUserPrincipal.class);

        ShopUserPrincipal principal =
                (ShopUserPrincipal) authentication.getPrincipal();

        // Principal에 인증된 회원 정보가 정확히 저장됐는지 검증한다.
        assertThat(principal.getMemberId())
                .isEqualTo(memberId);

        assertThat(principal.getUsername())
                .isEqualTo(email);

        assertThat(principal.getName())
                .isEqualTo("로그인회원");

        assertThat(principal.getRole())
                .isEqualTo(MemberRole.USER);

        // hasRole("USER")에서 사용할 ROLE_USER 권한이 있는지 검증한다.
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
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
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
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
                                .SPRING_SECURITY_CONTEXT_KEY
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
                                .SPRING_SECURITY_CONTEXT_KEY
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
     * 테스트 자체는 트랜잭션 없음
     * → memberService.deactivate()가 자체 트랜잭션 시작
     * → 메서드 종료 시 실제 커밋
     * → AFTER_COMMIT 리스너 실행
     * → SessionInformation 만료
     * → 다음 /admin 요청에서 ConcurrentSessionFilter가 만료 감지
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 로그인_후_비활성화된_관리자는_만료된_세션으로_관리자_페이지에_접근할_수_없다()
            throws Exception {

        /**
         * ACTIVE + ADMIN 로그인
         * → SecurityContext와 SessionRegistry에 로그인 세션 등록
         * → memberService.deactivate()
         * → 세션 만료 이벤트 발행
         * → 회원 비활성화 트랜잭션 커밋
         * → MemberSessionExpirationListener 실행
         * → SessionInformation.expireNow()
         * → 만료된 세션으로 GET /admin
         * → ConcurrentSessionFilter가 만료 감지
         * → 세션 무효화
         * → /login?reason=SESSION_EXPIRED 리다이렉트
         */
        // given
        String email =
                uniqueEmail("inactive-admin");

        Long memberId =
                memberService.signup(
                        email,
                        PASSWORD,
                        "비활성관리자",
                        "010-1111-2222"
                );

        memberService.changeRole(
                memberId,
                MemberRole.ADMIN
        );

        MockHttpSession session =
                loginAndGetSession(
                        email,
                        PASSWORD
                );

        SecurityContext securityContext =
                getSecurityContext(session);

        assertThat(
                securityContext.getAuthentication()
                        .isAuthenticated()
        ).isTrue();

        assertThat(
                securityContext.getAuthentication()
                        .getAuthorities()
        )
                .extracting(
                        GrantedAuthority::getAuthority
                )
                .contains("ROLE_ADMIN");

        SessionInformation sessionInformation =
                findSessionInformation(memberId);

        assertThat(sessionInformation)
                .as("관리자 로그인 세션이 등록되어야 한다.")
                .isNotNull();

        assertThat(sessionInformation.isExpired())
                .isFalse();

        // when
        memberService.deactivate(memberId);

        /*
         * deactivate() 트랜잭션이 커밋된 후
         * MemberSessionExpirationListener가 실행되어야 한다.
         */
        assertThat(sessionInformation.isExpired())
                .as("회원 비활성화 커밋 후 세션이 만료되어야 한다.")
                .isTrue();

        // then
        mockMvc.perform(get("/admin")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?reason=SESSION_EXPIRED"
                ));

        assertThat(session.isInvalid())
                .isTrue();
    }

    @Test
    void 로그인_후_비활성화된_관리자는_관리자_페이지에_접근할_수_없다() throws Exception {

        // given
        String email = uniqueEmail("inactive-admin-dashboard");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "비활성관리자",
                "010-1111-2222"
        );

        memberService.changeRole(memberId, MemberRole.ADMIN);

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        /**
         * expireNow()
         * → SessionInformation.expired = true
         * → 다음 요청
         * → ConcurrentSessionFilter가 있어야 만료 감지
         */
        SessionInformation sessionInformation = findSessionInformation(memberId);

        assertThat(sessionInformation.getSessionId())
                .isEqualTo(session.getId());
        assertThat(sessionInformation.isExpired())
                .isFalse();

        // when
        memberSessionService.expireAllByMemberId(memberId);

        assertThat(sessionInformation.isExpired())
                .isTrue();

        // then
        // 만약 200 으로 테스트 결과가 나오면 ConcurrentSessionFilter가 동작하는지 체크하기 (SecurityConfig에서)
        mockMvc.perform(get("/admin")
                .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/login?reason=SESSION_EXPIRED"
                ));
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

    @Test
    void 관리자는_requireAdmin_없이도_관리자_상품_페이지에_접근할_수_있다() throws Exception {

        // given
        String email = uniqueEmail("admin-products");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "상품관리자",
                "010-1111-2222"
        );

        memberService.changeRole(
                memberId,
                MemberRole.ADMIN
        );

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        // when & then
        mockMvc.perform(get("/admin/products")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "admin/products/list"
                ))
                .andExpect(model().attributeExists(
                        "products",
                        "summary"
                ));
    }

    @Test
    void 일반_회원은_관리자_상품_페이지에_접근할_수_없다() throws Exception {

        /**
         * GET /admin/products
         * → Spring SecurityFilterChain
         * → /admin/**에는 hasRole("ADMIN") 적용
         * → 일반 회원은 ROLE_USER 보유
         * → 권한 검사 실패
         * → AccessDeniedException
         * → ViewAccessDeniedHandler.handle()
         * → /security/forbidden으로 forward
         */
        // given
        String email = uniqueEmail("user-admin-products");

        memberService.signup(
                email,
                PASSWORD,
                "일반회원",
                "010-2222-3333"
        );

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        // when & then
        mockMvc.perform(get("/admin/products")
                        .session(session))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl(
                        "/security/forbidden"
                ))
                .andExpect(request().attribute(
                        "securityErrorMessage",
                        "접근 권한이 없습니다."
                ))
                .andExpect(request().attribute(
                        "securityErrorPath",
                        "/admin/products"
                ));
    }

    @Test
    void Spring_Security_ADMIN_권한만으로_관리자_상품_페이지에_접근할_수_있다() throws Exception {

        // given
        String email = uniqueEmail("security-only-admin-products");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "상품관리자",
                "010-1111-2222"
        );

        memberService.changeRole(memberId, MemberRole.ADMIN);

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        // Spring Security 인증 정보만 유지되는지 확인
        assertThat(
                session.getAttribute(
                        HttpSessionSecurityContextRepository
                                .SPRING_SECURITY_CONTEXT_KEY
                )
        ).isNotNull();

        // when & then
        mockMvc.perform(get("/admin/products")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products/list"))
                .andExpect(model().attributeExists(
                        "products",
                        "summary"
                ));
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

    private MockHttpSession loginAndGetSession(
            String email, String password
    ) throws Exception {

        /**
         * MvcResult
         * ├─ 요청 정보
         * ├─ 응답 정보
         * ├─ 세션 정보
         * ├─ ModelAndView
         * ├─ Controller에서 발생한 예외
         * ├─ 비동기 처리 결과
         * └─ Handler 정보
         */
        MvcResult result = mockMvc.perform(
                        post("/login")
                                .param("email", email)
                                .param("password", password)
                )
                .andExpect(status().is3xxRedirection())
                .andReturn();       // MockMvc로 실행한 요청의 전체 결과를 MvcResult 객체로 반환한다.

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        assertThat(session).isNotNull();

        return session;
    }

    private SessionInformation findSessionInformation(Long memberId) {
        ShopUserPrincipal principal = sessionRegistry.getAllPrincipals()
                .stream()
                .filter(ShopUserPrincipal.class::isInstance)
                .map(ShopUserPrincipal.class::cast)
                .filter(item ->
                        memberId.equals(item.getMemberId()))
                .findFirst()
                .orElseThrow();

        return sessionRegistry.getAllSessions(
                        principal,
                        false
                ).stream()
                .findFirst()
                .orElseThrow();
    }
}
