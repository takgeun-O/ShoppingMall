package io.github.takgeun.shop.global.legacy;

import io.github.takgeun.shop.global.view.ViewController;
import org.springframework.web.bind.annotation.GetMapping;

@ViewController
public class ErrorViewController {

    @GetMapping("/forbidden")
    public String forbidden() {
        return "error/403";
    }
}
