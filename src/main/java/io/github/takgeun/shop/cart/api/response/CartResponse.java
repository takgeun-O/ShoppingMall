package io.github.takgeun.shop.cart.api.response;

import io.github.takgeun.shop.cart.application.dto.CartResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * {
 *   "items": [
 *     {
 *       "productId": 1,
 *       "productName": "무선 기계식 키보드",
 *       "unitPrice": 40000,
 *       "originalPrice": 50000,
 *       "quantity": 2,
 *       "imageUrl": "/images/keyboard.jpg",
 *       "lineTotal": 80000,
 *       "discountAmount": 20000
 *     }
 *   ],
 *   "summary": {
 *     "originalSubtotal": 100000,
 *     "discountTotal": 20000,
 *     "subtotal": 80000,
 *     "shippingFee": 0,
 *     "totalPrice": 80000
 *   }
 * }
 */
@Schema(description = "장바구니 조회 응답")
public record CartResponse(

        @Schema(description = "장바구니 상품 목록")
        List<CartItemResponse> items,

        @Schema(description = "장바구니 금액 요약")
        CartSummaryResponse summary
) {

    public static CartResponse from(CartResult result) {
        return new CartResponse(
                result.items()
                        .stream()
                        .map(CartItemResponse::from)
                        .toList(),
                CartSummaryResponse.from(
                        result.summary()
                )
        );
    }
}
