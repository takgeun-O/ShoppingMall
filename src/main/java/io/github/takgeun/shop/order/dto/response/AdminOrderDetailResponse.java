package io.github.takgeun.shop.order.dto.response;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 주문 상세
 * 1. 주문 기본 정보
 * 2. 주문자 정보
 * 3. 상품 정보 (여러 건)
 * 4. 배송 정보
 * 5. 금액 정보
 */
@Getter
@Builder
public class AdminOrderDetailResponse {

    // ===== 주문 기본 정보 =====
    private Long orderId;
    private OrderStatus status;
    private LocalDateTime orderedAt;
    private LocalDateTime canceledAt;

    // ===== 주문자 정보 =====
    private Long buyerId;
    private String buyerEmail;
    private String buyerName;
    private String buyerPhone;

    // ===== 상품 정보 (스냅샷 기준) =====
    private List<OrderItemLine> items;

    // ===== 금액 정보 =====
    private int subtotal;
    private int shippingFee;
    private int totalPrice;

    // ===== 배송 정보 =====
    private String recipientName;
    private String recipientPhone;
    private String shippingZipCode;
    private String shippingAddress;
    private String requestMessage;

    @Getter
    @Builder
    public static class OrderItemLine {
        private Long productId;
        private String productName;
        private int unitPrice;
        private int quantity;
        private int lineTotal;

        public static OrderItemLine from(OrderItem item) {
            if (item == null) throw new IllegalArgumentException("item은 필수입니다.");
            return OrderItemLine.builder()
                    .productId(item.getProductId())
                    .productName(item.getProductNameSnapshot())
                    .unitPrice(item.getUnitPriceSnapshot())
                    .quantity(item.getQuantity())
                    .lineTotal(item.lineTotal())
                    .build();
        }
    }

    public static AdminOrderDetailResponse from(Order order, Member buyer) {
        if (order == null) throw new IllegalArgumentException("order는 필수입니다.");
        if (buyer == null) throw new IllegalArgumentException("buyer는 필수입니다.");

        List<OrderItemLine> items = order.getOrderItems().stream()
                .map(OrderItemLine::from)
                .toList();

        return AdminOrderDetailResponse.builder()
                // ===== 주문 기본 =====
                .orderId(order.getId())
                .status(order.getStatus())
                .orderedAt(order.getOrderedAt())
                .canceledAt(order.getCanceledAt())

                // ===== 주문자 =====
                .buyerId(buyer.getId())
                .buyerEmail(buyer.getEmail())
                .buyerName(buyer.getName())
                .buyerPhone(buyer.getPhone())

                // ===== 상품 라인 =====
                .items(items)

                // ===== 금액 =====
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .totalPrice(order.getTotalPrice())

                // ===== 배송 =====
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingZipCode(order.getShippingZipCode())
                .shippingAddress(order.getShippingAddress())
                .requestMessage(order.getRequestMessage())

                .build();
    }
}