package io.github.takgeun.shop.member.application;

import io.github.takgeun.shop.member.view.dto.MyPageOrderSummaryView;
import io.github.takgeun.shop.member.view.dto.MyPageRecentOrderView;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderRepository;
import io.github.takgeun.shop.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPageQueryService {

    private final OrderRepository orderRepository;

    public MyPageOrderSummaryView getOrderSummary(Long memberId) {
        List<Order> orders = orderRepository.findAllByMemberId(memberId);

        int ordered = 0, paid = 0, ready = 0, shipping = 0, completed = 0, canceled = 0;

        for (Order o : orders) {
            OrderStatus s = o.getStatus();
            if(s == null) continue;

            switch (s) {
                case ORDERED -> ordered++;
                case PAYMENT_COMPLETED -> paid++;
                case PREPARING -> ready++;
                case SHIPPING -> shipping++;
                case DELIVERED -> completed++;
                case CANCELED -> canceled++;
                default -> { /* 상태가 더 있으면 여기다가 추가 작성 */ }
            }
        }

        return MyPageOrderSummaryView.of(ordered, paid, ready, shipping, completed, canceled);
    }

    public List<MyPageRecentOrderView> getRecentOrders(Long memberId, int limit) {
        return orderRepository.findAllByMemberId(memberId).stream()
                .sorted(Comparator.comparing(Order::getOrderedAt).reversed())
                .limit(limit)
                .map(MyPageRecentOrderView::from)
                .toList();
    }
}
