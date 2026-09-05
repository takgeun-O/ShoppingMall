package io.github.takgeun.shop.member.application;

import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.global.error.exception.UnauthorizedException;
import io.github.takgeun.shop.global.security.session.MemberSessionExpirationEvent;
import io.github.takgeun.shop.member.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static io.github.takgeun.shop.global.error.code.ErrorCode.INVALID_CURRENT_PASSWORD;
import static io.github.takgeun.shop.global.error.code.ErrorCode.PASSWORD_REUSE_NOT_ALLOWED;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

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
    public void updateProfile(
            Long memberId,
            String name,
            String phone
    ) {

        Member member = findMember(memberId);

        boolean memberChanged = false;

        if (name != null && !name.equals(member.getName())) {
            member.changeName(name);

            memberChanged = true;
        }

        if (phone != null && !phone.equals(member.getPhone())) {
            member.changePhone(phone);

            memberChanged = true;
        }

        if (!memberChanged) {
            return;
        }

        memberRepository.save(member);
    }

    @Transactional
    public void changePassword(
            Long memberId,
            String currentPassword,
            String newPassword
    ) {
        Member member = findMember(memberId);

        if (!passwordEncoder
                .matches(
                        currentPassword,
                        member.getPassword())
        ) {
            throw new UnauthorizedException(
                    INVALID_CURRENT_PASSWORD
            );
        }

        validateRawPassword(newPassword);

        if(passwordEncoder.matches(
                newPassword,
                member.getPassword()
        )) {
            throw new ConflictException(
                    PASSWORD_REUSE_NOT_ALLOWED
            );
        }

        member.changePassword(
                passwordEncoder.encode(newPassword)
        );

        memberRepository.save(member);

        publishSessionExpiration(memberId);
    }

    // 회원 탈퇴
    @Transactional
    public void deactivate(Long memberId) {
        Member member = findMember(memberId);

        member.deactivate();
        memberRepository.save(member);

        publishSessionExpiration(memberId);
    }

    @Transactional
    public void changeRole(Long memberId, MemberRole newRole) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }

        if (newRole == null) {
            throw new IllegalArgumentException("변경할 권한은 필수입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다."));

        if (member.getRole() == newRole) {
            return;
        }

        member.changeRole(newRole);
        memberRepository.save(member);

        publishSessionExpiration(memberId);
    }

    @Transactional
    public void changeStatus(Long memberId, MemberStatus newStatus) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("memberId는 양수여야 합니다.");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("newStatus는 필수입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        if (member.getStatus() == newStatus) {
            return;
        }

        member.changeStatus(newStatus);
        memberRepository.save(member);

        publishSessionExpiration(memberId);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(normalizeEmail(email));
    }

    /**
     * 인증 성공 후 최근 로그인 시각을 갱신한다.
     *
     * 비밀번호 검증은 AuthenticationManager와
     * DaoAuthenticationProvider가 담당한다.
     */
    @Transactional
    public void recordSuccessfulLogin(Long memberId) {
        Member member = findMember(memberId);

        member.updateLastLoginAt();

        memberRepository.save(member);
    }


    private void validateDuplicateEmail(String normalizedEmail) {
        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("이미 사용 중인 이메일입니다.");
        }
    }

    /**
     * DTO 검증 : 잘못된 HTTP 요청을 빠르게 400으로 거부
     * Service 검증 : 다른 Controller나 Initializer가 호출해도 정책 보호
     */
    private void validateRawPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("password는 필수입니다.");
        }
        if (password.length() < PasswordPolicy.MIN_LENGTH) {
            throw new IllegalArgumentException("password 길이는 8자 이상이어야 합니다.");
        }
        if (password.length() > PasswordPolicy.MAX_LENGTH) {
            throw new IllegalArgumentException("password 길이는 20자 이하이어야 합니다.");
        }
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다."));
    }

    // 이메일 정규화
    private String normalizeEmail(String email) {
        if (email == null || email.trim().isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void publishSessionExpiration(Long memberId) {
        /**
         * 발행 코드는 Spring에 다음과 같이 요청한다.
         * MemberSessionExpirationEvent 이벤트가 발생했으니
         * 이 이벤트를 처리하는 리스너를 찾아달라.
         *
         * 그러면 Spring은 애플리케이션 시작 시 등록해 둔 이벤트 리스너 중에서
         * MemberSessionExpirationEvent를 받는 메서드를 찾는다.
         *
         * 연결 기준은 리스너 메서드의 매개변수 타입!
         * public void handle(MemberSessionExpirationEvent event) {
         *         memberSessionService.expireAllByMemberId(event.memberId());
         *     }
         *
         * 리스너가 받는 매개변수 타입과 발행한 객체 타입이 일치하면
         * Spring이 handle()을 현재 트랜잭션에 실행 작업 등록 (phase 옵션에 따라 실행 시점 다름. 이 코드에선 아직 실행 안함)
         *
         * 이후 트랜잭션 정상 커밋되면 MemberSessionExpirationListener.handle(event) 실행
         */
        eventPublisher.publishEvent(
                new MemberSessionExpirationEvent(memberId)
        );
    }
}
