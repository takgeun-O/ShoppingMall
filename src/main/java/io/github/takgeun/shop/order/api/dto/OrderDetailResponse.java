package io.github.takgeun.shop.order.api.dto;

import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        Long orderId,
        OrderStatus status,
        List<OrderItemResponse> items,
        int subtotal,
        int shippingFee,
        int totalPrice,
        String recipientName,
        String recipientPhone,
        String shippingZipCode,
        String shippingAddress,
        String shippingAddressDetail,
        String requestMessage,
        LocalDateTime orderedAt,
        LocalDateTime canceledAt
) {

    public static OrderDetailResponse from(Order order) {
        return new OrderDetailResponse(
                order.getId(),
                order.getStatus(),
                order.getOrderItems().stream()
                        .map(OrderItemResponse::from)
                        .toList(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTotalPrice(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getShippingZipCode(),
                order.getShippingAddress(),
                order.getShippingAddressDetail(),
                order.getRequestMessage(),
                order.getOrderedAt(),
                order.getCanceledAt()
        );
    }

}
