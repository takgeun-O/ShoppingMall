package io.github.takgeun.shop.order.view;

import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.order.view.dto.OrderHistoryPageView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

@Controller
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
                               HttpServletRequest request,
                               Model model) {

        HttpSession session = request.getSession(false);
        Long memberId = getLoginMemberId(session);

        if(memberId == null) {
            return "redirect:" + redirectToLogin("/members/me/orders");
        }

        OrderHistoryPageView view = orderHistoryViewService.getOrderHistoryPage(memberId, page);

        model.addAttribute("orders", view.getOrders());
        model.addAttribute("totalOrders", view.getTotalOrders());
        model.addAttribute("currentPage", view.getCurrentPage());
        model.addAttribute("totalPages", view.getTotalPages());
        model.addAttribute("pageNumbers", view.getPageNumbers());

        return "public/members/orders/index";
    }

    private Long getLoginMemberId(HttpSession session) {
        if(session == null) return null;

        Object idObj = session.getAttribute(SessionConst.LOGIN_MEMBER_ID);
        return (idObj instanceof  Long id) ? id : null;
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
