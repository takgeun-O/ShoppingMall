package io.github.takgeun.shop.order.api.dto;

import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderStatus;

import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long orderId,
        OrderStatus status,
        String representativeProductName,
        int itemCount,
        int totalPrice,
        LocalDateTime orderedAt,
        LocalDateTime canceledAt
) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getStatus(),
                order.getOrderItems().getFirst().getProductNameSnapshot(),
                order.getOrderItems().size(),
                order.getTotalPrice(),
                order.getOrderedAt(),
                order.getCanceledAt()
        );
    }
}
