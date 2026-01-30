package io.github.takgeun.shop.order.application;

import io.github.takgeun.shop.global.error.ForbiddenException;
import io.github.takgeun.shop.global.error.UnauthorizedException;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderRepository;
import io.github.takgeun.shop.order.dto.response.AdminOrderListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final MemberService memberService;

    // 전체 주문 목록 조회
    public List<AdminOrderListResponse> getAll(Long memberId) {
        if(memberId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        Member member = memberService.get(memberId);
        if(member.getRole() != MemberRole.ADMIN) {
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }

        List<Order> orders = orderRepository.findAll();

        return orders.stream()
                .map(AdminOrderListResponse::from)
                .toList();
    }
}
