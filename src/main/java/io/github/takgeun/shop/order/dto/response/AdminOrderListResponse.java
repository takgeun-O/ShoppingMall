package io.github.takgeun.shop.order.dto.response;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AdminOrderListResponse {

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

    // ===== 상품 요약 정보 =====
    private String productSummary;   // "상품명 외 N건"
    private int totalQuantity;       // 총 수량
    private int totalPrice;          // 총 결제금액

    public static AdminOrderListResponse from(Order order, Member buyer) {

        if (order == null) throw new IllegalArgumentException("order는 필수입니다.");
        if (buyer == null) throw new IllegalArgumentException("buyer는 필수입니다.");

        List<OrderItem> items = order.getItems();

        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("주문 상품이 존재하지 않습니다.");
        }

        // ===== 상품 요약 =====
        String firstProductName = items.getFirst().getProductNameSnapshot();
        int itemCount = items.size();

        String productSummary =
                (itemCount == 1)
                        ? firstProductName
                        : firstProductName + " 외 " + (itemCount - 1) + "건";

        int totalQuantity = items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        return AdminOrderListResponse.builder()
                // 주문 기본
                .orderId(order.getId())
                .status(order.getStatus())
                .orderedAt(order.getOrderedAt())
                .canceledAt(order.getCanceledAt())

                // 주문자
                .buyerId(buyer.getId())
                .buyerEmail(buyer.getEmail())
                .buyerName(buyer.getName())
                .buyerPhone(buyer.getPhone())

                // 상품 요약
                .productSummary(productSummary)
                .totalQuantity(totalQuantity)
                .totalPrice(order.getTotalPrice())

                .build();
    }
}