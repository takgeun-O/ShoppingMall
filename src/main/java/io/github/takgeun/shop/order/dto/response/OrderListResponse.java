package io.github.takgeun.shop.order.dto.response;

import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrderListResponse {

    private List<OrderResponse> orders;

    public static OrderListResponse from(List<Order> orders) {
        return new OrderListResponse(
                orders.stream()
                        .map(OrderResponse::from)
                        .toList()
        );
    }
}
