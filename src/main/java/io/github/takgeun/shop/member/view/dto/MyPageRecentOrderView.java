package io.github.takgeun.shop.member.view.dto;

import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class MyPageRecentOrderView {
    private final String productName;
    private final LocalDateTime orderedAt;
    private final int totalPrice;
    private final OrderStatus status;

    public static List<MyPageRecentOrderView> stub() {
        return List.of(
                new MyPageRecentOrderView("상품명", LocalDateTime.now().minusDays(2), 289000, OrderStatus.SHIPPING),
                new MyPageRecentOrderView("상품명2", LocalDateTime.now().minusDays(10), 99000, OrderStatus.DELIVERED)
        );
    }
}
