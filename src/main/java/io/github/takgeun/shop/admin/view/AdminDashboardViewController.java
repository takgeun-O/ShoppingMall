package io.github.takgeun.shop.admin.view;

import io.github.takgeun.shop.admin.application.AdminDashboardQueryService;
import io.github.takgeun.shop.admin.view.dto.AdminDashboardView;
import io.github.takgeun.shop.global.error.exception.ForbiddenException;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.global.view.ViewController;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ViewController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminDashboardViewController {

    private final AdminDashboardQueryService adminDashboardQueryService;
    private final MemberService memberService;

    @GetMapping
    public String dashboard(Model model,
                            HttpServletRequest request
    ) {

        HttpSession session = request.getSession(false);
        requireAdmin(session);

        AdminDashboardView dashboard = adminDashboardQueryService.getDashboard();
        model.addAttribute("dashboard", dashboard);
        return "admin/dashboard";
    }

    private void requireAdmin(HttpSession session) {
        if (session == null) {
            throw new ForbiddenException("로그인이 필요합니다.");
        }

        Object loginMemberIdObj = session.getAttribute(SessionConst.LOGIN_MEMBER_ID);
        if(!(loginMemberIdObj instanceof Long memberId)) {
            throw new ForbiddenException("로그인이 필요합니다.");
        }

        Object roleObj = session.getAttribute(SessionConst.LOGIN_ROLE);
        if (!(roleObj instanceof MemberRole role) || role != MemberRole.ADMIN) {
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }

        Member loginMember = memberService.findById(memberId);
        if(loginMember.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("비활성 상태의 계정입니다.");
        }
    }
}
