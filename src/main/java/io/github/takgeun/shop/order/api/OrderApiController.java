package io.github.takgeun.shop.order.api;

import io.github.takgeun.shop.global.api.ApiController;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.order.api.dto.request.CreateOrderRequest;
import io.github.takgeun.shop.order.api.dto.response.CreateOrderResponse;
import io.github.takgeun.shop.order.api.dto.response.OrderDetailResponse;
import io.github.takgeun.shop.order.api.dto.response.OrderListResponse;
import io.github.takgeun.shop.order.application.OrderService;
import io.github.takgeun.shop.order.application.dto.CheckoutItemCommand;
import io.github.takgeun.shop.order.application.dto.CreateOrderCommand;
import io.github.takgeun.shop.order.domain.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@ApiController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderApiController {

    private final OrderService orderService;

    @GetMapping
    public OrderListResponse getMyOrders(
            @AuthenticationPrincipal ShopUserPrincipal principal
    ) {
        List<Order> orders = orderService.getMyOrders(
                principal.getMemberId()
        );

        return OrderListResponse.from(orders);
    }

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

    /**
     * POST /api/v1/orders
     * → Spring Security 인증
     * → @Valid 요청 검증
     * → CreateOrderItemRequest를 CheckoutItemCommand로 변환
     * → CreateOrderRequest를 CreateOrderCommand로 변환
     * → OrderService.checkout()
     * → 주문 ID 반환
     * → 201 Created + Location 헤더
     */
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @AuthenticationPrincipal ShopUserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        List<CheckoutItemCommand> checkoutItems =
                request.items().stream()
                        .map(item -> new CheckoutItemCommand(
                                item.productId(),
                                item.quantity()
                        ))
                        .toList();

        CreateOrderCommand command =
                new CreateOrderCommand(
                        request.recipientName(),
                        request.phoneNumber(),
                        request.zipCode(),
                        request.address(),
                        request.addressDetail(),
                        request.requestMessage(),
                        request.requestKey()
                );

        Long orderId = orderService.checkout(
                principal.getMemberId(),
                checkoutItems,
                command
        );

        // 서버가 새로 생성한 주문 리소스의 주소를 클라이언트에게 알려주기 위해 만든다.
        URI location = URI.create(
                "/api/v1/orders/" + orderId
        );

        return ResponseEntity
                // 서버가 새로 생성한 주문 리소스의 주소를 클라이언트에게 알려주기 위해 만든다.
                // 여기서 created의 location은 리다이렉트가 아님을 주의!
                /**
                 * | 응답               | 의미                 |
                 * | ---------------- | ------------------ |
                 * | `201 + Location` | 새 리소스의 위치 안내       |
                 * | `302 + Location` | 해당 주소로 이동하라는 리다이렉트 |
                 */
                .created(location)
                .body(new CreateOrderResponse(orderId));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @AuthenticationPrincipal ShopUserPrincipal principal,
            @PathVariable @Positive Long orderId
    ) {

        orderService.cancel(
                principal.getMemberId(),
                orderId
        );

        return ResponseEntity.noContent().build();
    }
}
