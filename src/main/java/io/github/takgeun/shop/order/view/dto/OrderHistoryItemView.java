package io.github.takgeun.shop.order.view.dto;

import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderStatus;
import io.github.takgeun.shop.order.view.support.OrderStatusView;
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

        List<OrderItem> items = order.getOrderItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("주문 상품이 없습니다.");
        }

        OrderItem firstItem = items.get(0);

        // 상품 정보에 들어갈 내용들
        String productName = firstItem.getProductNameSnapshot();
        String productImageUrl = resolveImageUrl(firstItem.getImageUrlSnapshot());
        int extraItemCount = Math.max(items.size() - 1, 0);

        OrderStatus status = order.getStatus();
        OrderStatusView statusView = OrderStatusView.from(status);

        return new OrderHistoryItemView(
                order.getId(),
                order.getOrderedAt().format(DATE_FORMATTER),
                productName,
                productImageUrl,
                order.getTotalPrice(),
                status,
                statusView.getLabel(),
                statusView.getBadgeClass(),
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
}
