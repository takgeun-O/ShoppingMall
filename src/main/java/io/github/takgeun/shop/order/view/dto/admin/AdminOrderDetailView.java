package io.github.takgeun.shop.order.view.dto.admin;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
public class AdminOrderDetailView {

    private static final DateTimeFormatter ORDERED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
    private static final int SHIPPING_FEE = 3000;
    private static final int FREE_SHIPPING_THRESHOLD = 30000;

    // 주문 정보
    private final Long id;
    private final String orderNumber;
    private final String orderDate;
    private final OrderStatus status;
    private final String paymentStatus;

    // 고객 정보
    private final String customerName;
    private final String customerEmail;
    private final String customerPhone;

    // 배송 정보
    private final String recipientName;
    private final String recipientPhone;
    private final String zipCode;
    private final String address;
    private final String addressDetail;
    private final String requestMessage;

    // 주문 상품
    private final List<AdminOrderProductItemView> products;
    private final int productAmount;
    private final int shippingFee;
    private final int discountAmount;
    private final int finalAmount;

    private AdminOrderDetailView(Long id,
                                String orderNumber,
                                String orderDate,
                                OrderStatus status,
                                String paymentStatus,
                                String customerName,
                                String customerEmail,
                                String customerPhone,
                                String recipientName,
                                String recipientPhone,
                                String zipCode,
                                String address,
                                String addressDetail,
                                String requestMessage,
                                List<AdminOrderProductItemView> products,
                                int productAmount,
                                int shippingFee,
                                int discountAmount,
                                int finalAmount) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.requestMessage = requestMessage;
        this.products = products;
        this.productAmount = productAmount;
        this.shippingFee = shippingFee;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
    }

    // Order + Member -> View DTO 변환하는거니 from()
    public static AdminOrderDetailView from(Order order, Member member) {
        List<AdminOrderProductItemView> products = order.getOrderItems().stream()
                .map(AdminOrderProductItemView::from)
                .toList();

        int productAmount = order.getOrderItems().stream()
                .mapToInt(OrderItem::lineTotal)
                .sum();

        int shippingFee = productAmount >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;
        int discountAmount = 0;
        int finalAmount = order.getTotalPrice();

        return new AdminOrderDetailView(
                order.getId(),
                buildOrderNumber(order),
                order.getOrderedAt().format(ORDERED_AT_FORMAT),
                order.getStatus(),
                "COMPLETED",
                member.getName(),
                member.getEmail(),
                member.getPhone(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getShippingZipCode(),
                order.getShippingAddress(),
                order.getShippingAddressDetail(),
                order.getRequestMessage(),
                products,
                productAmount,
                shippingFee,
                discountAmount,
                finalAmount
        );
    }

    private static String buildOrderNumber(Order order) {
        return "ORD-" + order.getId();
    }


}
