package io.github.takgeun.shop.order.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(

        /**
         * @NotEmpty
         * null       → 실패
         * 빈 목록 [] → 실패
         * 1개 이상   → 통과
         *
         * @Valid : 목록 안의 각 CreateOrderItemRequest까지 검증하게 한다.
         * -> 이렇게 하면 내부 필드 검증 오류도 잡힌다.
         */
        @Valid
        @NotEmpty(message = "주문 상품은 1개 이상이어야 합니다.")
        List<CreateOrderItemRequest> items,

        @NotBlank(message = "수령인 이름은 필수입니다.")
        String recipientName,

        @NotBlank(message = "전화번호는 필수입니다.")
        String phoneNumber,

        @NotBlank(message = "우편번호는 필수입니다.")
        String zipCode,

        @NotBlank(message = "주소는 필수입니다.")
        String address,

        @Size(
                max = 200,
                message = "상세 주소는 200자 이하이어야 합니다."
        )
        String addressDetail,

        String requestMessage,

        @NotBlank(message = "요청 식별자는 필수입니다.")
        String requestKey
) {
}
