package io.github.takgeun.shop.order.application.dto;

public record CheckoutItemCommand(
        Long productId,
        int quantity
) {
}
