package io.github.takgeun.shop.order.application;

import io.github.takgeun.shop.cart.infra.SessionCartRepository;
import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.ForbiddenException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.global.error.UnauthorizedException;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderRepository;
import io.github.takgeun.shop.order.domain.OrderStatus;
import io.github.takgeun.shop.order.dto.request.CheckoutItem;
import io.github.takgeun.shop.order.dto.response.OrderResponse;
import io.github.takgeun.shop.order.view.form.CheckoutForm;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor    // 필수 인자를 가진 생성자 자동 생성
public class OrderService {

    private static final int FREE_SHIPPING_THRESHOLD = 30_000;
    private static final int SHIPPING_FEE = 3_000;

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final MemberService memberService;

    /**
     * 카트/바로구매 모두에서 사용하는 단일 진입점
     * 세션 모름 (서비스가 세션에 의존하는 문제점 해결)
     */
    public Long checkout(Long memberId, List<CheckoutItem> checkoutItems, CheckoutForm form) {
        requireAuthenticated(memberId);
        if(form == null) throw new IllegalArgumentException("form은 필수입니다.");

        // member ACTIVE 검증
        Member member = memberService.get(memberId);
        requireActiveMember(member);

        if(checkoutItems == null || checkoutItems.isEmpty()) {
            throw new ConflictException("주문 상품이 없습니다.");
        }

        // product조회 + stock 감소 + OrderItem 스냅샷 생성
        List<OrderItem> items = new ArrayList<>();
        int subtotal = 0;

        for (CheckoutItem checkoutItem : checkoutItems) {
            if(checkoutItem == null) continue;
            if(checkoutItem.getQuantity() <= 0) continue;

            Product product = productService.getForOrderPublic(checkoutItem.getProductId());
            requireOnSale(product);

            product.decreaseStock(checkoutItem.getQuantity());
            productService.save(product);

            OrderItem item = OrderItem.of(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    product.getOriginalPrice(),
                    checkoutItem.getQuantity(),
                    product.getImageUrl()
            );

            items.add(item);
            subtotal += item.lineTotal();
        }

        if(items.isEmpty()) {
            throw new ConflictException("유효한 주문 상품이 없습니다.");
        }

        int shippingFee = (subtotal >= FREE_SHIPPING_THRESHOLD) ? 0 : SHIPPING_FEE;

        Order order = Order.create(
                memberId,
                items,
                form.getRecipientName(),
                form.getPhoneNumber(),
                form.getZipCode(),
                form.getAddress(),
                form.getAddressDetail(),
                form.getRequestMessage(),
                shippingFee
        );

        // MVP : 결제 성공 가정
        order.changeStatus(OrderStatus.PAYMENT_COMPLETED);

        return orderRepository.save(order).getId();
    }

    public List<Order> getMyOrders(Long memberId) {
        requireAuthenticated(memberId);
        return orderRepository.findAllByMemberId(memberId);
    }

    public OrderResponse getDetail(Long memberId, Long orderId) {
        requireAuthenticated(memberId);

        Order order = getOrderOrThrow(orderId);
        requireOwner(memberId, order);

        return OrderResponse.from(order);
    }

    public void cancel(Long memberId, Long orderId) {
        requireAuthenticated(memberId);

        Order order = getOrderOrThrow(orderId);
        requireOwner(memberId, order);

        if(order.getStatus() == OrderStatus.CANCELED) {
            throw new ConflictException("이미 취소된 주문입니다.");
        }

        order.changeStatus(OrderStatus.CANCELED);

        // 재고 원복
        for (OrderItem item : order.getItems()) {
            productService.increaseStock(item.getProductId(), item.getQuantity());
        }

        orderRepository.save(order);
    }




    // 아래는 Helper 메소드들

    private void requireAuthenticated(Long memberId) {
        if(memberId == null) throw new UnauthorizedException("로그인이 필요합니다.");
    }

    private void requireActiveMember(Member member) {
        if(member == null) throw new UnauthorizedException("로그인이 필요합니다.");
        if(member.getStatus() != MemberStatus.ACTIVE) throw new ForbiddenException("비활성 회원은 주문할 수 없습니다.");
    }

    private void requireOnSale(Product product) {
        if(product.getStatus() != ProductStatus.ON_SALE) {
            throw new ConflictException("판매 중인 상품만 주문할 수 있습니다.");
        }
    }

    private Order getOrderOrThrow(Long orderId) {
        if(orderId == null || orderId <= 0) throw new IllegalArgumentException("orderId는 양수여야 합니다.");
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문이 존재하지 않습니다."));
    }

    private void requireOwner(Long memberId, Order order) {
        if(!memberId.equals(order.getMemberId())) {
            throw new ForbiddenException("본인 주문만 처리할 수 있습니다.");
        }
    }
}
