package io.github.takgeun.shop.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductListItemResponse {
    private Long id;
    private String name;
    private int price;
    private String thumbnailUrl;
    private boolean purchasable;            // ON_SALE && stock > 0
}
