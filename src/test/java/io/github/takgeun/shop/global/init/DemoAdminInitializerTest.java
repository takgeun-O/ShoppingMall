package io.github.takgeun.shop.global.init;

import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.infra.memory.MemoryMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoAdminInitializerTest {

    private static final String ADMIN_EMAIL = "admin@test.com";
    private static final String ADMIN_PASSWORD = "admin-password";

    private MemoryMemberRepository memberRepository;
    private MemberService memberService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        memberRepository = new MemoryMemberRepository();
        passwordEncoder = new BCryptPasswordEncoder(4);
        memberService = new MemberService(memberRepository, passwordEncoder);
    }

    @Test
    void 관리자는_한_번만_생성되고_BCrypt_비밀번호와_ADMIN_권한을_갖는다() {
        DemoAdminInitializer initializer = initializer(ADMIN_PASSWORD);

        initializer.run(null);
        initializer.run(null);

        assertEquals(1, memberRepository.findAll().size());
        Member admin = memberRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        assertEquals(MemberRole.ADMIN, admin.getRole());
        assertTrue(passwordEncoder.matches(ADMIN_PASSWORD, admin.getPassword()));
    }

    @Test
    void 신규_관리자에_필요한_비밀번호가_공백이면_실행을_중단한다() {
        DemoAdminInitializer initializer = initializer("   ");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> initializer.run(null)
        );

        assertEquals(
                "ADMIN_PASSWORD environment variable is required for demo admin initialization.",
                exception.getMessage()
        );
        assertTrue(memberRepository.findAll().isEmpty());
    }

    @Test
    void 기존_계정이_있으면_비밀번호나_권한을_덮어쓰지_않는다() {
        Long memberId = memberService.signup(
                ADMIN_EMAIL,
                "existing-password",
                "기존회원",
                "010-1111-2222"
        );
        String originalPassword = memberService.findById(memberId).getPassword();
        DemoAdminInitializer initializer = initializer("");

        assertDoesNotThrow(() -> initializer.run(null));

        Member existing = memberService.findById(memberId);
        assertEquals(MemberRole.USER, existing.getRole());
        assertEquals(originalPassword, existing.getPassword());
    }

    private DemoAdminInitializer initializer(String password) {
        DemoAdminInitializer initializer = new DemoAdminInitializer(memberService);
        ReflectionTestUtils.setField(initializer, "adminEmail", ADMIN_EMAIL);
        ReflectionTestUtils.setField(initializer, "adminPassword", password);
        ReflectionTestUtils.setField(initializer, "adminName", "관리자");
        ReflectionTestUtils.setField(initializer, "adminPhone", "010-9999-9999");
        return initializer;
    }
}
