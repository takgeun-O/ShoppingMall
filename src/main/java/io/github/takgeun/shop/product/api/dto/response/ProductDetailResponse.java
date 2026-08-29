package io.github.takgeun.shop.product.api.dto.response;

import io.github.takgeun.shop.product.domain.Product;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 공개 API에서 내부 관리 상태인 ProductStatus와 정확한 재고 수량은
 * 노출하지 않는 방향으로 가기
 */
@Schema(description = "공개 상품 상세 응답")
public record ProductDetailResponse(

        @Schema(
                description = "상품 ID",
                example = "1"
        )
        Long id,

        @Schema(
                description = "카테고리 ID",
                example = "1"
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
                example = "120000"
        )
        Integer originalPrice,

        @Schema(
                description = "할인율. 할인하지 않는 상품은 0",
                example = "18"
        )
        int discountPercent,

        @Schema(
                description = "상품 설명",
                example = "저소음 스위치를 적용한 무선 기계식 키보드입니다."
        )
        String description,

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

    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getCategoryId(),
                product.getName(),
                product.getPrice(),
                product.getOriginalPrice(),
                product.discountPercent(),
                product.getDescription(),
                product.getImageUrl(),
                product.getRatingValue(),
                product.isSoldOut()
        );
    }
}
