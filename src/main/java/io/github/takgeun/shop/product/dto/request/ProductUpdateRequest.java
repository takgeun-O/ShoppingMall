package io.github.takgeun.shop.product.dto.request;

import io.github.takgeun.shop.product.domain.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {
    private String name;
    private Integer price;
    private Integer stock;
    private Long categoryId;
    private String description;
    private String thumbnailUrl;    // 목록/카드 UI용 대표 이미지
    private ProductStatus status;
}
