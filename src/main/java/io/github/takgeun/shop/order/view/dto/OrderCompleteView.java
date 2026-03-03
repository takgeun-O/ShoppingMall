package io.github.takgeun.shop.order.view.dto;

import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제완료 페이지용
 */
@Getter
@AllArgsConstructor     // 모든 필드를 한번에 초기화해도 되는 단순 DTO면 사용 OK (하지만 도메인 무결성이나 검증이 필요한 객체일 땐 신중하게)
public class OrderCompleteView {

    private Long orderId;
    private LocalDateTime orderDate;
    private OrderStatus status;          // 결제완료 등등

    private List<OrderItemView> items;      // 주문 상품
    private ShippingView shipping;          // 배송 정보
    private PaymentView payment;            // 결제 정보

    @Getter
    @AllArgsConstructor
    public static class OrderItemView {
        private Long productId;
        private String name;
        private int unitPrice;
        private Integer originalPrice;
        private int quantity;
        private String imageUrl;
    }

    @Getter
    @AllArgsConstructor
    public static class ShippingView {
        private String recipientName;
        private String phoneNumber;
        private String zipCode;
        private String address;
        private String addressDetail;
        private String requestMessage;
    }

    @Getter
    @AllArgsConstructor
    public static class PaymentView {
        private int subtotal;
        private int shippingFee;
        private int total;
    }
}
