package io.github.takgeun.shop.global.security;

import io.github.takgeun.shop.global.security.handler.ApiAccessDeniedHandler;
import io.github.takgeun.shop.global.security.handler.ApiAuthenticationEntryPoint;
import io.github.takgeun.shop.global.security.handler.ViewAccessDeniedHandler;
import io.github.takgeun.shop.global.security.handler.ViewAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.util.List;

@Configuration
public class SecurityConfig {

    /**
     * 기존)
     * 로그인 요청
     * -> 기존 로그인 Controller
     * -> AuthService.login()
     * -> PasswordEncoder.matches() : 즉, AuthService가 직접 비밀번호를 검사하는 구조
     * -> 기존 HttpSession에 회원 ID와 권한 저장
     * -> 기존 Interceptor가 인증 인가 검사
     * <p>
     * Spring Security 전환 완료 후)
     * 로그인 요청
     * -> Spring Security 인증 필터
     * -> AuthenticationManager     : AuthService가 직접 비밀번호를 검사하는 구조에서 AuthenticationManager가 인증하도록 바꾸기
     * -> ShopUserDetailsService : 이메일로 회원 조희
     * -> PasswordEncoder.matches() : 입력 비밀번호와 DB의 BCrypt 해시 비교
     * -> SecurityContext에 Authentication 저장
     * -> SecurityFilterChain : 로그인 요청과 URL 접근 권한 처리
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SessionRegistry sessionRegistry,
            DaoAuthenticationProvider authenticationProvider,
            ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
            ApiAccessDeniedHandler apiAccessDeniedHandler,
            ViewAuthenticationEntryPoint viewAuthenticationEntryPoint,
            ViewAccessDeniedHandler viewAccessDeniedHandler
    ) throws Exception {

        http
                // TODO(security-migration): SecurityContext 전환 후 CSRF, logout, URL 권한 규칙을 활성화한다.
                .csrf(csrf -> csrf.disable())

                /**
                 * POST /logout
                 * → Spring Security 필터가 요청 감지
                 * → 컨트롤러로 보내지 않고 로그아웃 처리
                 * → SecurityContext 인증정보 제거
                 * → HttpSession 무효화
                 * → JSESSIONID 쿠키 삭제
                 * → 홈(/)으로 리다이렉트
                 */
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")    // 세션 쿠키 삭제 명령 Cookie: JSESSIONID=ABC123
                        .permitAll()
                )

                /**
                 * SessionManagementConfigurer 활성화
                 * → 동시 세션 제어 설정 활성화
                 * → 사용할 SessionRegistry 지정
                 * → 로그인 세션 등록 및 제어 전략 준비
                 * → ConcurrentSessionFilter 생성 : 생성되는 원인은 maximumSessions(-1) 덕분임. 이 호출이 동시 세션 제어 설정을 활성화하기 때문
                 * → SecurityFilterChain에 필터 추가
                 */
                .sessionManagement(session -> session
                        .maximumSessions(-1) // 동시 세션 관리 기능을 사용한다 + 허용할 동시 세션 수는 제한하지 않는다.
                        .sessionRegistry(sessionRegistry)
                        .expiredUrl(
                                "/login?reason=SESSION_EXPIRED" // 만료된 세션을 발견했을 때 여기로 이동하도록 설정
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        /**
                         * 누구나 접근할 수 있는 공개 경로
                         */
                        .requestMatchers(
                                "/",
                                "/login",
                                "/signup",
                                "/products/**",
                                "/cart/**",
                                "/api/v1/categories/**",
                                "/api/v1/products/**",
                                "/security/forbidden",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/assets/**",
                                "/webjars/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/error",
                                "/favicon.ico"
                        ).permitAll()

                        /**
                         * 관리자 권한 필요
                         *
                         * hasRole("ADMIN")은 내부적으로 ROLE_ADMIN 권한을 검사한다.
                         */
                        .requestMatchers(
                                "/admin/**",
                                "/api/v1/admin/**"
                        ).hasRole("ADMIN")

                        /**
                         * Role 과 관계없이 로그인 필요
                         */
                        .requestMatchers(
                                "/orders",
                                "/orders/**",
                                "/members/me",
                                "/members/me/**"
                        ).authenticated()

                        /**
                         * 위에서 명시하지 않은 경로는 기본 차단
                         *
                         * authenticated()를 쓰지 않은 이유
                         * 새 URL을 만들고 SecurityConfig에 등록해야 하는데 그걸 깜빡했을 때
                         * 공개되지 않아야 할 게 공개되는 경우가 있음.
                         *
                         * denyAll()을 쓰면 명시적으로 권한을 정하기 전까지 접근하지 못하도록 막을 수 있음.
                         */
                        .anyRequest().denyAll()
                )

                // SecurityConfig에 처리기 연결
                .exceptionHandling(exception -> exception

                        // API 비로그인 요청
                        .defaultAuthenticationEntryPointFor(
                                apiAuthenticationEntryPoint,
                                PathPatternRequestMatcher
                                        .withDefaults()
                                        .matcher("/api/**")
                        )

                        // 그 밖의 화면 비로그인 요청
                        .defaultAuthenticationEntryPointFor(
                                viewAuthenticationEntryPoint,
                                PathPatternRequestMatcher
                                        .withDefaults()
                                        .matcher("/**")
                        )

                        // API 권한 부족
                        .defaultAccessDeniedHandlerFor(
                                apiAccessDeniedHandler,
                                PathPatternRequestMatcher.withDefaults()
                                        .matcher("/api/**")
                        )

                        // 그 밖의 화면 권한 부족
                        .defaultAccessDeniedHandlerFor(
                                viewAccessDeniedHandler,
                                PathPatternRequestMatcher
                                        .withDefaults()
                                        .matcher("/**")
                        )
                )

                .authenticationProvider(authenticationProvider);

        return http.build();
    }

    /**
     * 비밀번호 암호화 및 비교 담당
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * DB 회원 조회와 비밀번호 검증을 수행하는 인증 공급자
     */
    @Bean
    DaoAuthenticationProvider authenticationProvider(
            ShopUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    /**
     * AuthenticationManager 빈 등록
     */
    @Bean
    AuthenticationManager authenticationManager(
            DaoAuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    /**
     * SecurityContext 저장소 빈 등록
     * AuthenticationManager.authenticate()는 인증 결과를 반환할 뿐
     * 컨트롤러에서 수동으로 인증한 경우 결과를 세션에 자동 저장해주지 않음.
     * <p>
     * 따라서 인증 결과를 저장해주는 역할을 하는 SecurityContextRepository을 빈으로 등록하기
     * 인증 정보를 SPRING_SECURITY_CONTEXT 라는 세션 속성에 저장하게 해준다.
     */
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * SessionRegistry는 다음 관계를 관리한다.
     * ShopUserPrincipal
     * └── SessionInformation
     *     ├── sessionId
     *     ├── lastRequest
     *     └── expired
     *
     * 기존 세션 로그인 방식은 Spring Security의 인증 필터가 아니라
     * AuthViewController에서 AuthenticationManager를 직접 호출하는 수동 인증 방식이다.
     *
     * AuthenticationManager.authenticate()는 인증된 Authentication을
     * 반환할 뿐, 세션 등록이나 SessionRegistry 관리를 수행하지 않는다.
     *
     * 따라서 로그인 성공 후 세션 고정 보호, 동시 세션 제어, SessionRegistry 등록 등의
     * 세션 인증 처리를 적용하려면
     * SessionAuthenticationStrategy를 Controller에서 직접 호출해야 한다.
     *
     * Controller에서 직접 호출하기 위해 아래쪽에서 SessionAuthenticationStrategy 빈을 추가한다.
     */
    @Bean
    SessionRegistry sessionRegistry() {
        /**
         * SessionRegistry 객체는 세션 정보를 보관할 장소만 제공한다.
         * 로그인 세션을 자동으로 찾아 등록하지 않음.
         *
         * 실제로 아래 호출이 발생해야 등록된다.
         * sessionRegistry.registerNewSession(sessionId, authentication.getPrincipal());
         *
         * 보통 이러한 호출을 직접 작성하지 않고 RegisterSEssionAuthenticationStrategy가 담당함.
         */
        return new SessionRegistryImpl();
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(
            SessionRegistry sessionRegistry
    ) {
        /**
         * ChangeSessionIdAuthenticationStrategy 의 역할
         * - 로그인 성공 시 세션 ID를 변경
         * - 세션 고정 공격 방지
         * - 기존 장바구니 같은 세션 속성은 유지
         *
         * RegisterSessionAuthenticationStrategy() 의 역할
         * - 로그인한 Principal과 세션 ID를 SessionRegistry에 등록
         */
        return new CompositeSessionAuthenticationStrategy(
                List.of(
                        new ChangeSessionIdAuthenticationStrategy(),
                        new RegisterSessionAuthenticationStrategy(sessionRegistry)
                )
        );
    }
}
