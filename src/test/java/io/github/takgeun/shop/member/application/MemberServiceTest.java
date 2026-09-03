package io.github.takgeun.shop.member.application;

import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.member.api.dto.request.MemberUpdateRequest;
import io.github.takgeun.shop.member.infra.memory.MemoryMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class MemberServiceTest {

    private MemberService memberService;
    private PasswordEncoder passwordEncoder;
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        MemoryMemberRepository memberRepository = new MemoryMemberRepository();
        passwordEncoder = new BCryptPasswordEncoder(4);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);

        memberService = new MemberService(memberRepository, passwordEncoder, eventPublisher);
    }

    @Test
    void 회원가입_성공() {

        // given
        String email = "aaa@abc.com";
        String password = "123123123";
        String name = "테스트";
        String phone = "010-1111-2222";

        // when
        Long memberId = memberService.signup(email, password, name, phone);
        Member member = memberService.findById(memberId);

        // then
        assertNotNull(memberId);
        assertEquals(email, member.getEmail());
        assertNotEquals(password, member.getPassword());
        assertTrue(passwordEncoder.matches(password, member.getPassword()));
        assertEquals(name, member.getName());
        assertEquals(phone, member.getPhone());
        assertEquals(MemberRole.USER, member.getRole());
        assertEquals(MemberStatus.ACTIVE, member.getStatus());
    }

    @Test
    void 회원가입_실패_이메일_중복() {

        // given
        String email = "aaa@abc.com";
        String password = "123123123";
        String name = "테스트";
        String phone = "010-1111-2222";

        // when
        memberService.signup(email, password, name, phone);

        // then
        ConflictException e = assertThrows(ConflictException.class,
                () -> memberService.signup(email, password, name, phone));
        assertEquals("이미 사용 중인 이메일입니다.", e.getMessage());
    }

    @Test
    void 회원_조회_성공_ID() {

        // given
        String email = "aaa@abc.com";
        String password = "123123123";
        String name = "테스트";
        String phone = "010-1111-2222";
        Long memberId = memberService.signup(email, password, name, phone);

        // when
        Member member = memberService.findById(memberId);

        // then
        assertEquals(memberId, member.getId());
        assertEquals("테스트", member.getName());
    }

    @Test
    void 회원_조회_실패_존재하지_않는_회원() {

        // given

        // when

        // then
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> memberService.findById(999L));
        assertEquals("회원이 존재하지 않습니다.", e.getMessage());
    }

    @Test
    void 회원_조회_성공_이메일() {

        // given
        String email = "aaa@abc.com";
        String password = "123123123";
        String name = "테스트";
        String phone = "010-1111-2222";
        Long memberId = memberService.signup(email, password, name, phone);

        // when
        Member member = memberService.findByEmail(email);

        // then
        assertEquals(1, member.getId());
        assertEquals("aaa@abc.com", member.getEmail());
    }

    @Test
    void 회원_조회_실패_이메일() {

        // given

        // when

        // then
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> memberService.findByEmail("ddd@ddd.com"));
        assertEquals("회원이 존재하지 않습니다.", e.getMessage());
    }

    @Test
    void 회원_수정_성공_이름_패스워드_전화번호() {

        // given
        String email = "aaa@abc.com";
        String password = "123123123";
        String name = "테스트";
        String phone = "010-1111-2222";
        Long memberId = memberService.signup(email, password, name, phone);
        String encodedPassword = memberService.findById(memberId).getPassword();

        String updatedName = "테스트2";
        String updatedPhone = "010-2222-3333";

        // when
        memberService.updateProfile(
                memberId,
                updatedName,
                null,
                updatedPhone
        );

        // then
        Member updated = memberService.findById(memberId);
        assertEquals("테스트2", updated.getName());
        assertEquals("010-2222-3333", updated.getPhone());
        assertEquals(encodedPassword, updated.getPassword());   // 비번 변경 X
    }

    @Test
    void 회원_수정_실패_회원_없음() {

        // given
        String updatedName = "업데이트테스트";
        String updatedPhone = "010-1111-2222";

        // when
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> memberService.updateProfile(
                        999L,
                        updatedName,
                        null,
                        updatedPhone
                )
        );

        // then
        assertEquals("회원이 존재하지 않습니다.", e.getMessage());
    }

    @Test
    void 회원_탈퇴_성공() {

        // given
        String email = "aaa@abc.com";
        String password = "123123123";
        String name = "테스트";
        String phone = "010-1111-2222";
        Long memberId = memberService.signup(email, password, name, phone);
        Member member = memberService.findById(memberId);

        // when
        memberService.deactivate(memberId);

        // then
        assertEquals(MemberStatus.INACTIVE, member.getStatus());
    }

    @Test
    void 회원_탈퇴_실패_회원_없음() {

        // given


        // when
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> memberService.deactivate(999L));

        // then
        assertEquals("회원이 존재하지 않습니다.", e.getMessage());
    }
}
