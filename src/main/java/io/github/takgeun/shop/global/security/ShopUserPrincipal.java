package io.github.takgeun.shop.global.security;

import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 회원 인증 객체 설계
 * 기존 Member를 직접 userDetails로 만들기보다는 별도 어댑터를 두는 것이 좋음.
 *  Spring Security의 인증 객체와 쇼핑몰의 Member 도메인 객체는 책임이 다르기 때문
 *
 *  UserDetails의 역할
 *  - 로그인 아이디가 무엇인가?
 *  - 암호화된 비밀번호가 무엇인가?
 *  - 어떤 권한을 가졌는가?
 *  - 계정이 활성 상태인가?
 *  - 계정이 잠겼거나 만료됐는가?
 *
 *  Member implements UserDetails 로 만들 경우
 *  도메인 계층이 Spring Security 프레임워크에 직접 의존하고
 *  그 결과 Member 안에 인증 프레임워크때문에 필요한 메서드들이 들어온다.
 *  쇼핑몰 도메인에 계정 만료나 자격 증명 만료라는 개념 자체가 있을 수 없는데
 *  Spring Security 인터페이스를 구현하기 위해 의미 없는 메서드와 고정값을 넣어야 하는 상황 발생
 *  -> 도메인 객체가 자신의 비즈니스 규칙이 아니라 프레임워크 계약까지 책임지는 구조가 됨.
 *
 *  ShopUserPrincipal 객체는 도메인 Member를 Spring Security가 이해할 수 있는 형태로 변환하는 어댑터 역할을 함.
 *
 *  로그인 요청
 *  -> Spring Security
 *  -> CustomUserDetailsService
 *  -> MemberRepository
 *  -> Member 조회
 *  -> SecurityMemberPrincipal.from(member)
 *  -> PasswordEncoder로 비밀번호 검증
 *  -> Authentication 생성
 *  -> SecurityContext에 저장
 */
@Getter
public class ShopUserPrincipal implements UserDetails {

    /**
     * Member 전체를 필드로 들고 있으면 편한데 왜 일부만 가져오지? private final Member member;
     * 현재 프로젝트는 MyBatis 기반이고 이후 JPA 전환 예정임.
     * JPA Entity 전체를 Security 세션에 저장하면 다음 문제가 생김
     * - 세션에 불필요하게 많은 정보가 저장됨
     * - 지연 로딩 프록시와 세션 직렬화 문제
     * - Entity 변경 내용과 인증 세션 정보가 즉시 일치하지 않음
     * - 비밀번호 등 민감한 값이 포함된 객체의 수명이 길어짐
     * - Security 객체가 영속성 컨텍스트 밖에서도 Entity에 의존
     * 따라서 인증 시점에 꼭 필요한 값만 복사해서 불변 인증 객체로 만드는 것이 안전함.
     */
    private final Long memberId;
    private final String email;
    private final String password;
    private final String name;
    private final MemberRole role;
    private final MemberStatus status;

    public ShopUserPrincipal(Long memberId, String email, String password, String name, MemberRole role, MemberStatus status) {
        this.memberId = memberId;
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.status = status;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    @Override
    public String getUsername() {
        // 여기서 말하는 username은 사용자 이름이 아니라 로그인할 때 사용하는 고유 식별자를 의미함
        return email;
    }

    @Override
    public boolean isEnabled() {
        return status == MemberStatus.ACTIVE;
    }
}
