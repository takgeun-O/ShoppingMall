package io.github.takgeun.shop.member.application;

import io.github.takgeun.shop.global.error.ForbiddenException;
import io.github.takgeun.shop.global.error.UnauthorizedException;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 로그인 정책 담당
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;

    public Long login(String email, String password) {
        // 존재 여부를 구체적으로 노출하지 않도록 하기 위해 이메일이 틀린거랑 비밀번호 틀린거 예외 메시지 통일
        String normalized = email.trim().toLowerCase();
        Member member = memberRepository.findByEmail(normalized)
                .orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 비밀번호 불일치
        if(!member.getPassword().equals(password)) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if(!member.isActive()) {
            throw new ForbiddenException("비활성화된 회원입니다.");
        }

        return member.getId();
    }
}
