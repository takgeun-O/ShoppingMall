package io.github.takgeun.shop.global.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorViewController {

    @GetMapping("/forbidden")
    public String forbidden() {
        return "403";
    }
}
