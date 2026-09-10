package io.github.takgeun.shop.cart.api.response;

import io.github.takgeun.shop.cart.application.dto.CartItemResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장바구니 상품 응답")
public record CartItemResponse(

        @Schema(description = "상품 ID", example = "1")
        Long productId,

        @Schema(description = "상품명", example = "무선 기계식 키보드")
        String productName,

        @Schema(description = "판매 단가", example = "40000")
        int unitPrice,

        @Schema(
                description = "할인 전 정가. 할인하지 않는 상품은 null",
                example = "50000"
        )
        Integer originalPrice,

        @Schema(description = "장바구니 수량", example = "2")
        int quantity,

        @Schema(description = "상품 이미지 URL")
        String imageUrl,

        @Schema(description = "판매가 기준 상품별 합계", example = "80000")
        int lineTotal,

        @Schema(description = "상품별 전체 할인 금액", example = "20000")
        int discountAmount
) {

    public static CartItemResponse from(
            CartItemResult result
    ) {
        return new CartItemResponse(
                result.productId(),
                result.productName(),
                result.unitPrice(),
                result.originalPrice(),
                result.quantity(),
                result.imageUrl(),
                result.lineTotal(),
                result.discountAmount()
        );
    }
}
