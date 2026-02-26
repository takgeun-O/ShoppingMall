package io.github.takgeun.shop.order.application;

import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRepository;
import io.github.takgeun.shop.member.infra.MemoryMemberRepository;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderRepository;
import io.github.takgeun.shop.order.domain.OrderStatus;
import io.github.takgeun.shop.order.dto.response.AdminOrderDetailResponse;
import io.github.takgeun.shop.order.dto.response.AdminOrderListResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final MemberService memberService;
    private final MemberRepository memberRepository;

    // 전체 주문 목록(관리자)
    public List<AdminOrderListResponse> getAll() {

        List<Order> orders = orderRepository.findAll();
        Map<Long, Member> memberMap = memberRepository.findAll().stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        return orders.stream()
                .map(order -> {
                    Member buyer = memberMap.get(order.getMemberId());
                    return AdminOrderListResponse.from(order, buyer);
                })
                .toList();
    }

    // 주문 상세(관리자) - 본인 검증 없이
    public AdminOrderDetailResponse getDetailForAdmin(@NotNull @Positive Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다."));

        Member buyer = memberService.get(order.getMemberId());
        if(buyer == null) throw new NotFoundException("주문자를 찾을 수 없습니다.");

        return AdminOrderDetailResponse.from(order, buyer);
    }

    // 주문 변경
    public void changeStatus(@NotNull @Positive Long orderId, OrderStatus newStatus) {

        if(newStatus == null) throw new IllegalArgumentException("상태는 필수입니다.");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다."));

        // 도메인에서 상태변경 검증(ConflictException 등등)
        order.changeStatus(newStatus);

        // 저장
        orderRepository.save(order);
    }
}
