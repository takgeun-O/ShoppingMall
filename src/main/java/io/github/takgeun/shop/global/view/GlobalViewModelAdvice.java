package io.github.takgeun.shop.global.view;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.member.domain.MemberRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = ViewController.class)
@RequiredArgsConstructor
public class GlobalViewModelAdvice {

    private final CategoryService categoryService;

    /**
     * @ViewController가 붙은 컨트롤러 실행 전에
     * 화면 공통 Model 속성을 추가한다. (노가다 방지)
     *
     * @ModelAttribute 메서드는 대상 컨트롤러 메서드 실행 전에 호출되어
     * 공통 Model 속성을 추가한다.
     */
    @ModelAttribute
    public void global(
            Authentication authentication,
            Model model
    ) {

        /**
         * 기존 세션 방식
         */
//        Long loginMemberId = getLoginMemberId(session);
//        String loginMemberName = getLoginMemberName(session);
//        MemberRole loginRole = getLoginRole(session);
//        boolean isAdmin = (loginRole == MemberRole.ADMIN);
//        String treeMode = isAdmin ? "admin" : "public";

        /**
         * Spring Security 전환 후 Principal 방식
         */
        ShopUserPrincipal principal = extractPrincipal(authentication);

        boolean loggedIn = principal != null;

        Long loginMemberId = loggedIn
                ? principal.getMemberId()
                : null;

        String loginMemberName = loggedIn
                ? principal.getMemberName()
                : null;

        MemberRole loginRole = loggedIn
                ? principal.getRole()
                : null;

        boolean isAdmin = loginRole == MemberRole.ADMIN;


        model.addAttribute("loginMemberId", loginMemberId);
        model.addAttribute("loginMemberName", loginMemberName);
        model.addAttribute("loginRole", loginRole);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("treeMode", isAdmin ? "admin" : "public");

        // 헤더 드롭다운용 루트 카테고리 (이건 항상 public 전용으로 할 것. 어차피 대표 카테고리는 admin이 의미 없음)
        // TODO: 이건 매 화면 요청마다 호출됨. -> 최적화 문제 발생할 수 있음. 나중에 생각해보기
        model.addAttribute("rootCategories", categoryService.getTopCategories());
    }

    private ShopUserPrincipal extractPrincipal(Authentication authentication) {
        if(authentication == null) {
            return null;        // 로그인하지 않은 사용자가 공개 페이지에 접근했을 때
        }

        if(authentication.getPrincipal() instanceof ShopUserPrincipal principal) {
            return principal;
        }

        return null;        // 익명의 인증 객체일 때를 대비해서 구현
    }
}
