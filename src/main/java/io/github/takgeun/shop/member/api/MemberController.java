package io.github.takgeun.shop.member.api;

/* HTTP 요청을 받아 Service에 위임하고 HTTP 응답으로 변환해서 돌려주는 역할
 * 비즈니스 로직 X
 * 검증/흐름 제어 O
 * 도메인 조작 X*/

import io.github.takgeun.shop.global.error.UnauthorizedException;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.global.validation.SignupValidationSequence;
import io.github.takgeun.shop.member.api.dto.request.MemberPasswordUpdateRequest;
import io.github.takgeun.shop.member.api.dto.request.MemberProfileUpdateRequest;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.api.dto.request.MemberUpdateRequest;
import io.github.takgeun.shop.member.api.dto.response.MemberResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated              // @RequestParam 이나 @PathVariable 검증할 때 필요
@RestController                     // HTTP 요청을 처리하는데 반환값을 View가 아니라 JSON(Response Body) 로 보내고자 하는 의도
@RequiredArgsConstructor            // 필수 의존성만 받는 생성자를 자동으로 만들어주는 어노테이션
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    // 내 정보 조회 (세션 기반)
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMe(
            @SessionAttribute(name = SessionConst.LOGIN_MEMBER_ID, required = false) Long memberId
    ) {
        if(memberId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        return ResponseEntity.ok(MemberResponse.from(memberService.get(memberId)));
    }

    // 내 정보 수정 (PATCH, 세션 기반)
    @PatchMapping("/me/profile")
    public ResponseEntity<Void> updateMyProfile(
            @SessionAttribute(name = SessionConst.LOGIN_MEMBER_ID, required = false) Long memberId,
            @Validated(SignupValidationSequence.class) @RequestBody MemberProfileUpdateRequest request
    ) {
        if(memberId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        memberService.updateProfile(memberId, request.getName(), null, request.getPhone());
        return ResponseEntity.noContent().build();
    }

    // 비밀번호 변경 (별도 화면, 별도 API)
    @PatchMapping("/me/password")
    public ResponseEntity<Void> updateMyPassword(
            @SessionAttribute(name = SessionConst.LOGIN_MEMBER_ID, required = false) Long memberId,
            @Validated @RequestBody MemberPasswordUpdateRequest request
            ) {
        if(memberId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        // name, phone은 건드리지 않고 password만 변경
        memberService.updateProfile(memberId, null, request.getPassword(), null);
        return ResponseEntity.noContent().build();
    }

    // 회원 탈퇴(비활성화)
    @DeleteMapping("/me")
    public ResponseEntity<Void> deactivateMe(
            @SessionAttribute(name = SessionConst.LOGIN_MEMBER_ID, required = false) Long memberId,
            HttpSession session
    ) {
        if(memberId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        memberService.deactivate(memberId);
        session.invalidate();       // 탈퇴했으면 세션 끊기
        return ResponseEntity.noContent().build();
    }
}
