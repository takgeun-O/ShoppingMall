package io.github.takgeun.shop.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

public record CheckoutItem(
        @NotNull @Positive
        Long productId,

        @Positive
        int quantity
) {

    public static CheckoutItem of(
            Long productId,
            int quantity
    ) {
        int resolvedQty = Math.max(quantity, 0);
        return new CheckoutItem(productId, resolvedQty);
    }
}
