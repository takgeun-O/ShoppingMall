package io.github.takgeun.shop.order.dto.response;

import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;
    private OrderStatus status;

    // 주문 상품 정보 (스냅샷)
    private Long productId;
    private String productName;
    private int unitPrice;
    private int quantity;
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

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(), order.getStatus(),
                order.getProductId(), order.getProductNameSnapshot(), order.getUnitPriceSnapshot(), order.getQuantity(),
                order.getTotalPrice(), order.getRecipientName(), order.getRecipientPhone(),
                order.getShippingZipCode(), order.getShippingAddress(), order.getRequestMessage(),
                order.getOrderedAt(), order.getCanceledAt()
        );
    }
}
