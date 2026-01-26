package io.github.takgeun.shop.order.dto.response;

import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderListResponse {

    private Long orderId;
    private OrderStatus status;

    private String productName;
    private int totalPrice;
    private LocalDateTime orderedAt;

    public static OrderListResponse from(Order order) {
        return new OrderListResponse(
                order.getId(), order.getStatus(),
                order.getProductNameSnapshot(), order.getTotalPrice(),
                order.getOrderedAt()
        );
    }
}
