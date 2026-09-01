package io.github.takgeun.shop.order.view;

import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.global.view.ViewController;
import io.github.takgeun.shop.order.view.dto.OrderHistoryPageView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

@ViewController
@RequiredArgsConstructor
@RequestMapping("/members/me/orders")
public class MyPageOrderViewController {

    private final OrderHistoryViewService orderHistoryViewService;

    /**
     * 마이페이지 전체 주문 내역
     * GET /members/me/orders?page=1
     */
    @GetMapping
    public String orderHistory(@RequestParam(defaultValue = "1") int page,
                               @AuthenticationPrincipal ShopUserPrincipal principal,
                               Model model) {

        Long memberId = principal.getMemberId();

        if(memberId == null) {
            return "redirect:" + redirectToLogin("/members/me/orders");
        }

        // 화면에 필요한 정보와 페이징 정보 넘어옴
        OrderHistoryPageView view = orderHistoryViewService.getOrderHistoryPage(memberId, page);

        model.addAttribute("orders", view.getOrders());
        model.addAttribute("totalOrders", view.getTotalOrders());
        model.addAttribute("currentPage", view.getCurrentPage());
        model.addAttribute("totalPages", view.getTotalPages());
        model.addAttribute("pageNumbers", view.getPageNumbers());

        return "public/members/orders/index";
    }

    private String redirectToLogin(String next) {
        return UriComponentsBuilder
                .fromPath("/login")
                .queryParam("reason", "LOGIN_REQUIRED")
                .queryParam("next", next)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }
}
