package io.github.takgeun.shop.global.view;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.MemberRole;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewModelAdvice {

    private final CategoryService categoryService;

    public GlobalViewModelAdvice(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * @ModelAttribute : 컨트롤러 실행 전에 Model에 값을 자동으로 추가
     * 해당 메서드의 반환값을 model.addAttribute("loginMemberId", 반환값); 처럼 자동으로 넣어준다.
     */
    @ModelAttribute
    public void global(HttpSession session, Model model) {
        model.addAttribute("loginMemberId", session.getAttribute(SessionConst.LOGIN_MEMBER_ID));
        model.addAttribute("loginMemberName", session.getAttribute(SessionConst.LOGIN_MEMBER_NAME));
        model.addAttribute("loginRole", session.getAttribute(SessionConst.LOGIN_ROLE));

        Object roleObj = session.getAttribute(SessionConst.LOGIN_ROLE);
        MemberRole role = (roleObj instanceof MemberRole) ? (MemberRole) roleObj : null;
        boolean admin = (role == MemberRole.ADMIN);

        String treeMode = admin ? "admin" : "public";
        model.addAttribute("treeMode", treeMode);

        model.addAttribute("categoryTree",
                admin ? categoryService.getAllAdminCategories()
                : categoryService.getAllPublicCategories()
        );

        // 헤더 드롭다운용 루트 카테고리 (이건 항상 public 전용으로 할 것. 어차피 대표 카테고리는 admin이 의미 없음)
        model.addAttribute("rootCategories", categoryService.getTopCategories());
    }
}
