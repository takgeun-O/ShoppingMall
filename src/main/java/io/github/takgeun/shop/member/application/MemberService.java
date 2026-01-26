package io.github.takgeun.shop.member.application;

import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRepository;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.member.dto.request.MemberUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    // 회원가입, 로그인, 내 정보 조회, 내 정보 수정, 회원탈퇴(비활성화)
    // Service는 비즈니스 규칙과 유스케이스 구현을 작성한다.
    // Controller -> MemberService -> MemberRepository 경로로 저장소 접근하기

    private final MemberRepository memberRepository;

    // 회원가입 (유스케이스 UC-M01 구현)
    public Long signup(String email, String password, String name, String phone) {
        // 기존 이메일 존재 검증
        String normalizedEmail = normalizeEmail(email);
        if(memberRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("이미 사용 중인 이메일입니다.");
        }

        // TODO(v2) : password는 인코딩 후 저장
        Member member = Member.create(normalizedEmail, password, name, phone);
        return memberRepository.save(member).getId();
    }

    // 로그인 (도메인 접근 규칙 구현)
    // 로그인은 단순히 행위로만 보기보다는 실제로 인증 + 검증 + 상태 판단 + 세션 생성 으로 봐야함.
    // Service는 HTTP/세션을 모름 -> 로그인 결과로 무엇을 할 지는 Controller 책임 -> 따라서 Service는 인증 가능 여부 판단만 한다.
    public Member get(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다."));
    }

    public Member getByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return memberRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다."));
    }

    public List<Member> getAll() {
        return memberRepository.findAll();
    }

    // 내 정보 수정 (UC-M05) - PATCH
    public void updateProfile(Long memberId, String name, String password, String phone) {

        // 변경하려는 회원이 존재하지 않을 때 예외 처리
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다."));

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

    // 회원 상태 변경
    public void changeStatus(Long memberId, MemberStatus status) {

        if(status == null) {
            throw new IllegalArgumentException("status는 필수입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다."));

        switch (status) {
            case ACTIVE -> member.activate();
            case INACTIVE -> member.deactivate();
            default -> throw new IllegalArgumentException("지원하지 않는 status 입니다.");
        }

        memberRepository.save(member);
    }

    // 회원 권한 변경
    public void changeRole(Long memberId, MemberRole role) {

        if(role == null) {
            throw new IllegalArgumentException("role은 필수입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다."));

        member.changeRole(role);

        memberRepository.save(member);
    }

    // 회원 탈퇴 (UC-M06) - 비활성화
    public void deactivate(Long memberId) {
        Member member = memberRepository.findById(memberId)
                        .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다."));
        member.deactivate();
        memberRepository.save(member);
    }

    // 이메일 정규화
    private String normalizeEmail(String email) {
        if(email == null) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        return email.trim().toLowerCase();
    }
}
