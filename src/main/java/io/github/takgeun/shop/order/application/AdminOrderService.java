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
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated              // @NotNull, @Positive 사용하기 위함
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;

    // 전체 주문 목록(관리자)
    public List<AdminOrderListResponse> getAll() {

        List<Order> orders = orderRepository.findAll();

        // memberId -> Member 맵
        // toMap(keyMapper, valueMapper, mergeFunction)
        // keyMapper : Map의 key를 어떻게 만들까
        // valueMapper : Map의 value를 어떻게 만들까
        // mergeFunction : key가 중복되면 어떻게 처리할까
        //      (toMap()은 key 중복되면 예외 발생하는데 (a, b) -> a 를 넣어서 중복이면 기존 값 유지해서 예외를 회피)
        // Function.identity() == (x -> x) : 입력값을 그대로 반환하는 함수
        Map<Long, Member> memberMap = memberRepository.findAll().stream()
                .collect(Collectors.toMap(Member::getId, Function.identity(), (a, b) -> a));

        // 주문에 memberId는 있는데 buyer가 없는 케이스도 생각할 것. (NPE 발생)
        return orders.stream()
                .map(order -> {
                    Member buyer = memberMap.get(order.getMemberId());

                    // 탈퇴회원/데이터 불일치 방어
                    if(buyer == null) {
                        buyer = Member.deletedStub(order.getMemberId());
                    }

                    return AdminOrderListResponse.from(order, buyer);
                })
                .toList();
    }

    // 주문 상세(관리자) - 본인 검증 없이
    public AdminOrderDetailResponse getDetailForAdmin(@NotNull @Positive Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다."));

        Member buyer = memberRepository.findById(order.getMemberId())
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다."));

        return AdminOrderDetailResponse.from(order, buyer);
    }

    // 주문 변경
    public void changeStatus(@NotNull @Positive Long orderId,
                             @NotNull OrderStatus newStatus) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다."));

        // 도메인에서 상태변경 검증(ConflictException 등등)
        order.changeStatus(newStatus);

        // 저장
        orderRepository.save(order);
    }
}
