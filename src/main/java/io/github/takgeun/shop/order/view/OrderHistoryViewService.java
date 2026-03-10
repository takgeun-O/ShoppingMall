package io.github.takgeun.shop.order.view;

import io.github.takgeun.shop.order.application.OrderService;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.view.dto.OrderHistoryItemView;
import io.github.takgeun.shop.order.view.dto.OrderHistoryPageView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 뷰 전용 서비스
 * OrderService에서 주문 받아서 View DTO로 바꿔주는 역할
 */
@Service
@RequiredArgsConstructor
public class OrderHistoryViewService {

    private static final int PAGE_SIZE = 8;

    private final OrderService orderService;

    public OrderHistoryPageView getOrderHistoryPage(Long memberId, int page) {
        if(memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("memberId는 양수여야 합니다.");
        }

        List<Order> orders = orderService.getMyOrders(memberId);

        List<OrderHistoryItemView> views = orders.stream()
                .sorted(Comparator.comparing(Order::getOrderedAt).reversed())
                .map(OrderHistoryItemView::from)
                .toList();

        return OrderHistoryPageView.of(views, page, PAGE_SIZE);
    }
}
