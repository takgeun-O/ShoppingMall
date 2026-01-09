package io.github.takgeun.shop.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {
    private List<OrderItemRequest> items;
    private Long shippingAddressId;
    private String memo;        // 요청사항
}
