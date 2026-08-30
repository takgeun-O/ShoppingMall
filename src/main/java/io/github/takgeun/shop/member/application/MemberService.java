package io.github.takgeun.shop.member.application;

import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRepository;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    @Transactional
    public Long signup(String email, String password, String name, String phone) {

        String normalizedEmail = normalizeEmail(email);

        validateDuplicateEmail(normalizedEmail);
        validateRawPassword(password);

        String encodedPassword = passwordEncoder.encode(password);
        Member member = Member.create(
                normalizedEmail,
                encodedPassword,
                name,
                phone
        );
        return memberRepository.save(member).getId();
    }

    // 회원 조회
    public Member findById(Long memberId) {
        return findMember(memberId);
    }

    public Member findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return memberRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다."));
    }

    // 내 정보 수정 (패스워드 변경은 다른 곳에서 할 예정)
    @Transactional
    public void updateProfile(Long memberId, String name, String password, String phone) {

        Member member = findMember(memberId);

        if(password != null) {
            validateRawPassword(password);
            member.changePassword(passwordEncoder.encode(password));
        }
        if(name != null) {
            member.changeName(name);
        }
        if(phone != null) {
            member.changePhone(phone);
        }
        memberRepository.save(member);
    }

    // 회원 탈퇴
    @Transactional
    public void deactivate(Long memberId) {
        Member member = findMember(memberId);
        member.deactivate();
        memberRepository.save(member);
    }

    @Transactional
    public void changeRole(Long memberId, MemberRole newRole) {
        if(memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }

        if(newRole == null) {
            throw new IllegalArgumentException("변경할 권한은 필수입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다."));

        member.changeRole(newRole);

        memberRepository.save(member);
    }

    @Transactional
    public void changeStatus(Long memberId, MemberStatus newStatus) {
        if(memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("memberId는 양수여야 합니다.");
        }
        if(newStatus == null) {
            throw new IllegalArgumentException("newStatus는 필수입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        member.changeStatus(newStatus);
        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(normalizeEmail(email));
    }

    /**
     * AuthService.login() 에서 담당하는 역할 중 하나인
     * 최근 로그인 시각 갱신을 MemberService 책임으로 옮김.
     * (Spring Security 전환하면서 AuthService는 끌꺼니까)
     */
    @Transactional
    public void recordSuccessfulLogin(Long memberId) {
        Member member = findMember(memberId);

        member.updateLastLoginAt();

        memberRepository.save(member);
    }


    private void validateDuplicateEmail(String normalizedEmail) {
        if(memberRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("이미 사용 중인 이메일입니다.");
        }
    }

    private void validateRawPassword(String password) {
        if(password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("password는 필수입니다.");
        }
        if(password.length() < 8) {
            throw new IllegalArgumentException("password 길이는 8자 이상이어야 합니다.");
        }
        if(password.length() > 20) {
            throw new IllegalArgumentException("password 길이는 20자 이하이어야 합니다.");
        }
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다."));
    }

    // 이메일 정규화
    private String normalizeEmail(String email) {
        if(email == null || email.trim().isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        return email.trim().toLowerCase();
    }
}
