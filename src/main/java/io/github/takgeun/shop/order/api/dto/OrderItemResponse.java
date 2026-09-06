package io.github.takgeun.shop.order.api.dto;

import io.github.takgeun.shop.order.domain.OrderItem;

public record OrderItemResponse(
        Long productId,
        String productName,
        int unitPrice,
        int quantity,
        int lineTotal
) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductNameSnapshot(),
                item.getUnitPriceSnapshot(),
                item.getQuantity(),
                item.lineTotal()
        );
    }
}
