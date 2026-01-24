package io.github.takgeun.shop.member.api;

/* HTTP 요청을 받아 Service에 위임하고 HTTP 응답으로 변환해서 돌려주는 역할
 * 비즈니스 로직 X
 * 검증/흐름 제어 O
 * 도메인 조작 X*/

import io.github.takgeun.shop.global.error.UnauthorizedException;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.dto.request.MemberSignupRequest;
import io.github.takgeun.shop.member.dto.request.MemberUpdateRequest;
import io.github.takgeun.shop.member.dto.response.MemberResponse;
import io.github.takgeun.shop.member.dto.response.MemberSignupResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated              // @RequestParam 이나 @PathVariable 검증할 때 필요
@RestController                     // HTTP 요청을 처리하는데 반환값을 View가 아니라 JSON(Response Body) 로 보내고자 하는 의도
@RequiredArgsConstructor            // 필수 의존성만 받는 생성자를 자동으로 만들어주는 어노테이션
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    // 회원가입
    @PostMapping
    public ResponseEntity<MemberSignupResponse> signup(@Valid @RequestBody MemberSignupRequest request) {
        Long id = memberService.signup(
                request.getEmail(),
                request.getPassword(),
                request.getName(),
                request.getPhone()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MemberSignupResponse(id));
    }

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
    @PatchMapping("/me")
    public ResponseEntity<Void> updateMe(
            @SessionAttribute(name = SessionConst.LOGIN_MEMBER_ID, required = false) Long memberId,
            @Valid @RequestBody MemberUpdateRequest request
    ) {
        if(memberId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        String name = request.getName();
        String password = request.getPassword();
        String phone = request.getPhone();

        memberService.updateProfile(memberId, name, password, phone);
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
