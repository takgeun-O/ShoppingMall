package io.github.takgeun.shop.global.view;

import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.MemberRole;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewModelAdvice {

    /**
     * @ModelAttribute : 컨트롤러 실행 전에 Model에 값을 자동으로 추가
     * 해당 메서드의 반환값을 model.addAttribute("loginMemberId", 반환값); 처럼 자동으로 넣어준다.
     */
    @ModelAttribute("loginMemberId")
    public Long loginMemberId(HttpSession session) {
        Object v = session.getAttribute(SessionConst.LOGIN_MEMBER_ID);
        return (v instanceof Long) ? (Long) v : null;
    }

    @ModelAttribute("loginRole")
    public MemberRole loginRole(HttpSession session) {
        Object v = session.getAttribute(SessionConst.LOGIN_ROLE);
        return (v instanceof MemberRole) ? (MemberRole) v : null;
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(@ModelAttribute("loginRole") MemberRole role) {
        return role != null && role == MemberRole.ADMIN;
    }
}
