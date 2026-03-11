package io.github.takgeun.shop.order.view.dto.admin;

import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.Getter;

@Getter
public class AdminOrderItemView {

    private final Long id;
    private final String orderNumber;
    private final String customerName;
    private final String customerEmail;
    private final String representativeProductName; // 디자이너 핸드백 외 1개 이런 식
    private final int totalAmount;
    private final OrderStatus status;
    private final String orderDate;
    private final int itemCount;    // (주문개수 - 1)개

    private AdminOrderItemView(Long id,
                              String orderNumber,
                              String customerName,
                              String customerEmail,
                              String representativeProductName,
                              int totalAmount,
                              OrderStatus status,
                              String orderDate,
                              int itemCount) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.representativeProductName = representativeProductName;
        this.totalAmount = totalAmount;
        this.status = status;
        this.orderDate = orderDate;
        this.itemCount = itemCount;
    }

    public static AdminOrderItemView of(
            Long id,
            String orderNumber,
            String customerName,
            String customerEmail,
            String representativeProductName,
            int totalAmount,
            OrderStatus status,
            String orderDate,
            int itemCount
    ) {
        return new AdminOrderItemView(
                id,
                orderNumber,
                customerName,
                customerEmail,
                representativeProductName,
                totalAmount,
                status,
                orderDate,
                itemCount);
    }
}
