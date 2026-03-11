package io.github.takgeun.shop.member.view.dto;

import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class MyPageRecentOrderView {
    private final String productName;
    private final LocalDateTime orderedAt;
    private final int totalPrice;
    private final OrderStatus status;

    public MyPageRecentOrderView(String productName, LocalDateTime orderedAt, int totalPrice, OrderStatus status) {
        this.productName = productName;
        this.orderedAt = orderedAt;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public static MyPageRecentOrderView from(Order order) {
        // 스냅샷이 OrderItem에 들어가있음
        // 여러 상품이면 "대표 상품명 외 n건"
        String name = order.getOrderItems().isEmpty()
                ? "주문 상품"
                : order.getOrderItems().get(0).getProductNameSnapshot();

        if(order.getOrderItems().size() >= 2) {
            name = name + " 외 " + (order.getOrderItems().size() - 1) + "건";
        }

        return new MyPageRecentOrderView(
                name,
                order.getOrderedAt(),
                order.getTotalPrice(),
                order.getStatus()
        );
    }
}
