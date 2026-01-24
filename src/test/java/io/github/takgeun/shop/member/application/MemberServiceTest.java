package io.github.takgeun.shop.member.application;

import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.member.dto.request.MemberUpdateRequest;
import io.github.takgeun.shop.member.infra.MemoryMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemberServiceTest {

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        MemoryMemberRepository memberRepository = new MemoryMemberRepository();

        memberService = new MemberService(memberRepository);
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
        Member member = memberService.get(memberId);

        // then
        assertNotNull(memberId);
        assertEquals("aaa@abc.com", member.getEmail());
        assertEquals("123123123", member.getPassword());
        assertEquals("테스트", member.getName());
        assertEquals("010-1111-2222", member.getPhone());
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
        Long memberId = memberService.signup(email, password, name, phone);

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
        Member member = memberService.get(memberId);

        // then
        assertEquals(1, member.getId());
        assertEquals("테스트", member.getName());
    }

    @Test
    void 회원_조회_실패_존재하지_않는_회원() {

        // given

        // when

        // then
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> memberService.get(999L));
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
        Member member = memberService.getByEmail(email);

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
                () -> memberService.getByEmail("ddd@ddd.com"));
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

        MemberUpdateRequest request = MemberUpdateRequest.of(
                "9999999999",
                "테스트2",
                "010-2222-3333"
        );

        // when
        memberService.updateProfile(
                memberId,
                request.getName(),
                request.getPassword(),
                request.getPhone()
        );

        // then
        Member updated = memberService.get(memberId);
        assertEquals("9999999999", updated.getPassword());
//        assertEquals("테스트2", updated.getName());
        assertEquals("010-2222-3333", updated.getPhone());
    }

    @Test
    void 회원_수정_실패_회원_없음() {

        // given
        String email = "aaa@abc.com";
        String password = "123123123";
        String name = "테스트";
        String phone = "010-1111-2222";
        Long memberId = memberService.signup(email, password, name, phone);

        MemberUpdateRequest request = MemberUpdateRequest.of(
                "22222222",
                "업데이트테스트",
                "010-1111-2222"
        );

        // when
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> memberService.updateProfile(
                        999L,
                        request.getName(),
                        request.getPassword(),
                        request.getPhone())
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
        Member member = memberService.get(memberId);

        // when
        memberService.deactivate(memberId);

        // then
        assertEquals(MemberStatus.DEACTIVATED, member.getStatus());
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