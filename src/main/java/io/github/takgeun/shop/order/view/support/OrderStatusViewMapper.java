package io.github.takgeun.shop.order.view.support;

import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class OrderStatusViewMapper {

    public static String getLabel(OrderStatus status) {
        if (status == null) {
            return "알 수 없음";
        }

        return switch (status) {
            case ORDERED -> "주문완료";
            case PAYMENT_COMPLETED -> "결제완료";
            case PREPARING -> "배송준비";
            case SHIPPING -> "배송중";
            case DELIVERED -> "배송완료";
            case CANCELED -> "주문취소";
        };
    }

    public static String getBadgeClass(OrderStatus status) {
        if (status == null) {
            return "bg-gray-400 text-white";
        }

        return switch (status) {
            case ORDERED -> "bg-slate-500 text-white";
            case PAYMENT_COMPLETED -> "bg-blue-600 text-white";
            case PREPARING -> "bg-indigo-600 text-white";
            case SHIPPING -> "bg-green-600 text-white";
            case DELIVERED -> "bg-gray-700 text-white";
            case CANCELED -> "bg-red-600 text-white";
        };
    }
}
