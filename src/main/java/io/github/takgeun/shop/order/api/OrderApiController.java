package io.github.takgeun.shop.order.api;

import io.github.takgeun.shop.global.api.ApiController;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.order.api.dto.OrderDetailResponse;
import io.github.takgeun.shop.order.application.OrderService;
import io.github.takgeun.shop.order.domain.Order;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderApiController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public OrderDetailResponse getMyOrder(
            @AuthenticationPrincipal ShopUserPrincipal principal,
            @PathVariable @Positive Long orderId
    ) {
        /**
         * GET /api/v1/orders/{orderId}
         * → Spring Security 인증
         * → ShopUserPrincipal에서 memberId 추출
         * → OrderService.getDetail(memberId, orderId)
         * → 주문 조회 및 소유권 검사
         * → Order 반환
         * → OrderDetailResponse.from(order)
         * → JSON 응답
         */

        Order order = orderService.getDetail(
                principal.getMemberId(),
                orderId
        );

        return OrderDetailResponse.from(order);
    }
}
