package io.github.takgeun.shop.order.application;

import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.ForbiddenException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.global.error.UnauthorizedException;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderRepository;
import io.github.takgeun.shop.order.domain.OrderStatus;
import io.github.takgeun.shop.order.dto.response.OrderListResponse;
import io.github.takgeun.shop.order.dto.response.OrderResponse;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor    // 필수 인자를 가진 생성자 자동 생성
// requestDTO로 곧바로 받기보다는 컨트롤러에서 풀어서 넘겨오도록 할 것.
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final MemberService memberService;

    /**
     * 주문 생성 (UC-O02)
     * 회원 ACTIVE 검증
     * 상품 존재/판매 가능 상태 검증
     * 재고 충분 검증 + 차감
     * 상품명/단가 스냅샷 생성
     * Order 생성 후 저장
     */
    public Long create(Long memberId, Long productId, int quantity,
                       String recipientName, String recipientPhone,
                       String shippingZipCode, String shippingAddress, String requestMessage) {

        requireAuthenticated(memberId);
        requirePositive(quantity, "quantity");

        Member member = memberService.get(memberId);
        requireActiveMember(member);

        // 주문 가능한 상품
        Product product = productService.getForOrderPublic(productId);
        requireOnSale(product);

        // 재고 검증+차감은 Product 도메인상에서 진행
        product.decreaseStock(quantity);
        productService.save(product);  // 저장 반영은 product 서비스 책임 (상품 저장이니깐)

        // 스냅샷
        String productNameSnapshot = product.getName();
        int unitPriceSnapshot = product.getPrice();

        Order order = Order.create(
                memberId, productId,
                productNameSnapshot, unitPriceSnapshot,
                quantity,
                recipientName, recipientPhone,
                shippingZipCode, shippingAddress, requestMessage
        );

        return orderRepository.save(order).getId();
    }

    // 내 주문 목록 조회
    public List<Order> getMyOrders(Long memberId) {
        requireAuthenticated(memberId);
        return orderRepository.findAllByMemberId(memberId);
    }

    // 주문 상세 조회
    public OrderResponse getDetail(Long memberId, Long orderId) {
        requireAuthenticated(memberId);

        Order order = getOrderOrThrow(orderId);
        requireOwner(memberId, order);      // 자기 자신의 주문인지 검증

        return OrderResponse.from(order);
    }

    // 단일 주문 취소
    public void cancel(Long memberId, Long orderId) {
        requireAuthenticated(memberId);

        Order order = getOrderOrThrow(orderId);
        requireOwner(memberId, order);

        // 이미 취소된 주문
        if(order.getStatus() == OrderStatus.CANCELED) {
            throw new ConflictException("이미 취소된 주문입니다.");
        }

        // 주문 상태 변경
        order.cancel();

        // 재고 원복
//        Product product = productService.get(order.getProductId());   // 아 뭔가 Order도메인이 Product 도메인 건드는 게 마음에 안 들음.
//        product.increaseStock(order.getQuantity());
        productService.increaseStock(order.getProductId(), order.getQuantity());

        // 저장 반영
        orderRepository.save(order);
    }



    // Helper 메소드들
    private void requireAuthenticated(Long memberId) {
        if(memberId == null) throw new UnauthorizedException("로그인이 필요합니다.");
    }

    private void requirePositive(int value, String fieldName) {
        if (value < 1) throw new IllegalArgumentException(fieldName + "는 1 이상입니다.");
    }

    private void requireActiveMember(Member member) {
        if(member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("비활성 회원은 주문할 수 없습니다.");
        }
    }
    private void requireOnSale(Product product) {
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new ConflictException("판매 중인 상품만 주문할 수 있습니다.");
        }
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문이 존재하지 않습니다."));
    }

    private void requireOwner(Long memberId, Order order) {
        if (!memberId.equals(order.getMemberId())) {
            throw new ForbiddenException("본인 주문만 처리할 수 있습니다.");
        }
    }
}
