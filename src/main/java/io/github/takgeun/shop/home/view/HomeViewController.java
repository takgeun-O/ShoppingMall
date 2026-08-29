package io.github.takgeun.shop.home.view;

import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.global.view.ViewController;
import io.github.takgeun.shop.member.domain.MemberRole;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@ViewController
public class HomeViewController {

    @GetMapping("/")
    public String home() {

        return "public/index";
    }
}
