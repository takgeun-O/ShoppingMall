package io.github.takgeun.shop.member.application;

import io.github.takgeun.shop.global.error.code.ErrorCode;
import io.github.takgeun.shop.global.error.exception.BusinessException;
import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.global.security.session.MemberSessionExpirationEvent;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.member.infra.memory.MemoryMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static io.github.takgeun.shop.global.error.code.ErrorCode.PASSWORD_REUSE_NOT_ALLOWED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
        assertEquals(memberId, member.getId());
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
    void 회원_수정_성공_이름과_전화번호() {

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

    @Test
    void 회원_수정_성공_이름만_변경() {

        // given
        Long memberId = memberService.signup(
                "name@test.com",
                "pw123123!",
                "기존이름",
                "010-1111-2222"
        );

        // when
        memberService.updateProfile(
                memberId,
                "변경된이름",
                null
        );

        // then
        Member updatedMember = memberService.findById(memberId);

        assertEquals("변경된이름", updatedMember.getName());
        assertEquals("010-1111-2222", updatedMember.getPhone());
    }

    @Test
    void 회원_수정_성공_전화번호만_변경() {

        // given
        Long memberId = memberService.signup(
                "phone@test.com",
                "pw123123!",
                "기존이름",
                "010-1111-2222"
        );

        // when
        memberService.updateProfile(
                memberId,
                null,
                "010-9999-9999"
        );

        // then
        Member updatedMember = memberService.findById(memberId);

        assertEquals("기존이름", updatedMember.getName());
        assertEquals("010-9999-9999", updatedMember.getPhone());
    }

    @Test
    void 회원_수정값이_모두_null이면_기존정보를_유지한다() {

        // given
        Long memberId = memberService.signup(
                "null@test.com",
                "pw123123!",
                "기존이름",
                "010-1111-2222"
        );

        Member originalMember = memberService.findById(memberId);
        String originalPassword = originalMember.getPassword();

        // when
        memberService.updateProfile(
                memberId,
                null,
                null
        );

        // then
        Member updatedMember = memberService.findById(memberId);

        assertEquals("기존이름", updatedMember.getName());
        assertEquals("010-1111-2222", updatedMember.getPhone());
        assertEquals(originalPassword, originalMember.getPassword());
    }

    @Test
    void 회원_수정값이_기존값과_같으면_정보를_유지한다() {

        // given
        Long memberId = memberService.signup(
                "same@test.com",
                "123123123",
                "기존이름",
                "010-1111-2222"
        );

        // when
        memberService.updateProfile(
                memberId,
                "기존이름",
                "010-1111-2222"
        );

        // then
        Member unchanged =
                memberService.findById(memberId);

        assertEquals("기존이름", unchanged.getName());
        assertEquals("010-1111-2222", unchanged.getPhone());
    }

    @Test
    void 회원_수정은_세션만료_이벤트를_발행하지_않는다() {

        // given
        Long memberId = memberService.signup(
                "session@test.com",
                "123123123",
                "기존이름",
                "010-1111-2222"
        );

        // when
        memberService.updateProfile(
                memberId,
                "변경된이름",
                "010-2222-2222"
        );

        // then
        Mockito.verifyNoInteractions(eventPublisher);
    }

    @Test
    void 현재_비밀번호가_일치하면_새_비밀번호로_변경한다() {

        // given
        Long memberId = memberService.signup(
                "member@test.com",
                "current-password",
                "회원",
                "010-1111-2222"
        );

        // when
        memberService.changePassword(
                memberId,
                "current-password",
                "changed-password"
        );

        // then
        Member findMember = memberService.findById(memberId);

        assertThat(passwordEncoder.matches(
                "changed-password",
                findMember.getPassword()
        )).isTrue();

        assertThat(passwordEncoder.matches(
                "current-password",
                findMember.getPassword()
        )).isFalse();

        verify(eventPublisher).publishEvent(
                any(MemberSessionExpirationEvent.class)
        );
    }

    @Test
    void 현재_비밀번호가_일치하지_않으면_변경하지_않는다() {

        // given
        String currentPassword = "current-password";
        String newPassword = "changed-password";

        Long memberId = memberService.signup(
                "member@test.com",
                currentPassword,
                "회원",
                "010-1111-2222"
        );

        // when
        BusinessException exception =
                catchThrowableOfType(
                        () -> memberService.changePassword(
                                memberId,
                                "wrong-password",
                                newPassword
                        ),
                        BusinessException.class
                );

        // then : 오류 코드
        assertThat(exception.getErrorCode())
                .isEqualTo(
                        ErrorCode.INVALID_CURRENT_PASSWORD
                );

        // then : 기존 비밀번호 유지
        Member member = memberService.findById(memberId);

        assertThat(passwordEncoder.matches(
                currentPassword,
                member.getPassword()
        )).isTrue();

        assertThat(passwordEncoder.matches(
                newPassword,
                member.getPassword()
        )).isFalse();

        verify(eventPublisher, never())
                .publishEvent(any());
    }

    @Test
    void 새_비밀번호가_현재_비밀번호와_같으면_변경_실패() {

        // given
        String currentPassword = "current-password";

        Long memberId = memberService.signup(
                "member@test.com",
                currentPassword,
                "회원",
                "010-1111-2222"
        );

        // when & then
        BusinessException exception =
                catchThrowableOfType(
                        () -> memberService.changePassword(
                                memberId,
                                currentPassword,
                                currentPassword
                        ),
                        BusinessException.class
                );

        Member findMember = memberService.findById(memberId);

        assertThat(exception.getErrorCode())
                .isEqualTo(PASSWORD_REUSE_NOT_ALLOWED);

        assertThat(passwordEncoder.matches(
                currentPassword,
                findMember.getPassword()
        )).isTrue();

        verify(eventPublisher, never())
                .publishEvent(any());
    }
}
