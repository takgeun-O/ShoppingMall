package io.github.takgeun.shop.order.api.dto;

import io.github.takgeun.shop.order.domain.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OrderListResponse {

    private List<OrderSummaryResponse> orders;

    public static OrderListResponse from(List<Order> orders) {
        return new OrderListResponse(
                orders.stream()
                        .map(OrderSummaryResponse::from)
                        .toList()
        );
    }
}
