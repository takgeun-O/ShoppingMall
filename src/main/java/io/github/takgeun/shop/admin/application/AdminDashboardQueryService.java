package io.github.takgeun.shop.admin.application;

import io.github.takgeun.shop.admin.view.dto.AdminDashboardView;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRepository;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderRepository;
import io.github.takgeun.shop.order.domain.OrderStatus;
import io.github.takgeun.shop.order.view.dto.admin.AdminOrderItemView;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardQueryService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    /**
     * 대시보드 만들 떄 필요한 데이터
     * - 회원 관련 데이터
     * - 주문 관련 데이터
     * - 상품 관련 데이터
     */
    public AdminDashboardView getDashboard() {
        List<Order> orders = orderRepository.findAll();
        List<Member> members = memberRepository.findAll();
        List<Product> products = productRepository.findAllAdmin();

        int totalOrderCount = orders.size();
        int totalMemberCount = members.size();
        int totalProductCount = products.size();
        int totalRevenue = calculateTotalRevenue(orders);   // 취소된 주문 제외

        List<AdminOrderItemView> recentOrders = orders.stream()
                .sorted(Comparator.comparing(Order::getOrderedAt).reversed())
                .limit(5)
                .map(this::toAdminOrderItemView)
                .toList();

        return AdminDashboardView.of(
                totalOrderCount,
                totalMemberCount,
                totalProductCount,
                totalRevenue,
                recentOrders
        );
    }

    private AdminOrderItemView toAdminOrderItemView(Order order) {

        Member member = memberRepository.findById(order.getMemberId())
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        String representativeProductName = "";      // 최근 주문 목록에 들어갈 상품 이름 문자열

        int itemCount = 0;
        List<OrderItem> items = order.getOrderItems();
        if(items != null && !items.isEmpty()) {
            representativeProductName = items.get(0).getProductNameSnapshot();      // 주문 첫 번째 상품이름 스냅샷
            itemCount = Math.max(items.size() - 1, 0);                              // "외 N건" 에 들어갈 카운트 (타임리프 뷰에서 1 이상일 때만 텍스트 노출시킬 것)
        }

        return AdminOrderItemView.of(
                order.getId(),
                order.getOrderNumber(),
                member.getName(),
                member.getEmail(),
                representativeProductName,
                order.getTotalPrice(),
                order.getStatus(),
                order.getOrderedAt().format(DATE_FORMATTER),
                itemCount
        );
    }

    private int calculateTotalRevenue(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getStatus() != null)     // 객체니까 null 검증
                .filter(order -> order.getStatus() != OrderStatus.CANCELED)
                .mapToInt(Order::getTotalPrice)
                .sum();
    }
}
