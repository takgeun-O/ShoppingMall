package io.github.takgeun.shop.product.dto.request;

import io.github.takgeun.shop.product.domain.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {
    private String name;
    private int price;
    private int stock;      // 재고
    private Long categoryId;
    private String description;     // 상품 상세 페이지에 보이는 텍스트 설명
    private String thumbnailUrl;    // 목록/카드 UI용 대표 이미지
    private ProductStatus status;
}