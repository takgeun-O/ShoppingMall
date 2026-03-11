package io.github.takgeun.shop.order.dto.response;

import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderResponse {

    private final Long orderId;
    private final OrderStatus status;

    // 주문 상품 정보 (스냅샷)
    private final List<OrderItemResponse> items;

    // 금액 정보
    private final int subtotal;
    private final int shippingFee;
    private final int totalPrice;

    // 배송 정보
    private final String recipientName;
    private final String recipientPhone;
    private final String shippingZipCode;
    private final String shippingAddress;
    private final String shippingAddressDetail;
    private final String requestMessage;

    // 시간 정보
    private final LocalDateTime orderedAt;
    private final LocalDateTime canceledAt;


    private OrderResponse(Long orderId, OrderStatus status, List<OrderItemResponse> items, int subtotal, int shippingFee, int totalPrice, String recipientName, String recipientPhone, String shippingZipCode, String shippingAddress, String shippingAddressDetail, String requestMessage, LocalDateTime orderedAt, LocalDateTime canceledAt) {
        this.orderId = orderId;
        this.status = status;
        this.items = items;
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.totalPrice = totalPrice;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.shippingZipCode = shippingZipCode;
        this.shippingAddress = shippingAddress;
        this.shippingAddressDetail = shippingAddressDetail;
        this.requestMessage = requestMessage;
        this.orderedAt = orderedAt;
        this.canceledAt = canceledAt;
    }

    public static OrderResponse from(Order order) {
        if(order == null) {
            throw new IllegalArgumentException("order는 필수입니다.");
        }

        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                itemResponses,
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTotalPrice(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getShippingZipCode(),
                order.getShippingAddress(),
                order.getShippingAddressDetail(),
                order.getRequestMessage(),
                order.getOrderedAt(),
                order.getCanceledAt()
        );
    }

    @Getter
    public static class OrderItemResponse {
        private final Long productId;
        private final String productName;
        private final int unitPrice;
        private final int quantity;
        private final int lineTotal;


        public OrderItemResponse(Long productId, String productName, int unitPrice, int quantity, int lineTotal) {
            this.productId = productId;
            this.productName = productName;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
            this.lineTotal = lineTotal;
        }

        // OrderItem 객체로 OrderItemResponse 객체 생성
        public static OrderItemResponse from(OrderItem item) {
            if(item == null) {
                throw new IllegalArgumentException("item은 필수입니다.");
            }

            return new OrderItemResponse(
                    item.getProductId(),
                    item.getProductNameSnapshot(),
                    item.getUnitPriceSnapshot(),
                    item.getQuantity(),
                    item.lineTotal()
            );
        }
    }
}

