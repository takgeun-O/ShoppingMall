package io.github.takgeun.shop.order.application;

import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRepository;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderRepository;
import io.github.takgeun.shop.order.domain.OrderStatus;
import io.github.takgeun.shop.order.dto.response.AdminOrderListResponse;
import io.github.takgeun.shop.order.view.dto.admin.AdminOrderDetailView;
import io.github.takgeun.shop.order.view.dto.admin.AdminOrderItemView;
import io.github.takgeun.shop.order.view.dto.admin.AdminOrderSummaryView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;

    /**
     * 관리자 주문 목록 조회
     */
    public List<AdminOrderItemView> getOrderList(String keyword, String status) {
        String normalizedKeyword = normalize(keyword);
        OrderStatus statusFilter = parseStatus(status);

        return orderRepository.findAll().stream()
                .filter(order -> matchesKeyword(order, normalizedKeyword))
                .filter(order -> matchesStatus(order, statusFilter))
                .map(this::toAdminOrderItemView)
                .toList();
    }

    /**
     * 관리자 주문 요약
     */
    public AdminOrderSummaryView getOrderSummary() {
        List<Order> orders = orderRepository.findAll();

        int totalCount = orders.size();
        int orderedCount = countByStatus(orders, OrderStatus.ORDERED);
        int paymentCompletedCount = countByStatus(orders, OrderStatus.PAYMENT_COMPLETED);
        int preparingCount = countByStatus(orders, OrderStatus.PREPARING);
        int shippingCount = countByStatus(orders, OrderStatus.SHIPPING);
        int deliveredCount = countByStatus(orders, OrderStatus.DELIVERED);
        int canceledCount = countByStatus(orders, OrderStatus.CANCELED);

        return AdminOrderSummaryView.of(
                totalCount,
                orderedCount,
                paymentCompletedCount,
                preparingCount,
                shippingCount,
                deliveredCount,
                canceledCount
        );
    }

    /**
     * 관리자 주문 상세 조회
     */
    public AdminOrderDetailView getDetailForAdmin(Long orderId) {
        Order order = findOrder(orderId);

        Member member = memberRepository.findById(order.getMemberId())
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다."));

        return AdminOrderDetailView.from(order, member);
    }

    /**
     * 관리자 주문 상태 변경
     */
    @Transactional
    public void changeStatus(Long orderId, OrderStatus newStatus) {
        if(newStatus == null) {
            throw new IllegalArgumentException("변경할 주문 상태는 필수입니다.");
        }

        Order order = findOrder(orderId);
        order.changeStatus(newStatus);
        orderRepository.save(order);
    }

    /**
     * AdminOrderListResponse DTO 전용 getAll()
     */
    public List<AdminOrderListResponse> getAll() {
        List<Order> orders = orderRepository.findAll();

        return orders.stream()
                .map(order -> {
                    Member buyer = memberRepository.findById(order.getMemberId())
                            .orElseThrow(() -> new NotFoundException("주문자의 회원 정보를 찾을 수 없습니다. memberId=" + order.getMemberId()));
                    return AdminOrderListResponse.from(order, buyer);
                })
                .toList();
    }

    /**
     * 현재 주문 상태에 변경 가능한 상태만 반환하기
     * ORDERED → PAYMENT_COMPLETED 또는 CANCELED
     * PAYMENT_COMPLETED → PREPARING 또는 CANCELED
     * PREPARING → SHIPPING (이 단계부터 취소 막음.)
     * SHIPPING → DELIVERED
     * DELIVERED / CANCELED → 변경 불가(종료 상태)
     */
    public List<OrderStatus> getAvailableNextStatuses(Long orderId) {
        Order order = findOrder(orderId);

        return switch (order.getStatus()) {
            case ORDERED -> List.of(OrderStatus.ORDERED, OrderStatus.PAYMENT_COMPLETED, OrderStatus.CANCELED);
            case PAYMENT_COMPLETED -> List.of(OrderStatus.PAYMENT_COMPLETED, OrderStatus.PREPARING, OrderStatus.CANCELED);
            case PREPARING -> List.of(OrderStatus.PREPARING, OrderStatus.SHIPPING);
            case SHIPPING -> List.of(OrderStatus.SHIPPING, OrderStatus.DELIVERED);
            case DELIVERED -> List.of(OrderStatus.DELIVERED);
            case CANCELED -> List.of(OrderStatus.CANCELED);
        };
    }


    private String normalize(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase();
    }

    private OrderStatus parseStatus(String status) {
        if(status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            // "ALL".equalsIgnoreCase(status) : 대소문자 무시하고 비교
            return null;
        }
        return OrderStatus.valueOf(status);
    }

    private boolean matchesKeyword(Order order, String keyword) {
        if(keyword.isBlank()) {
            return true;        // 검색어 없으면 전체
        }
        return contains(buildOrderNumber(order), keyword)
                || contains(getCustomerName(order), keyword)
                || contains(getCustomerEmail(order), keyword)
                || contains(getRepresentativeProductName(order.getOrderItems()), keyword);
    }

    private boolean matchesStatus(Order order, OrderStatus statusFilter) {
        if(statusFilter == null) {
            return true;        // 검색어 없으면 전체
        }
        return order.getStatus() == statusFilter;
    }

    private String buildOrderNumber(Order order) {
        return "ORD-" + order.getId();
    }

    private String getCustomerName(Order order) {
        Member member = memberRepository.findById(order.getMemberId())
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다."));

        return member.getName();
    }

    private String getCustomerEmail(Order order) {
        Member member = memberRepository.findById(order.getMemberId())
                .orElseThrow(() -> new NotFoundException("회원이 존재하지 않습니다."));

        return member.getEmail();
    }

    private String getRepresentativeProductName(List<OrderItem> items) {
        if(items == null || items.isEmpty()) {
            return "주문 상품 없음";
        }
        return items.get(0).getProductNameSnapshot();
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword);
    }

    private AdminOrderItemView toAdminOrderItemView(Order order) {
        List<OrderItem> items = order.getOrderItems();

        String representativeProductName = getRepresentativeProductName(items);
        int itemCount = getItemCount(items);

        return AdminOrderItemView.of(
                order.getId(),
                buildOrderNumber(order),
                getCustomerName(order),
                getCustomerEmail(order),
                representativeProductName,
                order.getTotalPrice(),
                order.getStatus(),
                order.getOrderedAt().format(ORDER_DATE_FORMAT),
                itemCount
        );
    }

    private int getItemCount(List<OrderItem> items) {
        if(items == null) {
            return 0;
        }
        return items.size();
    }

    private int countByStatus(List<Order> orders, OrderStatus status) {
        return (int) orders.stream()
                .filter(order -> order.getStatus() == status)
                .count();
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다."));
    }
}
