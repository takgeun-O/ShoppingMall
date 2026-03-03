package io.github.takgeun.shop.cart.view.dto;

import lombok.Getter;

@Getter
public class CartSummaryView {

    private final int subtotal;
    private final int discountTotal;
    private final int shippingFee;
    private final int total;

    private CartSummaryView(int subtotal, int discountTotal, int shippingFee, int total) {
        this.subtotal = subtotal;
        this.discountTotal = discountTotal;
        this.shippingFee = shippingFee;
        this.total = total;
    }

    public static CartSummaryView of(int subtotal, int discountTotal, int shippingFee) {
        int resolvedSubtotal = Math.max(subtotal, 0);
        int resolvedDiscount = Math.max(discountTotal, 0);
        int resolvedShippingFee = Math.max(shippingFee, 0);

        // 할인은 상품금액을 초과할 수 없음
        if(resolvedDiscount > resolvedSubtotal) {
            resolvedDiscount = resolvedSubtotal;
        }

        int total = resolvedSubtotal - resolvedDiscount + resolvedShippingFee;
        return new CartSummaryView(resolvedSubtotal, resolvedDiscount, resolvedShippingFee, resolvedSubtotal + resolvedShippingFee);
    }
}
