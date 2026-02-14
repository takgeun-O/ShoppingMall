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

        log.info("memberId={}, productId={}, productStatus={}",
                memberId, productId, productService.getPublic(productId).getStatus());

        // 로그인 상태 검증
        validateAuthenticated(memberId);

        // 회원 상태 검증
        Member member = memberService.get(memberId);
        if(member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("비활성 회원은 주문할 수 없습니다.");
        }

        // 상품 상태 검증
        Product product = productService.getPublic(productId);
        if(product.getStatus() != ProductStatus.ON_SALE) {
            throw new ConflictException("판매 중인 상품만 주문할 수 있습니다.");
        }

        // 재고 검증/차감
        if(quantity < 1) {
            throw new IllegalArgumentException("quantity는 1 이상입니다.");
        }

//        int productStock = product.getStock();
//        if(productStock < quantity) {
//            throw new ConflictException("판매 중인 상품의 재고가 주문 수량보다 적습니다.");
//        }
//        int decreasedStock = productStock - quantity;
//        productService.update(productId, null, null, null, decreasedStock, null);

        // 주문서비스에서 상품 수정을 하면 재고 변경 규칙/상품 변경 규칙이 섞일 수 있고
        // null을 여러 개 넘기는 방식은 유지보수에 좋지 않음.
        // 또한 stock 읽고, 비교하고, update 호출 방식은 동시 요청이 들어왔을 때 동시성 문제가 발생할 가능성이 있음.
        // 검증 + 차감은 Product 도메인 하나에서 처리하도록 하자.
        product.decreaseStock(quantity);    // 내부에서 재고 부족이면 ConflictException
        productService.save(product);       // 저장 반영은 ProductService 에서

        // 스냅샷
        String productNameSnapshot = product.getName();
        int unitPriceSnapshot = product.getPrice();

        // 주문 생성 + 저장
        Order order = Order.create(
                memberId, productId, productNameSnapshot, unitPriceSnapshot, quantity, recipientName, recipientPhone,
                shippingZipCode, shippingAddress, requestMessage
        );
        return orderRepository.save(order).getId();
    }

    // 내 주문 목록 조회
    public List<Order> getMyOrders(Long memberId) {
        validateAuthenticated(memberId);

        return orderRepository.findAllByMemberId(memberId);
    }

    // 주문 상세 조회
    public OrderResponse getDetail(Long memberId, Long orderId) {
        validateAuthenticated(memberId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문이 존재하지 않습니다."));

        // 본인 주문 여부
        if(!memberId.equals(order.getMemberId())) {
            throw new ForbiddenException("본인 주문만 조회할 수 있습니다.");
        }

        return OrderResponse.from(order);
    }

    // 단일 주문 취소
    public void cancel(Long memberId, Long orderId) {
        validateAuthenticated(memberId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문이 존재하지 않습니다."));

        // 본인 주문 여부
        if(!memberId.equals(order.getMemberId())) {
            throw new ForbiddenException("본인 주문만 취소할 수 있습니다.");
        }

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

    private void validateAuthenticated(Long memberId) {
        if(memberId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
    }
}
