package io.github.takgeun.shop.product.view.dto;

import io.github.takgeun.shop.product.domain.ProductStatus;
import lombok.Getter;

@Getter
public class AdminProductListItemView {

    private final Long id;
    private final String name;
    private final int price;
    private final Integer originalPrice;
    private final int stock;
    private final String imageUrl;
    private final String categoryName;
    private final ProductStatus status;

    private AdminProductListItemView(
            Long id,
            String name,
            int price,
            Integer originalPrice,
            int stock,
            String imageUrl,
            String categoryName,
            ProductStatus status) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.categoryName = categoryName;
        this.status = status;
    }

    public static AdminProductListItemView of(
            Long id,
            String name,
            int price,
            Integer originalPrice,
            int stock,
            String imageUrl,
            String categoryName,
            ProductStatus status
    ) {
        return new AdminProductListItemView(
                id, name, price, originalPrice, stock, imageUrl, categoryName, status
        );
    }
}
