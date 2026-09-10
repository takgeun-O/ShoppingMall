package io.github.takgeun.shop.cart.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "장바구니 상품 추가 요청")
public record AddCartItemRequest(

        @Schema(
                description = "장바구니에 추가할 상품 ID",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "상품 ID는 필수입니다.")
        @Positive(message = "상품 ID는 양수여야 합니다.")
        Long productId,

        @Schema(
                description = "추가할 상품 수량",
                example = "2",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Positive(message = "수량은 1 이상이어야 합니다.")
        int quantity    // int 선언 시 JSON에서 필드가 빠졌을 때 기본값 0이 들어온다. 이ㅏ 떄 @Positive에서 검증 실패할 것
) {
}
