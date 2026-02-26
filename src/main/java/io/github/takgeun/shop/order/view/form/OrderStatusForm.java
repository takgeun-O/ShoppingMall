package io.github.takgeun.shop.order.view.form;

import io.github.takgeun.shop.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// form 전용 DTO
@Getter @Setter
public class OrderStatusForm {

    @NotNull(message = "상태는 필수입니다.")
    private OrderStatus status;
}
