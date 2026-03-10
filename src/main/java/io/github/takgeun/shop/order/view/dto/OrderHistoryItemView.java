package io.github.takgeun.shop.order.view.dto;

import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
public class OrderHistoryItemView {

    private static final String DEFAULT_IMAGE_URL = "/images/no-image.png";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final Long orderId;
    private final String orderDateText;
    private final String productName;
    private final String productImageUrl;
    private final int totalAmount;
    private final OrderStatus status;
    private final String statusLabel;
    private final String statusBadgeClass;
    private final int extraItemCount;

    private OrderHistoryItemView(
            Long orderId,
            String orderDateText,
            String productName,
            String productImageUrl,
            int totalAmount,
            OrderStatus status,
            String statusLabel,
            String statusBadgeClass,
            int extraItemCount) {

        this.orderId = orderId;
        this.orderDateText = orderDateText;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.totalAmount = totalAmount;
        this.status = status;
        this.statusLabel = statusLabel;
        this.statusBadgeClass = statusBadgeClass;
        this.extraItemCount = extraItemCount;
    }

    public static OrderHistoryItemView from(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("order는 필수입니다.");
        }

        List<OrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("주문 상품이 없습니다.");
        }

        OrderItem firstItem = items.get(0);

        // 상품 정보에 들어갈 내용들
        String productName = firstItem.getProductNameSnapshot();
        String productImageUrl = resolveImageUrl(firstItem.getImageUrlSnapshot());
        int extraItemCount = Math.max(items.size() - 1, 0);

        OrderStatus status = order.getStatus();

        return new OrderHistoryItemView(
                order.getId(),
                order.getOrderedAt().format(DATE_FORMATTER),
                productName,
                productImageUrl,
                order.getTotalPrice(),
                status,
                getStatusLabel(status),
                getStatusBadgeClass(status),
                extraItemCount
        );
    }


    private static String resolveImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isBlank()) {
            return DEFAULT_IMAGE_URL;
        }

        String trimmed = imageUrl.trim();

        if (trimmed.startsWith("/") ||
                trimmed.startsWith("http://") ||
                trimmed.startsWith("https://")) {
            return trimmed;
        }

        return "/" + trimmed;
    }

    private static String getStatusLabel(OrderStatus status) {
        if(status == null) return "상태 없음";

        return switch (status) {
            case ORDERED -> "주문완료";
            case PAYMENT_COMPLETED -> "결제완료";
            case PREPARING -> "배송준비";
            case SHIPPING -> "배송중";
            case DELIVERED -> "배송완료";
            case CANCELED -> "취소";
        };
    }

    private static String getStatusBadgeClass(OrderStatus status) {
        if(status == null) return "bg-gray-500";

        return switch (status) {
            case ORDERED -> "bg-blue-600";
            case PAYMENT_COMPLETED -> "bg-purple-600";
            case PREPARING -> "bg-orange-600";
            case SHIPPING -> "bg-cyan-600";
            case DELIVERED -> "bg-green-600";
            case CANCELED -> "bg-red-600";
        };
    }
}
