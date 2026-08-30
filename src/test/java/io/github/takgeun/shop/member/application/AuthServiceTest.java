package io.github.takgeun.shop.member.application;

import io.github.takgeun.shop.global.error.exception.ForbiddenException;
import io.github.takgeun.shop.global.error.exception.UnauthorizedException;
import io.github.takgeun.shop.member.infra.memory.MemoryMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    private static final String PASSWORD = "test-password";
    private static final String AUTHENTICATION_FAILURE_MESSAGE =
            "이메일 또는 비밀번호가 올바르지 않습니다.";

    private MemberService memberService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MemoryMemberRepository memberRepository = new MemoryMemberRepository();
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
        memberService = new MemberService(memberRepository, passwordEncoder);
        authService = new AuthService(memberRepository, passwordEncoder);
    }

    @Test
    void 올바른_평문_비밀번호로_로그인한다() {
        Long memberId = memberService.signup(
                "login@test.com",
                PASSWORD,
                "로그인회원",
                "010-1111-2222"
        );

        assertEquals(memberId, authService.login("LOGIN@test.com", PASSWORD));
        assertNotNull(memberService.findById(memberId).getLastLoginAt());
    }

    @Test
    void 잘못된_비밀번호와_존재하지_않는_이메일은_같은_실패_메시지를_사용한다() {
        memberService.signup(
                "member@test.com",
                PASSWORD,
                "회원",
                "010-2222-3333"
        );

        UnauthorizedException wrongPassword = assertThrows(
                UnauthorizedException.class,
                () -> authService.login("member@test.com", "wrong-password")
        );
        UnauthorizedException unknownEmail = assertThrows(
                UnauthorizedException.class,
                () -> authService.login("unknown@test.com", PASSWORD)
        );

        assertEquals(AUTHENTICATION_FAILURE_MESSAGE, wrongPassword.getMessage());
        assertEquals(AUTHENTICATION_FAILURE_MESSAGE, unknownEmail.getMessage());
    }

    @Test
    void 비활성_회원의_로그인_정책을_유지한다() {
        Long memberId = memberService.signup(
                "inactive@test.com",
                PASSWORD,
                "비활성회원",
                "010-3333-4444"
        );
        memberService.deactivate(memberId);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> authService.login("inactive@test.com", PASSWORD)
        );

        assertEquals("비활성화된 회원입니다.", exception.getMessage());
    }
}
