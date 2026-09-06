package io.github.takgeun.shop.order.api.dto;

import io.github.takgeun.shop.order.domain.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

public record OrderListResponse(
        List<OrderSummaryResponse> orders
) {

    public static OrderListResponse from(
            List<Order> orders
    ) {

        if(orders == null) {
            throw new IllegalArgumentException(
                    "orders는 필수입니다."
            );
        }
        return new OrderListResponse(
                orders.stream()
                        .map(OrderSummaryResponse::from)
                        .toList()
        );
    }

}
