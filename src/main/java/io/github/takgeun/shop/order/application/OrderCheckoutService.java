package io.github.takgeun.shop.order.application;

import io.github.takgeun.shop.cart.application.CartService;
import io.github.takgeun.shop.cart.infra.SessionCartRepository;
import io.github.takgeun.shop.cart.view.dto.CartViewResult;
import io.github.takgeun.shop.global.error.ForbiddenException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.order.application.dto.CreateOrderCommand;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderRepository;
import io.github.takgeun.shop.order.domain.OrderStatus;
import io.github.takgeun.shop.order.view.dto.OrderCompleteView;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCheckoutService {

    private static final String LAST_ORDER_VIEW_KEY_PREFIX = "LAST_ORDER_VIEW";

    private final CartService cartService;
    private final SessionCartRepository sessionCartRepository;
    private final OrderRepository orderRepository;

    /**
     * 장바구니 기반 주문 생성
     * 결제 연동 전 : 생성 즉시 결제완료로 일단 지정
     * 주문 생성 후 : 장바구니 비우기
     */
    public Long createOrderFromCart(Long memberId, HttpSession session, CreateOrderCommand cmd) {
        validateInputs(memberId, session, cmd);

        CartViewResult cartView = cartService.getCartView(session);
        if(cartView.getItems().isEmpty()) {
            throw new NotFoundException("장바구니가 비어있습니다.");
        }

        // 카트로부터 주문정보를 만들 것.
        Order order = createOrder(memberId, cartView, cmd);

        // 상태, 시간 세팅
        order.changeStatus(OrderStatus.PAYMENT_COMPLETED);

        // 저장
        orderRepository.save(order);

        // 주문완료 화면은 세션에 캐시
        OrderCompleteView completeView = buildCompleteViewFromOrder(order);
        session.setAttribute(orderViewKey(memberId, order.getId()), completeView);

        // 주문 생성 후 카트 비우기
        sessionCartRepository.clear(session);

        return order.getId();
    }

    public OrderCompleteView getOrderCompleteView(HttpSession session, Long memberId, Long orderId) {
        validateKeyInputs(memberId, session, orderId);

        Object obj = session.getAttribute(orderViewKey(memberId, orderId));
        if(obj instanceof OrderCompleteView view) {
            return view;
        }

        // fallback
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문 정보를 찾을 수 없습니다."));

        if(!order.getMemberId().equals(memberId)) {
            throw new ForbiddenException("접근 권한이 없습니다.");
        }

        return buildCompleteViewFromOrder(order);
    }

    private Order createOrder(Long memberId, CartViewResult cartView, CreateOrderCommand cmd) {
        List<OrderItem> orderItems = cartView.getItems().stream()
                .map(i -> OrderItem.of(
                        i.getProductId(),
                        i.getName(),
                        i.getUnitPrice(),
                        i.getOriginalPrice(),
                        i.getQuantity(),
                        i.getImageUrl()
                ))
                .toList();

        int shippingFee = cartView.getSummary().getShippingFee();

        return Order.create(
                memberId,
                orderItems,
                cmd.getRecipientName(),
                cmd.getPhoneNumber(),
                cmd.getZipCode(),
                cmd.getAddress(),
                cmd.getAddressDetail(),
                cmd.getRequestMessage(),
                shippingFee
        );
    }

    // 카트 정보로부터 가져오기
    private OrderCompleteView buildCompleteViewFromCart(long orderId, long memberId, CartViewResult cartView, CreateOrderCommand cmd) {

        List<OrderCompleteView.OrderItemView> items = cartView.getItems().stream()
                .map(i -> new OrderCompleteView.OrderItemView(
                        i.getProductId(),
                        i.getName(),
                        i.getUnitPrice(),
                        i.getOriginalPrice(),
                        i.getQuantity(),
                        i.getImageUrl()
                ))
                .toList();

        OrderCompleteView.ShippingView shipping = new OrderCompleteView.ShippingView(
                cmd.getRecipientName(),
                cmd.getPhoneNumber(),
                cmd.getZipCode(),
                cmd.getAddress(),
                cmd.getAddressDetail(),
                cmd.getRequestMessage()
        );

        OrderCompleteView.PaymentView payment = new OrderCompleteView.PaymentView(
                cartView.getSummary().getSubtotal(),
                cartView.getSummary().getShippingFee(),
                cartView.getSummary().getTotal()
        );

        return new OrderCompleteView(
                orderId,
                LocalDateTime.now(),
                OrderStatus.PAYMENT_COMPLETED,
                items,
                shipping,
                payment
        );
    }

    private OrderCompleteView buildCompleteViewFromOrder(Order order) {
        List<OrderCompleteView.OrderItemView> items = order.getOrderItems().stream()
                .map(i -> new OrderCompleteView.OrderItemView(
                        i.getProductId(),
                        i.getProductNameSnapshot(),
                        i.getUnitPriceSnapshot(),
                        (i.getOriginalPriceSnapshot() == null ? 0 : i.getOriginalPriceSnapshot()),
                        i.getQuantity(),
                        i.getImageUrlSnapshot()     // 주문 당시 스냅샷이미지 적용 (상품 삭제시에도 유지되게끔)
                ))
                .toList();

        OrderCompleteView.ShippingView shipping = new OrderCompleteView.ShippingView(
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getShippingZipCode(),
                order.getShippingAddress(),
                order.getShippingAddressDetail(),
                order.getRequestMessage()
        );

        OrderCompleteView.PaymentView payment = new OrderCompleteView.PaymentView(
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTotalPrice()
        );

        return new OrderCompleteView(
                order.getId(),
                order.getOrderedAt(),
                order.getStatus(),
                items,
                shipping,
                payment
        );
    }

    private void validateInputs(Long memberId, HttpSession session, CreateOrderCommand cmd) {
        if(session == null) {
            throw new IllegalArgumentException("session은 필수입니다.");
        }
        if(memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("memberId는 양수여야 합니다.");
        }
        if(cmd == null) {
            throw new IllegalArgumentException("주문 생성 정보는 필수입니다.");
        }
    }

    private void validateKeyInputs(Long memberId, HttpSession session, Long orderId) {
        if (session == null) {
            throw new IllegalArgumentException("session은 필수입니다.");
        }
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("memberId는 양수여야 합니다.");
        }
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId는 양수여야 합니다.");
        }
    }

    private String orderViewKey(Long memberId, long orderId) {
        // LAST_ORDER_VIEW:member=3:order=12
        return LAST_ORDER_VIEW_KEY_PREFIX + ":member=" + memberId + ":order=" + orderId;
    }
}
