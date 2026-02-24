package io.github.takgeun.shop.global.view;

import io.github.takgeun.shop.global.session.SessionConst;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalHeaderAdvice {

    @ModelAttribute("loginMemberName")
    public String loginMemberName(HttpSession session) {
        Object name = session.getAttribute(SessionConst.LOGIN_MEMBER_NAME);
        return name != null ? name.toString() : null;
    }

    @ModelAttribute("loginMemberId")
    public Long LoginMemberId(HttpSession session) {
        Object id = session.getAttribute(SessionConst.LOGIN_MEMBER_ID);
        return (id instanceof Long) ? (Long) id : null;
    }
}
