package io.github.takgeun.shop.home.view;

import io.github.takgeun.shop.global.session.SessionConst;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeViewController {

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        Object role = session.getAttribute(SessionConst.LOGIN_ROLE);

        String productsUrl = "/products";
        if(role != null && role.toString().equals("ADMIN")) {
            productsUrl = "/admin/products";
        }

        model.addAttribute("productsUrl", productsUrl);
        return "index";
    }
}
