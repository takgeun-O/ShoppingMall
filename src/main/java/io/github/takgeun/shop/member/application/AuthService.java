package io.github.takgeun.shop.member.application;

import io.github.takgeun.shop.global.error.exception.ForbiddenException;
import io.github.takgeun.shop.global.error.exception.UnauthorizedException;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 로그인 정책 담당
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 이메일로 회원 조회
     * 비밀번호 검증
     * 할성 회원 검증
     * lastLoginAt 갱신
     */
    @Transactional
    public Long login(String email, String password) {

        String normalizedEmail = normalizeEmail(email);
        validatePasswordInput(password);

        // 존재 여부를 구체적으로 노출하지 않도록 하기 위해 이메일이 틀린거랑 비밀번호 틀린거 예외 메시지 통일
        Member member = memberRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if(!passwordEncoder.matches(password, member.getPassword())) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if(!member.isActive()) {
            throw new ForbiddenException("비활성화된 회원입니다.");
        }

        member.updateLastLoginAt();
        memberRepository.save(member);
        return member.getId();
    }

    private String normalizeEmail(String email) {
        if(email == null || email.trim().isEmpty()) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return email.trim().toLowerCase();
    }

    private void validatePasswordInput(String password) {
        if(password == null || password.isEmpty()) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
    }
}
