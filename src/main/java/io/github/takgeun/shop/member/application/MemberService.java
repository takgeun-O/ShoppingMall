package io.github.takgeun.shop.member.application;

import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRepository;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    // 회원가입 (유스케이스 UC-M01 구현)
    public Long signup(String email, String password, String name, String phone) {

        String normalizedEmail = normalizeEmail(email);

        validateDuplicateEmail(normalizedEmail);

        // TODO(v2) : password는 인코딩 후 저장
        Member member = Member.create(normalizedEmail, password, name, phone);
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

    // 내 정보 수정 (UC-M05) - PATCH
    public void updateProfile(Long memberId, String name, String password, String phone) {

        Member member = findMember(memberId);

        if(password != null) {
            member.changePassword(password);
        }
        if(name != null) {
            member.changeName(name);
        }
        if(phone != null) {
            member.changePhone(phone);
        }
        memberRepository.save(member);      // 메모리 저장소니까 save를 명시 호출
    }

    // 회원 탈퇴 (UC-M06) - 비활성화
    public void deactivate(Long memberId) {
        Member member = findMember(memberId);
        member.deactivate();
        memberRepository.save(member);
    }

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




    private void validateDuplicateEmail(String normalizedEmail) {
        if(memberRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("이미 사용 중인 이메일입니다.");
        }
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다."));
    }

    // 이메일 정규화
    private String normalizeEmail(String email) {
        if(email == null) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        return email.trim().toLowerCase();
    }
}
