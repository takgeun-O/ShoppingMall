package io.github.takgeun.shop.order.application;

import io.github.takgeun.shop.cart.application.CartService;
import io.github.takgeun.shop.cart.infra.SessionCartRepository;
import io.github.takgeun.shop.cart.view.dto.CartItemView;
import io.github.takgeun.shop.cart.view.dto.CartViewResult;
import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.ForbiddenException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.global.error.UnauthorizedException;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.order.application.dto.CreateOrderCommand;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderRepository;
import io.github.takgeun.shop.order.domain.OrderStatus;
import io.github.takgeun.shop.order.view.dto.OrderCompleteView;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCheckoutService {

    private static final String LAST_ORDER_VIEW_KEY_PREFIX = "LAST_ORDER_VIEW";
    private static final DateTimeFormatter ORDER_NUMBER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MemberService memberService;
    private final CartService cartService;
    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final SessionCartRepository sessionCartRepository;

    /**
     * 장바구니 기반 주문 생성
     * 결제 연동 전 : 생성 즉시 결제완료로 일단 지정
     * 주문 생성 후 : 장바구니 비우기
     *
     * 필요 데이터
     * - memberId
     * - orderNumber
     * - orderItems
     * - cmd (배송 정보들)
     * - 배송비 (주문금액에 따라 계산된 결과)
     */
    public Long createOrderFromCart(Long memberId,
                                    HttpSession session,
                                    CreateOrderCommand cmd
    ) {
        validateInputs(memberId, session, cmd);

        Member member = memberService.findById(memberId);
        requireActiveMember(member);        // 주문할 수 있는 회원 상태인지 검증

        // 이미 요청한 주문이면 예외 처리 -> ConflictException
        ensureNotProcessedRequest(cmd.getRequestKey());

        CartViewResult cartView = cartService.getCartView(session);
        if(cartView.getItems().isEmpty()) {
            throw new ConflictException("장바구니가 비어있습니다.");
        }

        // 실제 상품 기준으로 검증 + 재고 차감 + 주문 아이템 생성
        List<OrderItem> orderItems = createOrderItemsAndDecreaseStock(cartView.getItems());

        if(orderItems.isEmpty()) {
            throw new ConflictException("주문 가능한 상품이 없습니다.");
        }

        int shippingFee = cartView.getSummary().getShippingFee();       // 배송비는 장바구니에 담길 때 계산이 완료된 상태임.
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

        // MVP : 결제 연동 구현하기 전이므로 주문 생성 직후엔 결제완료 처리하기
        order.markPaymentCompleted();

        Order savedOrder = orderRepository.save(order);

        // 주문 완료 화면은 세션에 캐시
        // 이유 : 새로고침 시 주문이 다시 생성되는 것을 방지 (가장 중요)
        // 위 현상을 방지하는 대표적인 방법이 PRG 패턴인데 문제는 redirect를 하면 POST에서 만든 데이터가 사라짐. -> GET /orders/complete 진입할 때 주문 데이터가 없음.
        // 그래서 중간 저장소에 값을 저장할 필요가 있는데 그 역할을 세션이 하는 걸로 결정
        OrderCompleteView completeView = buildCompleteViewFromOrder(savedOrder);
        session.setAttribute(orderViewKey(memberId, savedOrder.getId()), completeView);     // 동일회원의 각 주문을 키로 해서 주문완료정보를 세션에 저장

        // 주문 생성했으면 장바구니 비우기
        sessionCartRepository.clear(session);

        log.info("장바구니 주문 생성 완료: orderId={}, orderNumber={}, memberId={}, itemCount={}, totalPrice={}",
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                memberId,
                savedOrder.getOrderItems().size(),
                savedOrder.getTotalPrice());

        return savedOrder.getId();
    }

    /**
     * 주문 완료 페이지 찾아가기
     * 주문 생성 당시에 저장해놓은 세션을 통해서 찾아내거나
     */
    public OrderCompleteView getOrderCompleteView(HttpSession session, Long memberId, Long orderId) {
        validateKeyInputs(memberId, session, orderId);

        // 세션에 이미 있으면 그걸 우선 사용한다. (굳이 DB를 읽지 않아도 됨) -> 주문 직후에 세션이 살아있을 가능성이 높으니 빠르게 화면을 그릴 수 있음.
        Object obj = session.getAttribute(orderViewKey(memberId, orderId)); // 세션에 여러 종류의 객체가 들어갈 수 있으니 꼭 OrderCompleteView 타입만 꺼내기 위해서
        if(obj instanceof OrderCompleteView view) {
            return view;
        }

        // 세션이 만료되어 없으면 DB에서 주문을 다시 조회한다. -> 세션이 날아가도 주문 데이터가 살아있으면 다시 화면을 복구할 수 있음.
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문 정보를 찾을 수 없습니다."));

        // 조회한 주문이 현재 회원의 주문이 맞는지 확인 (일종의 fallback) -> 사용자가 URL을 바꿔서 다른 사람의 주문에 접근하는 것 방지
        if(!order.getMemberId().equals(memberId)) {
            throw new ForbiddenException("접근 권한이 없습니다.");
        }

        return buildCompleteViewFromOrder(order);
    }



    private String orderViewKey(Long memberId, Long orderId) {
        return LAST_ORDER_VIEW_KEY_PREFIX + ":member=" + memberId + ":order=" + orderId;
    }

    private OrderCompleteView buildCompleteViewFromOrder(Order order) {
        // 주문완료 화면에 뿌려줄 뷰를 조립해서 넘기기
        // items(주문상품), shipping(배송정보), payment(결제정보) 따로 구해야 함.

        // 1. items(주문상품) : 해당 회원이 주문한 전체 주문 상품을 주문완료 화면에 알맞게 변환하기
        List<OrderCompleteView.OrderItemView> items = order.getOrderItems().stream()
                .map(item -> new OrderCompleteView.OrderItemView(
                        item.getProductId(),
                        item.getProductNameSnapshot(),
                        item.getUnitPriceSnapshot(),
                        item.getOriginalPriceSnapshot() == null ? 0 : item.getOriginalPriceSnapshot(),
                        item.getQuantity(),
                        item.getImageUrlSnapshot()
                ))
                .toList();

        // 2. shipping(배송정보)
        OrderCompleteView.ShippingView shipping = new OrderCompleteView.ShippingView(
                order.getRecipientName(),
                order.getOrderNumber(),
                order.getShippingZipCode(),
                order.getShippingAddress(),
                order.getShippingAddressDetail(),
                order.getRequestMessage()
        );

        // 3. payment(결제정보)
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

    /**
     * 실제 상품을 다시 조회해서 --> 각 카트 아이템별 Product 뽑아내기
     * - 판매 가능 여부 검증
     * - 재고 검증 및 차감
     * - 주문 스냅샷 생성
     *
     * 장바구니에 담은 시점의 상품으로 주문을 해야 하기 때문에 스냅샷된 OrderItem 활용 필요
     */
    private List<OrderItem> createOrderItemsAndDecreaseStock(List<CartItemView> cartItems) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItemView cartItem : cartItems) {
            if(cartItem == null) {
                continue;
            }
            if (cartItem.getQuantity() <= 0) {
                throw new ConflictException("주문 수량은 1개 이상이어야 합니다.");
            }

            Product product = productService.getForOrderPublic(cartItem.getProductId());
            requireOrderable(product);

            // 상품 상태 갱신
            product.decreaseStock(cartItem.getQuantity());
            productService.save(product);

            OrderItem orderItem = OrderItem.of(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    product.getOriginalPrice(),
                    cartItem.getQuantity(),
                    product.getImageUrl()
            );

            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private void requireOrderable(Product product) {
        if(product == null) {
            throw new NotFoundException("상품을 찾을 수 없습니다.");
        }
        if(product.getStatus() != ProductStatus.ON_SALE) {
            throw new ConflictException("판매 중인 상품만 주문할 수 있습니다.");
        }
    }

    private void validateInputs(Long memberId, HttpSession session, CreateOrderCommand cmd) {
        if(session == null) {
            throw new IllegalArgumentException("session은 필수입니다.");
        }
        if(memberId == null || memberId <= 0) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        if(cmd == null) {
            throw new IllegalArgumentException("주문 생성 정보는 필수입니다.");
        }
        if(cmd.getRequestKey() == null || cmd.getRequestKey().isBlank()) {
            throw new IllegalArgumentException("requestKey는 필수입니다.");
        }
    }

    private void validateKeyInputs(Long memberId, HttpSession session, Long orderId) {
        if(session == null) {
            throw new IllegalArgumentException("session은 필수입니다.");
        }
        if(memberId == null || memberId <= 0) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        if(orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId는 양수여야 합니다.");
        }
    }

    private void requireActiveMember(Member member) {
        if(member == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        if(member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("비활성 회원은 주문할 수 없습니다.");
        }
    }

    private void ensureNotProcessedRequest(String requestKey) {
        orderRepository.findByRequestKey(requestKey)
                .ifPresent(order -> {
                    throw new ConflictException("이미 처리된 주문 요청입니다.");
                });
    }
}
