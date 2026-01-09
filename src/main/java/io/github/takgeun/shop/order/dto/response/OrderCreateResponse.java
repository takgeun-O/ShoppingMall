package io.github.takgeun.shop.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderCreateResponse {
    private Long orderId;
    private String status;
    private int totalPrice;
}
