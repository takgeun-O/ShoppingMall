package io.github.takgeun.shop.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderListItemResponse {
    private Long orderId;
    private String orderedAt;
    private String status;
    private int totalPrice;
    private String summary;     // 대표 상품명 (운동화 외 2건)
}
