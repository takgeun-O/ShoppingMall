package io.github.takgeun.shop.order.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderCancelRequest {

    @Size(max = 200, message = "cancelReason은 200자 이하입니다.")
    private String cancelReason;
}
