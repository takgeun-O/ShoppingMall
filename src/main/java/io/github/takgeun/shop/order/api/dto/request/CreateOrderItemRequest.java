package io.github.takgeun.shop.order.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(

        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,

//        @Min(
//                value = 1,
//                message = "주문 수량은 1개 이상이어야 합니다."
//        )
//        int quantity

        // int는 필드가 누락되면 0으로 들어와서 @Min(1)에 걸린다.
        // 누락과 0 입력을 구분하고자 더 명확한 방식으로 작성함.
        @NotNull(message = "주문 수량은 필수입니다.")
        @Positive(message = "주문 수량은 1개 이상이어야 합니다.")
        Integer quantity
) {
}
