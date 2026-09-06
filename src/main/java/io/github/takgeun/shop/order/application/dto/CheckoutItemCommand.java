package io.github.takgeun.shop.order.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CheckoutItemCommand(
        @NotNull(message = "상품 ID는 필수입니다.")
        @Positive(message = "상품 ID는 양수여야 합니다.")
        Long productId,

        @Positive(message = "주문 수량은 1 이상이어야 합니다.")
        int quantity
) {
}
