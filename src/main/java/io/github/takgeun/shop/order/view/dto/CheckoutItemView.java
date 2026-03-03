package io.github.takgeun.shop.order.view.dto;

import io.github.takgeun.shop.cart.view.dto.CartItemView;
import lombok.Getter;

@Getter
public class CheckoutItemView {
    private final Long productId;
    private final String name;
    private final String imageUrl;

    private final int unitPrice;
    private final Integer originalPrice;    // 정가 (null 가능)
    private final int quantity;

    private CheckoutItemView(Long productId, String name, String imageUrl,
                             int unitPrice, Integer originalPrice, int quantity) {
        this.productId = productId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.unitPrice = unitPrice;
        this.originalPrice = originalPrice;
        this.quantity = quantity;
    }

    public static CheckoutItemView from(CartItemView item) {
        return new CheckoutItemView(
                item.getProductId(),
                item.getName(),
                item.getImageUrl(),
                item.getUnitPrice(),
                item.getOriginalPrice(),
                item.getQuantity()
        );
    }

    // 판매가 * 수량
    public int lineTotal() {
        return unitPrice * quantity;
    }

    // 정가 * 수량 (정가가 없으면 0)
    public int originalLineTotal() {
        if (originalPrice == null) return 0;
        return originalPrice * quantity;
    }

    // 라인 할인금액 (정가 없거나 정가<=판매가 라면 0)
    public int lineDiscountAmount() {
        if (originalPrice == null) return 0;
        int unitDiscount = originalPrice - unitPrice;
        if (unitDiscount <= 0) return 0;
        return unitDiscount * quantity;
    }

    // 할인율(%) 계산
    // 정가 없거나 정가<=판매가라면 null
    public Integer discountRatePercent() {
        if (originalPrice == null) return null;
        if (originalPrice <= unitPrice) return null;

        // (정가-판매가)/정가 * 100, 반올림
        double rate = ((double) (originalPrice - unitPrice) / (double) originalPrice) * 100.0;
        return (int) Math.round(rate);
    }

    public boolean hasDiscount() {
        return discountRatePercent() != null;
    }
}
