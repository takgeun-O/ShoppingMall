package io.github.takgeun.shop.home.view;

import io.github.takgeun.shop.global.view.ViewController;
import org.springframework.web.bind.annotation.GetMapping;

@ViewController
public class HomeViewController {

    @GetMapping("/")
    public String home() {

        return "public/index";
    }
}
