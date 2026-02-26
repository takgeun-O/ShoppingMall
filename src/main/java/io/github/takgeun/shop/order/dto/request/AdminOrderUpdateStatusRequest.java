package io.github.takgeun.shop.order.dto.request;

import io.github.takgeun.shop.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// 뷰 DTO는 @Setter 쓰는 게 편함
@Getter @Setter
public class AdminOrderUpdateStatusRequest {

    @NotNull(message = "상태는 필수입니다.")
    private OrderStatus status;
}
