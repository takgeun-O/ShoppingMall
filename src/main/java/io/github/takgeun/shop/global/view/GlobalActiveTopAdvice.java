package io.github.takgeun.shop.global.view;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = ViewController.class)
public class GlobalActiveTopAdvice {

    @ModelAttribute("activeTop")
    public String activeTop(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // 커뮤니티
        if(uri.startsWith("/community")) return "COMMUNITY";

        // 고객센터
        if(uri.startsWith("/cs")) return "CS";

        // 나머지는 일단 전부 쇼핑(카테고리/상품/주문/회원)
        return "SHOP";
    }
}
