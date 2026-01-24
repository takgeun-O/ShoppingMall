package io.github.takgeun.shop.member.api;

import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.dto.request.MemberRoleUpdateRequest;
import io.github.takgeun.shop.member.dto.request.MemberStatusUpdateRequest;
import io.github.takgeun.shop.member.dto.response.MemberResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/members")
public class AdminMemberController {

    private final MemberService memberService;

    // 회원 단건 조회 -> GET /admin/members/{memberId} -> 200 OK
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponse> get(
            @PathVariable
            @NotNull(message = "memberId는 필수입니다.")
            @Positive(message = "memberId는 양수여야 합니다.") Long memberId
    ) {
        Member member = memberService.get(memberId);
        return ResponseEntity.ok(MemberResponse.from(member));
    }

    // 회원 목록 조회 -> GET /admin/members -> 200 OK
    @GetMapping
    public ResponseEntity<List<MemberResponse>> getAll() {
        List<MemberResponse> result = memberService.getAll().stream()
                .map(MemberResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    // 회원 상태 변경 -> PATCH /admin/members/{memberId}/status -> 204 No Content
    @PatchMapping("/{memberId}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable
            @NotNull(message = "memberId는 필수입니다.")
            @Positive(message = "memberId는 양수여야 합니다.") Long memberId,
            @Valid @RequestBody MemberStatusUpdateRequest request
    ) {
        memberService.changeStatus(memberId, request.getStatus());
        return ResponseEntity.noContent().build();
    }

    // 권한 변경(USER <-> ADMIN) -> PATCH /admin/members/{memberId}/role -> 204 No Content
    @PatchMapping("/{memberId}/role")
    public ResponseEntity<Void> changeRole(
            @PathVariable
            @NotNull(message = "memberId는 필수입니다.")
            @Positive(message = "memberId는 양수여야 합니다.") Long memberId,
            @Valid @RequestBody MemberRoleUpdateRequest request
    ) {
        memberService.changeRole(memberId, request.getRole());
        return ResponseEntity.noContent().build();
    }
}
