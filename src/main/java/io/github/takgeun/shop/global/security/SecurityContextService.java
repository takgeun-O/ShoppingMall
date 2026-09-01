package io.github.takgeun.shop.global.security;

import io.github.takgeun.shop.member.domain.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * 프로필 변경 직후 헤더 이름까지 바뀌도록 하는 역할
 */
@Service
@RequiredArgsConstructor
public class SecurityContextService {

    private final SecurityContextRepository securityContextRepository;

    /**
     * DB에서 변경된 회원 정보를 현재 SecurityContext에 반영한다.
     *
     * 이름이나 권한 등 Principal에 포함된 정보는
     * DB만 수정한다고 해서 자동 갱신되지 않으므로 새로운 Principal을 만드는 전략
     */
    public void refreshPrincipal(
            Member member,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        /**
         * 현재 실행 중인 요청
         * → SecurityContextHolder에서 SecurityContext 조회
         * → SecurityContext에서 Authentication 조회
         * → 현재 로그인 사용자 정보 획득
         *
         * SecurityContextHolder
         * └─ SecurityContext
         *    └─ Authentication
         *       ├─ principal
         *       ├─ authorities
         *       ├─ credentials
         *       └─ authenticated
         */
        Authentication currentAuthentication = SecurityContextHolder.getContext()   // 현재 요청을 처리 중인 스레드에서 Spring Security 인증 정보를 꺼낼 수 있도록 보관하는 공간
                .getAuthentication();

        /** refreshPrincipal : 현재 로그인 회원의 최신정보를 담은 객체
         * 기존 Principal
         * ├─ name = "기존 이름"
         * ├─ role = USER
         * └─ status = ACTIVE
         *
         * DB 최신 정보
         * ├─ name = "변경된 이름"
         * ├─ role = ADMIN
         * └─ status = ACTIVE
         *
         * refreshedPrincipal
         * ├─ name = "변경된 이름"
         * ├─ role = ADMIN
         * └─ status = ACTIVE
         */
        ShopUserPrincipal refreshPrincipal = new ShopUserPrincipal(
                member.getId(),
                member.getEmail(),
                member.getPassword(),
                member.getName(),
                member.getRole(),
                member.getStatus()
        );

        /**
         * 갱신된 사용자 정보인 refreshPrincipal을 바탕으로 이미 인증이 완료된 새로운 Authentication 객체를 생성한다.
         *
         * UsernamePasswordAuthenticationToken
         * Spring Security가 아이디와 비밀번호 방식의 인증 정보를 표현할 때 사용하는 Authentication 구현체
         *
         * 인증 전 토큰
         * ├─ 사용자가 입력한 이메일
         * ├─ 사용자가 입력한 평문 비밀번호
         * ├─ 권한 없음
         * └─ authenticated = false
         *
         * 인증 후 토큰
         * ├─ 조회가 완료된 ShopUserPrincipal
         * ├─ 비밀번호 제거
         * ├─ ROLE_USER 또는 ROLE_ADMIN
         * └─ authenticated = true
         */
        UsernamePasswordAuthenticationToken refreshedAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                // 현재 로그인한 회원의 최신 정보

                refreshPrincipal,

                /**
                 * credentials: 인증수단
                 *
                 * 로그인 전에는 일반적으로 평문 비밀번호가 들어감.
                 * 반면에 이미 인증이 끝낸 객체를 만들 때는 평문 비밀번호가 더 이상 필요하지 않음.
                 * 평문 비밀번호를 Authentication이나 세션에 계속 보관하지 않는 것이 보안상 안전함.
                 *
                 * 로그인 입력 시점
                 * → 평문 비밀번호 사용
                 * → PasswordEncoder.matches() 검증
                 * → 인증 완료
                 * → 평문 비밀번호 제거
                 *
                 * 그리고 DB에 저장된 BCrypt 해시도 새 Authentication의 credentials에 넣을 필요 없음.
                 */
                null,

                /**
                 * 갱신된 회원이 보유한 최신 권한 목록
                 * 여기에 권한 정보가 있는데 authenticated 메소드에 의해
                 * authentication.isAuthenticated() -> true 로 바뀌어진다.
                 */
                refreshPrincipal.getAuthorities()
        );

        if(currentAuthentication != null) {
            refreshedAuthentication.setDetails(
                    currentAuthentication.getDetails()
            );
        }

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(refreshedAuthentication);

        SecurityContextHolder.setContext(securityContext);

        /**
         * 현재 요청 Thread에서만 바뀌는 것으로 끝내지 않고
         * 다음 요청에서도 유지되도록 세션에 저장한다.
         */
        securityContextRepository.saveContext(
                securityContext,
                request,
                response
        );


    }
}
