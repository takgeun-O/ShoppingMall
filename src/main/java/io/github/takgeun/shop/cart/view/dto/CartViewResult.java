package io.github.takgeun.shop.cart.view.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class CartViewResult {

    private final List<CartItemView> items;
    private final CartSummaryView summary;

    private CartViewResult(List<CartItemView> items, CartSummaryView summary) {
        this.items = items;
        this.summary = summary;
    }

    // 다른 객체로부터 CartViewResult 생성
    public static CartViewResult of(List<CartItemView> items, CartSummaryView summary) {

        List<CartItemView> safeItems = (items == null) ? List.of() : List.copyOf(items);      // DTO는 불변으로 쓰는 게 좋으니 리스트를 그대로 받지 말고 복사해서 쓰기
        return new CartViewResult(safeItems, summary);
    }

    public static CartViewResult empty() {
        return new CartViewResult(List.of(), CartSummaryView.of(0, 0, 0));
    }
}
