package io.github.takgeun.shop.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class CheckoutItem {

    @NotNull
    @Positive
    private final Long productId;

    @Positive
    private final int quantity;

    private CheckoutItem(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public static CheckoutItem of(Long productId, int quantity) {
        int resolvedQty = Math.max(quantity, 0);
        return new CheckoutItem(productId, resolvedQty);
    }
}
