package io.github.takgeun.shop.admin.view;

import io.github.takgeun.shop.admin.application.AdminDashboardQueryService;
import io.github.takgeun.shop.admin.view.dto.AdminDashboardView;
import io.github.takgeun.shop.global.error.ForbiddenException;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.MemberRole;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminDashboardViewController {

    private final AdminDashboardQueryService adminDashboardQueryService;

    @GetMapping
    public String dashboard(Model model,
                            HttpSession session
    ) {

        requireAdmin(session);

        AdminDashboardView dashboard = adminDashboardQueryService.getDashboard();
        model.addAttribute("dashboard", dashboard);
        return "admin/dashboard";
    }

    private void requireAdmin(HttpSession session) {
        if (session == null) {
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }
        Object roleObj = session.getAttribute(SessionConst.LOGIN_ROLE);
        if (!(roleObj instanceof MemberRole role) || role != MemberRole.ADMIN) {
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }
    }
}
