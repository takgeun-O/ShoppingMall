package io.github.takgeun.shop.cart.application.dto;

import lombok.Getter;

import java.util.List;

public record CartResult(
        List<CartItemResult> items,
        CartSummaryResult summary
) {

    public CartResult {
        items = List.copyOf(items);
    }

    public static CartResult empty() {
        return new CartResult(
                List.of(),
                CartSummaryResult.of(0, 0, 0)
        );
    }
}
