package io.github.takgeun.shop.member;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 DB, 인증 통합 테스트
 * <p>
 * 회원가입
 * → 로그인
 * → 비밀번호 변경
 * → DB에 BCrypt 저장
 * → 이전 비밀번호 인증 실패
 * → 새 비밀번호 인증 성공
 * <p>
 * 여기서는 @Transactional을 붙여도 된다.
 * 같은 트랜잭션 안에서 아래만 확인하기 때문
 * DB 비밀번호 UPDATE
 * → MyBatis 조회
 * → DaoAuthenticationProvider 인증
 */
@Transactional
public class MemberPasswordAuthenticationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberService memberService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Test
    void 비밀번호_변경_후_새_비밀번호만_인증할_수_있다() {

        // given
        String email = uniqueEmail("password-change");

        String currentPassword = "current-password";
        String changedPassword = "changed-password";

        Long memberId = memberService.signup(
                email,
                currentPassword,
                "회원",
                "010-1111-2222"
        );

        Authentication beforeChange =
                authenticate(email, currentPassword);

        assertThat(beforeChange.isAuthenticated())
                .isTrue();

        // when
        memberService.changePassword(
                memberId,
                currentPassword,
                changedPassword
        );

        // then : DB에는 평문이 아닌 BCrypt 해시가 저장된다.
        Member member = memberService.findById(memberId);

        assertThat(member.getPassword())
                .isNotEqualTo(changedPassword); // 평문 비교로 하면 당연히 다름

        assertThat(passwordEncoder.matches(
                changedPassword,
                member.getPassword()
        )).isTrue();

        // 이전 비밀번호로는 더 이상 인증할 수 없다.
        assertThatThrownBy(() ->
                authenticate(email, currentPassword)
        )
                .isInstanceOf(
                        BadCredentialsException.class
                );

        // 변경된 비밀번호로는 인증할 수 있다.
        Authentication afterChange =
                authenticate(email, changedPassword);

        assertThat(afterChange.isAuthenticated())
                .isTrue();

        assertThat(afterChange.getName())
                .isEqualTo(email);
    }

    /**
     * 미인증 토큰 생성
     * → AuthenticationManager에 전달
     * → DaoAuthenticationProvider가 처리 : 이메일과 비밀번호 방식의 로그인을 실제로 검증하는 Spring Security 인증 담당자
     * → ShopUserDetailsService로 회원 조회
     * → PasswordEncoder.matches()로 비밀번호 비교
     * → 성공하면 인증된 Authentication 반환
     */
    private Authentication authenticate(String email, String password) {
        return authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        email, password
                )
        );
    }

    private String uniqueEmail(String prefix) {
        return prefix
                + "-"
                + System.nanoTime()
                + "@test.com";
    }
}
