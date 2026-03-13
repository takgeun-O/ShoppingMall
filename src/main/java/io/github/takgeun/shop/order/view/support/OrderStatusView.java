package io.github.takgeun.shop.order.view.support;

import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.Getter;

/**
 * 공통 상태 UI DTO
 */
@Getter
public class OrderStatusView {

    private final OrderStatus status;
    private final String label;
    private final String badgeClass;

    private OrderStatusView(OrderStatus status, String label, String badgeClass) {
        this.status = status;
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public static OrderStatusView from(OrderStatus status) {
        if (status == null) {
            return new OrderStatusView(null, "알 수 없음", "bg-gray-400");
        }

        return switch (status) {
            case ORDERED -> new OrderStatusView(status, "주문완료", "bg-blue-600 text-white");
            case PAYMENT_COMPLETED -> new OrderStatusView(status, "결제완료", "bg-purple-600 text-white");
            case PREPARING -> new OrderStatusView(status, "배송준비", "bg-orange-600 text-white");
            case SHIPPING -> new OrderStatusView(status, "배송중", "bg-cyan-600 text-white");
            case DELIVERED -> new OrderStatusView(status, "배송완료", "bg-green-600 text-white");
            case CANCELED -> new OrderStatusView(status, "주문취소", "bg-red-600 text-white");
        };
    }
}
