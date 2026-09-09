package io.github.takgeun.shop.cart.application.dto;

import lombok.Getter;

public record CartSummaryResult(
        int subtotal,
        int discountTotal,
        int shippingFee,
        int totalPrice
) {

    public static CartSummaryResult of(
            int subtotal,
            int discountTotal,
            int shippingFee
    ) {
        return new CartSummaryResult(
                subtotal,
                discountTotal,
                shippingFee,
                subtotal + shippingFee
        );
    }
}
