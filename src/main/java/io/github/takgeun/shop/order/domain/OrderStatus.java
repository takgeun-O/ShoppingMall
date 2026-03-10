package io.github.takgeun.shop.order.domain;

import lombok.Getter;

@Getter
public enum OrderStatus {

    ORDERED("주문완료"),
    PAYMENT_COMPLETED("결제완료"),
    PREPARING("배송준비"),
    SHIPPING("배송중"),
    DELIVERED("배송완료"),
    CANCELED("취소");

    private final String label;

    // th:text="${order.status.label}"
    OrderStatus(String label) {
        this.label = label;
    }
}


