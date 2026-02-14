package io.github.takgeun.shop.global.dev;

import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Profile("local")
@Controller
@RequiredArgsConstructor
@RequestMapping("/dev")
public class DevAuthController {

    private final MemberService memberService;

    @GetMapping("/login/{memberId}")
    public String devLogin(@PathVariable Long memberId, HttpSession session) {
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, memberId);
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String devLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/login-test")
    public String loginTest(HttpSession session) {
        Member member = memberService.getByEmail("test1@test.com");
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, member.getId());
        return "redirect:/products";
    }

    @GetMapping("/logout-test")
    public String logoutTest(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
