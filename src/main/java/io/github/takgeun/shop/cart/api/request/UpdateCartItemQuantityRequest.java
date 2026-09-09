package io.github.takgeun.shop.cart.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "장바구니 상품 수량 변경 요청")
public record UpdateCartItemQuantityRequest(

        @Schema(
                description = "변경할 장바구니 수량. 0이면 상품을 삭제합니다.",
                example = "3",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "수량은 필수입니다.")
        @PositiveOrZero(message = "수량은 0 이상이어야 합니다.")   // 수량이 0이면 장바구니 삭제, 음수는 거부
        Integer quantity
) {
}
