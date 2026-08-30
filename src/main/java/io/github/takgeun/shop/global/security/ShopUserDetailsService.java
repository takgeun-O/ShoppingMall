package io.github.takgeun.shop.global.security;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일로 회원을 조회하고 ShopUserPrincipal로 변환하는 역할
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // email이 null로 들어올 때 NullPointerException 방지
        if(email == null || email.trim().isBlank()) {
            throw new UsernameNotFoundException("이메일 또는 비밀번호가 올바르지 않습니다.");

        }
        String normalizedEmail = email.trim().toLowerCase();

        Member member = memberRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "이메일 또는 비밀번호가 올바르지 않습니다."));

        return new ShopUserPrincipal(
                member.getId(),
                member.getEmail(),
                member.getPassword(),   // DB에 저장된 BCrypt 해시
                member.getName(),
                member.getRole(),
                member.getStatus()
        );
    }
}
