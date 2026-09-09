package io.github.takgeun.shop.cart.api.response;

import io.github.takgeun.shop.cart.application.dto.CartSummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장바구니 금액 요약 응답")
public record CartSummaryResponse(

        @Schema(
                description = "할인 전 정가 기준 상품 합계",
                example = "100000"
        )
        int originalSubtotal,

        @Schema(
                description = "전체 할인 금액",
                example = "20000"
        )
        int discountTotal,

        @Schema(
                description = "할인이 적용된 판매가 기준 상품 합계",
                example = "80000"
        )
        int subtotal,

        @Schema(
                description = "배송비",
                example = "0"
        )
        int shippingFee,

        @Schema(
                description = "최종 결제 예정 금액",
                example = "80000"
        )
        int totalPrice
) {

    public static CartSummaryResponse from(
            CartSummaryResult result
    ) {
        return new CartSummaryResponse(
                result.originalSubtotal(),
                result.discountTotal(),
                result.subtotal(),
                result.shippingFee(),
                result.totalPrice()
        );
    }
}
