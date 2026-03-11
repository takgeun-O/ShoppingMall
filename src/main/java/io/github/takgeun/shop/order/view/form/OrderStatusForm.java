package io.github.takgeun.shop.order.view.form;

import io.github.takgeun.shop.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// form 전용 DTO
@Getter
@Setter
public class OrderStatusForm {

    @NotNull(message = "주문 상태를 선택해주세요.")
    private OrderStatus status;
}
