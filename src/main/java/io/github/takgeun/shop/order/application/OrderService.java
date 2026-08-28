package io.github.takgeun.shop.order.application;

import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.ForbiddenException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.global.error.exception.UnauthorizedException;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.order.application.dto.CreateOrderCommand;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor    // 필수 인자를 가진 생성자 자동 생성
@Transactional(readOnly = true)
public class OrderService {

    private static final int FREE_SHIPPING_THRESHOLD = 30_000;
    private static final int SHIPPING_FEE = 3_000;
    private static final DateTimeFormatter ORDER_NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final MemberService memberService;

    /**
     * 카트/바로구매 모두에서 사용하는 단일 진입점
     * 세션 모름 (서비스가 세션에 의존하는 문제점 해결)
     */
    @Transactional
    public Long checkout(Long memberId,
                         List<CheckoutItem> checkoutItems,
                         CreateOrderCommand cmd
    ) {
        requireAuthenticated(memberId);
        validateCreateOrderCommand(cmd);
        validateCheckoutItems(checkoutItems);

        // member ACTIVE 검증
        Member member = memberService.findById(memberId);
        requireActiveMember(member);

        // 이미 처리된 requestKey인지 먼저 확인
        orderRepository.findByRequestKey(cmd.getRequestKey())
                .ifPresent(existing -> {
                    throw new ConflictException("이미 처리된 주문 요청입니다.");
                });

        // product조회 + stock 감소 + OrderItem 스냅샷 생성
        List<OrderItem> orderItems = new ArrayList<>();
        int subtotal = 0;

        for (CheckoutItem checkoutItem : checkoutItems) {
            if(checkoutItem == null) {
                continue;
            }
            if(checkoutItem.getQuantity() <= 0) {
                continue;
            }

            Product product = productService.getForOrder(checkoutItem.getProductId());
            requireOnSale(product);

            // 트랜잭션 주의
            // 재고를 먼저 줄이고 주문 저장
            product.decreaseStock(checkoutItem.getQuantity());  // 여기서 예외 발생 시 롤백
            productService.save(product);

            OrderItem orderItem = OrderItem.of(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    product.getOriginalPrice(),
                    checkoutItem.getQuantity(),
                    product.getImageUrl()
            );

            orderItems.add(orderItem);
            subtotal += orderItem.lineTotal();
        }

        if(orderItems.isEmpty()) {
            throw new ConflictException("유효한 주문 상품이 없습니다.");
        }

        int shippingFee = (subtotal >= FREE_SHIPPING_THRESHOLD) ? 0 : SHIPPING_FEE;
        String orderNumber = generateOrderNumber();

        Order order = Order.create(
                memberId,
                orderNumber,
                cmd.getRequestKey(),
                orderItems,
                cmd.getRecipientName(),
                cmd.getPhoneNumber(),
                cmd.getZipCode(),
                cmd.getAddress(),
                cmd.getAddressDetail(),
                cmd.getRequestMessage(),
                shippingFee
        );

        // MVP : 결제 성공 가정함.
        order.markPaymentCompleted();

        Order savedOrder = orderRepository.save(order);

        log.info("주문 생성 완료 : orderId={}, orderNumber={}, memberId={}, requestKey={}, itemCount={}, subtotal={}, shippingFee={}, totalPrice={}",
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                memberId,
                cmd.getRequestKey(),
                orderItems.size(),
                subtotal,
                shippingFee,
                savedOrder.getTotalPrice());

        return savedOrder.getId();
    }

    public List<Order> getMyOrders(Long memberId) {
        requireAuthenticated(memberId);
        return orderRepository.findAllByMemberId(memberId);     // 각 주문에 orderItems를 붙인 주문을 반환
    }

    public OrderResponse getDetail(Long memberId, Long orderId) {
        requireAuthenticated(memberId);

        Order order = getOrderOrThrow(orderId);
        requireOwner(memberId, order);

        return OrderResponse.from(order);
    }

    @Transactional
    public void cancel(Long memberId, Long orderId) {
        requireAuthenticated(memberId);

        Order order = getOrderOrThrow(orderId);
        requireOwner(memberId, order);

        if(order.getStatus() == OrderStatus.CANCELED) {
            throw new ConflictException("이미 취소된 주문입니다.");
        }

        order.changeStatus(OrderStatus.CANCELED);

        // 재고 원복
        for (OrderItem item : order.getOrderItems()) {
            productService.increaseStock(item.getProductId(), item.getQuantity());
        }

        orderRepository.save(order);
    }




    // 아래는 Helper 메소드들

    private void validateCheckoutForm(CheckoutForm form) {
        if(form == null) throw new IllegalArgumentException("form은 필수입니다.");
    }

    private void validateCheckoutItems(List<CheckoutItem> checkoutItems) {
        if(checkoutItems == null || checkoutItems.isEmpty()) {
            throw new ConflictException("주문 상품이 없습니다.");
        }
    }

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

    /**
     * 외부 노출용 주문번호 생성
     * ORD-20260312160533-A1B2C3
     */
    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(ORDER_NUMBER_DATE_FORMAT);
        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();
        return "ORD-" + timestamp + "-" + suffix;
    }

    private void validateCreateOrderCommand(CreateOrderCommand cmd) {
        if(cmd == null) {
            throw new IllegalArgumentException("주문 생성 정보는 필수입니다.");
        }
        if(cmd.getRequestKey() == null || cmd.getRequestKey().isBlank()) {
            throw new IllegalArgumentException("requestKey는 필수입니다.");
        }
    }
}
