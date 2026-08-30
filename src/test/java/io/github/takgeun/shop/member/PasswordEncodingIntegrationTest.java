package io.github.takgeun.shop.member;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.global.error.exception.UnauthorizedException;
import io.github.takgeun.shop.member.application.AuthService;
import io.github.takgeun.shop.member.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired private AuthService authService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void 회원가입한_BCrypt_비밀번호로_로그인한다() {
        String email = uniqueEmail("bcrypt-signup");

        Long memberId = memberService.signup(
                email,
                INITIAL_PASSWORD,
                "암호화회원",
                "010-1111-2222"
        );

        Member storedMember = memberService.findById(memberId);
        assertThat(storedMember.getPassword()).isNotEqualTo(INITIAL_PASSWORD);
        assertThat(passwordEncoder.matches(INITIAL_PASSWORD, storedMember.getPassword())).isTrue();
        assertThat(authService.login(email, INITIAL_PASSWORD)).isEqualTo(memberId);
    }

    @Test
    void 비밀번호를_변경하면_새_비밀번호만_로그인할_수_있다() {
        String email = uniqueEmail("bcrypt-change");
        Long memberId = memberService.signup(
                email,
                INITIAL_PASSWORD,
                "변경회원",
                "010-2222-3333"
        );

        memberService.updateProfile(memberId, null, CHANGED_PASSWORD, null);

        Member updatedMember = memberService.findById(memberId);
        assertThat(updatedMember.getPassword()).isNotEqualTo(CHANGED_PASSWORD);
        assertThat(passwordEncoder.matches(CHANGED_PASSWORD, updatedMember.getPassword())).isTrue();
        assertThatThrownBy(() -> authService.login(email, INITIAL_PASSWORD))
                .isInstanceOf(UnauthorizedException.class);
        assertThat(authService.login(email, CHANGED_PASSWORD)).isEqualTo(memberId);
    }

    private String uniqueEmail(String prefix) {
        return prefix + System.nanoTime() + "@test.com";
    }
}
