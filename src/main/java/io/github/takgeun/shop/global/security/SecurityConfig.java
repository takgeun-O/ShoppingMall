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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

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
            DaoAuthenticationProvider authenticationProvider,
            ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
            ApiAccessDeniedHandler apiAccessDeniedHandler,
            ViewAuthenticationEntryPoint viewAuthenticationEntryPoint,
            ViewAccessDeniedHandler viewAccessDeniedHandler
    ) throws Exception {

        http
                // TODO(security-migration): SecurityContext 전환 후 CSRF, logout, URL 권한 규칙을 활성화한다.
                // 기존 세션/Interceptor 기반 POST 화면 흐름을 Security 전환 중에도 유지한다.
                .csrf(csrf -> csrf.disable())
                // 기존 AuthViewController가 세션 무효화와 성공 메시지를 계속 담당한다.
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(auth -> auth
                        /**
                         * 누구나 접근할 수 있는 공개 경로
                         */
                        .requestMatchers(
                                "/",
                                "/login",
                                "/signup",
                                "/products/**",
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
                         * 아직 권한 계약을 정하지 않은 경로는
                         * 이번 마이그레이션 단계에서 기준 동작을 유지
                         */
                        .anyRequest().permitAll()
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
}
