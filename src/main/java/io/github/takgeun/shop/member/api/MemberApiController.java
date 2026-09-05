package io.github.takgeun.shop.member.api;

import io.github.takgeun.shop.global.api.ApiController;
import io.github.takgeun.shop.global.security.SecurityContextService;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.member.api.dto.request.MemberUpdateRequest;
import io.github.takgeun.shop.member.api.dto.request.MemberWithdrawalRequest;
import io.github.takgeun.shop.member.api.dto.request.PasswordChangeRequest;
import io.github.takgeun.shop.member.api.dto.response.MemberMeResponse;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@ApiController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberApiController {

    private final MemberService memberService;
    private final SecurityContextService securityContextService;

    @GetMapping("/me")
    public MemberMeResponse getMe(
            @AuthenticationPrincipal ShopUserPrincipal principal
    ) {
        Member member = memberService.findById(principal.getMemberId());

        return MemberMeResponse.from(member);
    }

    /**
     * 로그인한 회원의 프로필을 수정한다.
     * <p>
     * 요청 본문은 JSON을 통해 MemberUpdateRequest로 역직렬화된다.
     * 지원하지 않는 Content-Type 요청은
     * HttpMediaTypeNotSupportedException으로 처리한다.
     */
    @PatchMapping(
            value = "/me"
    )
    public MemberMeResponse updateMe(
            @AuthenticationPrincipal ShopUserPrincipal principal,
            @Valid @RequestBody MemberUpdateRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {

        Long memberId = principal.getMemberId();

        memberService.updateProfile(
                memberId,
                request.name(),
                request.phone()
        );

        // 수정 완료 후 최신 회원 정보를 여기서 재조회
        Member updatedMember = memberService.findById(memberId);

        /**
         * DB에서 변경된 회원 정보를 현재 SecurityContext에 반영한다.
         *
         * 로그인할 때 생성된 ShopUserPrincipal은 DB 회원 정보의 복사본이다.
         * Spring Security는 DB 변경을 자동으로 감지해서 principal을 다시 만들어주지 않는다.
         * 따라서 아래와 같이 ShopUserPrincipal과 Authentication을 새로 만들고
         * 기존 SecurityContext를 갱신한다.
         */
        securityContextService.refreshPrincipal(
                updatedMember,
                httpRequest,
                httpResponse
        );

        return MemberMeResponse.from(updatedMember);
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal ShopUserPrincipal principal,

            @Valid @RequestBody
            PasswordChangeRequest request
    ) {
        memberService.changePassword(
                principal.getMemberId(),
                request.currentPassword(),
                request.newPassword()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(
            value = "/me",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> withdrawMe(
            @AuthenticationPrincipal ShopUserPrincipal principal,
            @Valid @RequestBody MemberWithdrawalRequest request
    ) {
        memberService.withdraw(
                principal.getMemberId(),
                request.currentPassword()
        );

        return ResponseEntity.noContent().build();
    }
}
