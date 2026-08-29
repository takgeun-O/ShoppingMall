package io.github.takgeun.shop.product.api.dto.response;

import io.github.takgeun.shop.product.domain.Product;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공개 상품 응답")
public record ProductListItemResponse(
        @Schema(
                description = "상품 ID",
                example = "1"
        )
        Long id,

        @Schema(
                description = "카테고리 ID",
                example = "10"
        )
        Long categoryId,

        @Schema(
                description = "상품명",
                example = "무선 기계식 키보드"
        )
        String name,

        @Schema(
                description = "판매가",
                example = "99000"
        )
        int price,

        @Schema(
                description = "할인 전 정가. 할인하지 않는 상품은 null",
                example = "120000",
                nullable = true
        )
        Integer originalPrice,

        @Schema(
                description = "할인율. 할인하지 않는 상품은 0",
                example = "18"
        )
        int discountPercent,

        @Schema(
                description = "상품 이미지 URL",
                example = "https://example.com/images/products/keyboard.jpg"
        )
        String imageUrl,

        @Schema(
                description = "상품 평점",
                example = "4.5"
        )
        double rating,

        @Schema(
                description = "품절 여부",
                example = "false"
        )
        boolean soldOut
) {

        public static ProductListItemResponse from(Product product) {
                return new ProductListItemResponse(
                        product.getId(),
                        product.getCategoryId(),
                        product.getName(),
                        product.getPrice(),
                        product.getOriginalPrice(),
                        product.discountPercent(),
                        product.getImageUrl(),
                        product.getRatingValue(),
                        product.isSoldOut()
                );
        }
}
