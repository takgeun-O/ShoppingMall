package io.github.takgeun.shop.home.view;

import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.MemberRole;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeViewController {

    @GetMapping("/")
    public String home(HttpSession session, Model model) {

        Object roleObj = session.getAttribute(SessionConst.LOGIN_ROLE);
        MemberRole role = (roleObj instanceof MemberRole mr) ? mr : null;
        boolean admin = (role == MemberRole.ADMIN);

        model.addAttribute("activeTop", "SHOP");
        model.addAttribute("treeMode", admin ? "admin" : "public");

        return "public/index";
    }
}
