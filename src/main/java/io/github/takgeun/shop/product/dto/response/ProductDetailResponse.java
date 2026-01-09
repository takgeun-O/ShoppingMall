package io.github.takgeun.shop.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProductDetailResponse {
    private Long id;
    private String name;
    private int price;
    private boolean purchasable;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String thumbnailUrl;            // 상품상세보기에서도 썸네일 이미지 필요할 수도 있음.
    private List<String> imageUrls;
}
