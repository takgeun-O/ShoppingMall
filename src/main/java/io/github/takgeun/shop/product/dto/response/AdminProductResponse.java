package io.github.takgeun.shop.product.dto.response;

import io.github.takgeun.shop.product.domain.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminProductResponse {
    private Long id;
    private String name;
    private int price;
    private int stock;
    private Long categoryId;
    private String categoryName;
    private String thumbnailUrl;
    private ProductStatus status;
}
