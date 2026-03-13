package io.github.takgeun.shop.member.view.dto.admin;

import io.github.takgeun.shop.order.domain.Order;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

/**
 * 주문 테이블 한 줄 표현 DTO
 */
@Getter
public class AdminMemberOrderItemView {

    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final Long id;
    private final String orderNumber;
    private final String orderDate;
    private final int amount;
    private final String status;

    private AdminMemberOrderItemView(Long id,
                                    String orderNumber,
                                    String orderDate,
                                    int amount,
                                    String status) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.amount = amount;
        this.status = status;
    }

    public static AdminMemberOrderItemView from(Order order) {

        String orderDate = order.getOrderedAt() != null
                ? order.getOrderedAt().format(ORDER_DATE_FORMAT)
                : "-";

        return new AdminMemberOrderItemView(
                order.getId(),
                order.getOrderNumber(),
                orderDate,
                order.getTotalPrice(),
                order.getStatus().getLabel()
        );
    }
}
