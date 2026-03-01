package io.github.takgeun.shop.cart.view.dto;

import lombok.Getter;

@Getter
public class CartSummaryView {

    private final int subtotal;
    private final int shippingFee;
    private final int total;

    private CartSummaryView(int subtotal, int shippingFee, int total) {
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.total = total;
    }

    public static CartSummaryView of(int subtotal, int shippingFee) {
        int resolvedSubtotal = Math.max(subtotal, 0);
        int resolvedShippingFee = Math.max(shippingFee, 0);
        return new CartSummaryView(resolvedSubtotal, resolvedShippingFee, resolvedSubtotal + resolvedShippingFee);
    }
}
