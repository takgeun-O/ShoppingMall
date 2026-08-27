package io.github.takgeun.shop.global.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestApiController {

    @GetMapping
    public String test() {
        return "Swagger 정상 작동";
    }
}
