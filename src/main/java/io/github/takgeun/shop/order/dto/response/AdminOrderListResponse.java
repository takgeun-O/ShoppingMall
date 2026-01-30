package io.github.takgeun.shop.order.dto.response;

import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminOrderListResponse {

    private Long orderId;
    private OrderStatus status;

    private Long memberId;

    private Long productId;
    private String productName;
    private int totalPrice;
    private LocalDateTime orderedAt;

    public static AdminOrderListResponse from(Order order) {
        return new AdminOrderListResponse(
                order.getId(), order.getStatus(), order.getMemberId(), order.getProductId(), order.getProductNameSnapshot(),
                order.getTotalPrice(), order.getOrderedAt()
        );
    }
}
