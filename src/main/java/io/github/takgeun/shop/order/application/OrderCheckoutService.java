package io.github.takgeun.shop.order.application;

import io.github.takgeun.shop.cart.application.CartService;
import io.github.takgeun.shop.cart.infra.SessionCartRepository;
import io.github.takgeun.shop.cart.view.dto.CartViewResult;
import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.UnauthorizedException;
import io.github.takgeun.shop.order.application.dto.CheckoutItemCommand;
import io.github.takgeun.shop.order.application.dto.CreateOrderCommand;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.view.dto.OrderCompleteView;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCheckoutService {

    private final CartService cartService;
    private final SessionCartRepository sessionCartRepository;
    private final OrderService orderService;

    /**
     * 트랜잭션 구조는 아래와 같이 구성되어야 한다.
     * OrderCheckoutService.createOrderFromCart()
     *     │ 트랜잭션 없음
     *     │
     *     ├─ 장바구니 조회
     *     │
     *     ├─ OrderService.checkout()
     *     │    └─ @Transactional
     *     │       회원 검증
     *     │       재고 차감
     *     │       주문 저장
     *     │       커밋 (여기서 커밋되어야 해! 중요!)
     *     │
     *     └─ 세션 장바구니 제거
     *
     *
     * 만약 createOrderFromCart에서 @Transaction 시작하면
     * 내부에 있는 OrderService.checkout()이 이 트랜잭션에 참여하게 되고
     * 그러면 주문 저장이 실제로 커밋되기 전에 장바구니가 비워질 수 있음.
     *
     * 1. 주문 INSERT 실행
     * 2. 재고 UPDATE 실행
     * 3. checkout() 반환 (외부 트랜잭션 참여니까 여기서 커밋이 안됨...)
     * 4. 세션 장바구니 삭제
     * 5. 외부 트랜잭션 커밋 시도
     * 6. DB 커밋 실패
     * 7. 주문과 재고는 롤백
     * 8. 세션 장바구니는 이미 삭제됨
     */
    public Long createOrderFromCart(
            Long memberId,
            HttpSession session,
            CreateOrderCommand command
    ) {
        validateSession(session);

        CartViewResult cartView = cartService.getCartView(session);

        if(cartView.getItems().isEmpty()) {
            throw new ConflictException("장바구니가 비어있습니다.");
        }

        // 실제 상품 기준으로 검증 + 재고 차감 + 주문 아이템 생성
        List<CheckoutItemCommand> checkoutItems =
                cartView.getItems().stream()
                        .map(item -> new CheckoutItemCommand(
                                item.getProductId(),
                                item.getQuantity()
                        ))
                        .toList();

        /**
         * OrderService.checkout()의 트랜잭션이 정상적으로 완료된 후에
         * 이 코드로 돌아온다.
         */
        Long orderId = orderService.checkout(
                memberId,
                checkoutItems,
                command
        );

        /**
         * 주문 저장이 완료된 경우에만 장바구니를 비운다.
         */
        sessionCartRepository.clear(session);

        return orderId;
    }

    /**
     * 주문 완료 페이지 찾아가기
     * 주문 생성 당시에 저장해놓은 세션을 통해서 찾아내거나
     */
    public OrderCompleteView getOrderCompleteView(
            Long memberId,
            Long orderId
    ) {

        Order order =
                orderService.getDetail(memberId, orderId);

        return OrderCompleteView.from(order);
    }

    private void validateSession(HttpSession session) {
        if(session == null) {
            throw new IllegalArgumentException("session은 필수입니다.");
        }
    }
}
