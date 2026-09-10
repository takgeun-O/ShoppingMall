package io.github.takgeun.shop.cart.application.dto;

import io.github.takgeun.shop.product.domain.Product;
import lombok.Getter;

public record CartItemResult(
        Long productId,
        String productName,
        int unitPrice,
        Integer originalPrice,
        int quantity,
        String imageUrl,
        int lineTotal
) {

    public static CartItemResult from(
            Product product,
            int quantity
    ) {
        return new CartItemResult(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getOriginalPrice(),
                quantity,
                product.getImageUrl(),
                product.getPrice() * quantity
        );
    }

    public int discountAmount() {
        if(originalPrice == null || originalPrice <= unitPrice) {
            return 0;
        }

        return (originalPrice - unitPrice) * quantity;
    }
}
