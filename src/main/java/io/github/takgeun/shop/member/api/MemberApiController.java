package io.github.takgeun.shop.member.api;

import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.member.api.dto.response.MemberMeResponse;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberApiController {

    private final MemberService memberService;

    @GetMapping("/me")
    public MemberMeResponse getMe(
            @AuthenticationPrincipal ShopUserPrincipal principal
    ) {
        Member member = memberService.findById(principal.getMemberId());

        return MemberMeResponse.from(member);
    }
}
