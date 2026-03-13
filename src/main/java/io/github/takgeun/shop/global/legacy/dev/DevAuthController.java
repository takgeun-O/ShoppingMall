package io.github.takgeun.shop.global.legacy.dev;

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
        Member member = memberService.findById(memberId);
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, memberId);
        session.setAttribute(SessionConst.LOGIN_ROLE, member.getRole());
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String devLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/login-user-test")
    public String loginUserTest(HttpSession session) {
        Member member = memberService.findByEmail("test1@test.com");
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, member.getId());
        session.setAttribute(SessionConst.LOGIN_ROLE, member.getRole());
        return "redirect:/products";
    }

    @GetMapping("/login-admin-test")
    public String loginAdminTest(HttpSession session) {
        Member member = memberService.findByEmail("testAdmin1@test.com");
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, member.getId());
        session.setAttribute(SessionConst.LOGIN_ROLE, member.getRole());
        return "redirect:/admin/products";
    }

    @GetMapping("/logout-test")
    public String logoutTest(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
