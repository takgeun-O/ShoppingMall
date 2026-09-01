package io.github.takgeun.shop.admin.view;

import io.github.takgeun.shop.admin.application.AdminDashboardQueryService;
import io.github.takgeun.shop.admin.view.dto.AdminDashboardView;
import io.github.takgeun.shop.global.view.ViewController;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ViewController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminDashboardViewController {

    private final AdminDashboardQueryService adminDashboardQueryService;

    /**
     * 관리자 대시보드
     *
     * 접근 권한은 SecurityConfig에서 처리한다.
     * - 비로그인 사용자: 로그인 화면으로 리다이렉트
     * - 일반 사용자: 403 오류 확인
     * - ROLE_ADMIN: 컨트롤러 진입 허용
     */
    @GetMapping
    public String dashboard(
            Model model
    ) {

        AdminDashboardView dashboard = adminDashboardQueryService.getDashboard();
        model.addAttribute("dashboard", dashboard);
        return "admin/dashboard";
    }
}
