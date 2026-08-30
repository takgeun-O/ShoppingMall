package io.github.takgeun.shop.global.security;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRepository;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ShopUserDetailsServiceTest {

    private MemberRepository memberRepository;
    private ShopUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        userDetailsService = new ShopUserDetailsService(memberRepository);
    }

    @Test
    void 이메일로_회원을_조회해_Principal로_변환한다() {

        // given
        Member member = mock(Member.class);     // mock을 써서 생성한거라 member에는 아무 정보가 들어있지 않음. (Member처럼 행동할 수 있는 테스트용 대역)

        when(member.getId()).thenReturn(1L);
        when(member.getEmail()).thenReturn("user@test.com");
        when(member.getPassword()).thenReturn("{bcrypt}encoded");
        when(member.getName()).thenReturn("테스트");
        when(member.getRole()).thenReturn(MemberRole.USER);
        when(member.getStatus()).thenReturn(MemberStatus.ACTIVE);

        when(memberRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(member));

        // when
        UserDetails result = userDetailsService.loadUserByUsername(" USER@Test.com ");

        // then
        assertThat(result)
                .isInstanceOf(ShopUserPrincipal.class);

        ShopUserPrincipal principal = (ShopUserPrincipal) result;

        assertThat(principal.getMemberId()).isEqualTo(1L);
        assertThat(principal.getUsername())
                .isEqualTo("user@test.com");
        assertThat(principal.getPassword())
                .isEqualTo("{bcrypt}encoded");
        assertThat(principal.getName()).isEqualTo("테스트");
        assertThat(principal.getRole())
                .isEqualTo(MemberRole.USER);
        assertThat(principal.isEnabled()).isTrue();

        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        verify(memberRepository)
                .findByEmail("user@test.com");
    }

    @Test
    void 존재하지_않는_이메일이면_예외가_발생한다() {

        // given
        when(memberRepository.findByEmail("none@test.com"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                userDetailsService.loadUserByUsername("none@test.com")
        )
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    void 이메일이_null이면_회원_조회를_시도하지_않는다() {

        assertThatThrownBy(() ->
                userDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

        verifyNoInteractions(memberRepository);
    }

    @Test
    void 이메일이_공백이면_회원_조회를_시도하지_않는다() {

        assertThatThrownBy(() ->
                userDetailsService.loadUserByUsername(" "))
                .isInstanceOf(UsernameNotFoundException.class);

        verifyNoInteractions(memberRepository);
    }
}