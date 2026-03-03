package io.github.takgeun.shop.order.api;

import io.github.takgeun.shop.cart.application.CartService;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.order.application.OrderService;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.dto.request.CheckoutItem;
import io.github.takgeun.shop.order.dto.request.OrderCreateRequest;
import io.github.takgeun.shop.order.dto.response.OrderCreateResponse;
import io.github.takgeun.shop.order.dto.response.OrderListResponse;
import io.github.takgeun.shop.order.dto.response.OrderResponse;
import io.github.takgeun.shop.order.view.form.CheckoutForm;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    /**
     * 카트 기반 주문 (체크아웃)
     * 요청 바디 : CheckoutForm (배송 정보)
     * 주문 상품 : 세션 장바구니에서 읽음
     * */
    @PostMapping("/checkout")
    public ResponseEntity<OrderCreateResponse> create(
            @Valid @RequestBody CheckoutForm form,
            HttpSession session
    ) {

        Long memberId = (Long) session.getAttribute(SessionConst.LOGIN_MEMBER_ID);

        List<CheckoutItem> items = cartService.getCheckoutItems(session);

        Long orderId = orderService.checkout(memberId, items, form);

        cartService.clear(session);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OrderCreateResponse(orderId));
    }

    // 내 주문 목록 조회
    @GetMapping
    public ResponseEntity<OrderListResponse> getMyOrders(HttpSession session) {
        Long memberId = (Long) session.getAttribute(SessionConst.LOGIN_MEMBER_ID);

        List<Order> myOrders = orderService.getMyOrders(memberId);

        return ResponseEntity.ok(OrderListResponse.from(myOrders));
    }

    // 내 주문 상세 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getDetail(
            @PathVariable @NotNull(message = "orderId는 필수입니다.") @Positive(message = "orderId는 양수여야 합니다.") Long orderId,
            HttpSession session
    ) {
        Long memberId = (Long) session.getAttribute(SessionConst.LOGIN_MEMBER_ID);

        OrderResponse response = orderService.getDetail(memberId, orderId);

        return ResponseEntity.ok(response);
    }

    // 주문 취소
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable @NotNull(message = "orderId는 필수입니다.") @Positive(message = "orderId는 양수여야 합니다.") Long orderId,
            HttpSession session
    ) {
        Long memberId = (Long) session.getAttribute(SessionConst.LOGIN_MEMBER_ID);

        orderService.cancel(memberId, orderId);

        return ResponseEntity.noContent().build();
    }
}
