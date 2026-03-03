package io.github.takgeun.shop.order.dto.response;

import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;
    private OrderStatus status;

    // 주문 상품 정보 (스냅샷)
    private List<OrderItemResponse> items;

    // 금액 정보
    private int subtotal;
    private int shippingFee;
    private int totalPrice;

    // 배송 정보
    private String recipientName;
    private String recipientPhone;
    private String shippingZipCode;
    private String shippingAddress;
    private String requestMessage;

    // 시간 정보
    private LocalDateTime orderedAt;
    private LocalDateTime canceledAt;

    @Getter
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long productId;
        private String productName;
        private int unitPrice;
        private int quantity;
        private int lineTotal;

        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getProductId(),
                    item.getProductNameSnapshot(),
                    item.getUnitPriceSnapshot(),
                    item.getQuantity(),
                    item.lineTotal()
            );
        }
    }

    public static OrderResponse from(Order order) {
        if (order == null) throw new IllegalArgumentException("order는 필수입니다.");

        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                items,
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTotalPrice(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getShippingZipCode(),
                order.getShippingAddress(),
                order.getRequestMessage(),
                order.getOrderedAt(),
                order.getCanceledAt()
        );
    }
}