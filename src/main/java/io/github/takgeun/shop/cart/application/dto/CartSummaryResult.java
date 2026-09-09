package io.github.takgeun.shop.cart.application.dto;

public record CartSummaryResult(
        int originalSubtotal,
        int discountTotal,
        int subtotal,
        int shippingFee,
        int totalPrice
) {

    public static CartSummaryResult of(
            int subtotal,
            int discountTotal,
            int shippingFee
    ) {
        int resolvedSubtotal = Math.max(subtotal, 0);
        int resolvedDiscountTotal = Math.max(discountTotal, 0);
        int resolvedShippingFee = Math.max(shippingFee, 0);
        int originalSubtotal =
                resolvedSubtotal + resolvedDiscountTotal;
        int totalPrice =
                resolvedSubtotal + resolvedShippingFee;

        return new CartSummaryResult(
                originalSubtotal,
                resolvedDiscountTotal,
                resolvedSubtotal,
                resolvedShippingFee,
                totalPrice
        );
    }
}
