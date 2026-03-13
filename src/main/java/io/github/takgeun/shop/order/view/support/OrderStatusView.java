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
        if(status == null) {
            return new OrderStatusView(null, "상태 없음", "bg-gray-500");
        }

        return switch (status) {
            case ORDERED -> new OrderStatusView(status, "주문완료", "bg-slate-500");
            case PAYMENT_COMPLETED -> new OrderStatusView(status, "결제완료", "bg-blue-600");
            case PREPARING -> new OrderStatusView(status, "배송준비", "bg-indigo-600");
            case SHIPPING -> new OrderStatusView(status, "배송중", "bg-green-600");
            case DELIVERED -> new OrderStatusView(status, "배송완료", "bg-gray-700");
            case CANCELED -> new OrderStatusView(status, "취소", "bg-red-600");
        };
    }
}
