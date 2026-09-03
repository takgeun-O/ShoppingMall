package io.github.takgeun.shop.member;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.global.error.exception.UnauthorizedException;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@Rollback
class PasswordEncodingIntegrationTest extends IntegrationTestSupport {

    private static final String INITIAL_PASSWORD = "initial-password";
    private static final String CHANGED_PASSWORD = "changed-password";

    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MemberService memberService;
    @Autowired private AuthenticationManager authenticationManager;

    @Test
    void 회원가입한_BCrypt_비밀번호로_인증할_수_있다() {

        // given
        String email = uniqueEmail("bcrypt-signup");

        Long memberId = memberService.signup(
                email,
                INITIAL_PASSWORD,
                "암호화회원",
                "010-1111-2222"
        );

        // 저장된 비밀번호가 평문이 아닌지 확인
        Member storedMember =
                memberService.findById(memberId);

        assertThat(storedMember.getPassword())
                .isNotEqualTo(INITIAL_PASSWORD);

        assertThat(passwordEncoder.matches(
                INITIAL_PASSWORD,
                storedMember.getPassword()
        )).isTrue();

        // when
        Authentication authentication = authenticate(
                email,
                INITIAL_PASSWORD
        );

        // then
        assertThat(authentication.isAuthenticated())
                .isTrue();

        assertThat(authentication.getPrincipal())
                .isInstanceOf(ShopUserPrincipal.class);

        ShopUserPrincipal principal =
                (ShopUserPrincipal) authentication.getPrincipal();

        assertThat(principal.getMemberId())
                .isEqualTo(memberId);

        assertThat(principal.getUsername())
                .isEqualTo(email);
    }

    @Test
    void 비밀번호를_변경하면_새_비밀번호만_인증할_수_있다() {

        // given
        String email = uniqueEmail("bcrypt-change");

        Long memberId = memberService.signup(
                email,
                INITIAL_PASSWORD,
                "변경회원",
                "010-2222-3333"
        );

        // when
        memberService.updateProfile(
                memberId,
                null,
                CHANGED_PASSWORD,
                null
        );

        // then: 변경된 비밀번호가 BCrypt로 저장됐는지 확인
        Member updatedMember =
                memberService.findById(memberId);

        assertThat(updatedMember.getPassword())
                .isNotEqualTo(CHANGED_PASSWORD);

        assertThat(passwordEncoder.matches(
                CHANGED_PASSWORD,
                updatedMember.getPassword()
        )).isTrue();

        // 이전 비밀번호로 인증할 수 없음
        assertThatThrownBy(() ->
                authenticate(email, INITIAL_PASSWORD)
        ).isInstanceOf(BadCredentialsException.class);

        // 새 비밀번호로 인증 성공
        Authentication authentication = authenticate(
                email,
                CHANGED_PASSWORD
        );

        assertThat(authentication.isAuthenticated())
                .isTrue();

        ShopUserPrincipal principal =
                (ShopUserPrincipal) authentication.getPrincipal();

        assertThat(principal.getMemberId())
                .isEqualTo(memberId);
    }

    private String uniqueEmail(String prefix) {
        return prefix + System.nanoTime() + "@test.com";
    }

    private Authentication authenticate(
            String email,
            String rawPassword
    ) {
        UsernamePasswordAuthenticationToken authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                email,
                rawPassword
        );

        return authenticationManager.authenticate(authenticationRequest);
    }
}
