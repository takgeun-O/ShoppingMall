package io.github.takgeun.shop.cart.view.dto;

import lombok.Getter;

@Getter
public class CartItemView {

    private final Long productId;        // 템플릿에서 path variable로 사용
    private final String name;
    private final int unitPrice;            // 판매가
    private final Integer originalPrice;    // 정가 (없으면 null)
    private final int quantity;
    private final String imageUrl;


    private CartItemView(Long productId, String name, int unitPrice, Integer originalPrice, int quantity, String imageUrl) {
        this.productId = productId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.originalPrice = originalPrice;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
    }

    // of() : 주어진 값들로 새 객체를 만든다.
    public static CartItemView of(Long id, String name, int unitPrice, Integer originalPrice, int quantity, String imageUrl) {
        String resolvedImageUrl;
        if(imageUrl == null || imageUrl.trim().isBlank()) {
            resolvedImageUrl = "/images/no-image.png";
        } else {
            String trimmed = imageUrl.trim();
            // "/..." 형태면 그대로
            // 아니면 "/" 붙여서 절대경로로 고정시키기 (정적 리소스 기준임)
            resolvedImageUrl = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        }

        int resolvedQty = Math.max(quantity, 1);

        // originalPrice는 표시용
        Integer resolvedOriginalPrice = (originalPrice == null || originalPrice <= unitPrice)
                ? null
                : originalPrice;

        return new CartItemView(id, name, unitPrice, resolvedOriginalPrice, resolvedQty, resolvedImageUrl);
    }

    public int lineTotal() {
        return unitPrice * quantity;
    }

    public int discountPercent() {
        if(originalPrice == null || originalPrice <= 0 || originalPrice <= unitPrice) return 0;
        return (int) Math.round((1 - (double) unitPrice / originalPrice) * 100);
    }

    public boolean hasDiscount() {
        return discountPercent() > 0;
    }
}
