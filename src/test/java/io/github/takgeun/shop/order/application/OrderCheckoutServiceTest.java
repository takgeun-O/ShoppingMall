package io.github.takgeun.shop.order.application;

import io.github.takgeun.shop.cart.application.CartService;
import io.github.takgeun.shop.cart.infra.SessionCartRepository;
import io.github.takgeun.shop.cart.view.dto.CartItemView;
import io.github.takgeun.shop.cart.view.dto.CartViewResult;
import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.UnauthorizedException;
import io.github.takgeun.shop.order.application.dto.CheckoutItemCommand;
import io.github.takgeun.shop.order.application.dto.CreateOrderCommand;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class OrderCheckoutServiceTest {

    private CartService cartService;
    private OrderService orderService;
    private SessionCartRepository sessionCartRepository;

    private OrderCheckoutService orderCheckoutService;

    @BeforeEach
    void setUp() {
        cartService = mock(CartService.class);
        orderService = mock(OrderService.class);
        sessionCartRepository =
                mock(SessionCartRepository.class);

        orderCheckoutService = new OrderCheckoutService(
                cartService,
                sessionCartRepository,
                orderService
        );
    }

    @Test
    void 세션_장바구니를_주문명령으로_변환하여_주문을_생성한다() {

        // given
        Long memberId = 1L;
        Long orderId = 100L;

        HttpSession session = mock(HttpSession.class);
        CreateOrderCommand command = createOrderCommand();

        CartItemView firstItem = mock(CartItemView.class);
        CartItemView secondItem = mock(CartItemView.class);
        CartViewResult cartView = mock(CartViewResult.class);

        when(firstItem.getProductId())
                .thenReturn(10L);
        when(firstItem.getQuantity())
                .thenReturn(2);

        when(secondItem.getProductId())
                .thenReturn(20L);
        when(secondItem.getQuantity())
                .thenReturn(3);

        when(cartView.getItems()).thenReturn(
                List.of(firstItem, secondItem)
        );

        when(cartService.getCartView(session))
                .thenReturn(cartView);

        /**
         * Mockito를 이용해 orderService.checkout() 동작을 미리 설정하는 stubbing
         * checkout()이 특정 회원 ID, 아무 List, 정확히 동일한 command 객체를 전달 받아 호출되면
         * orderId를 반환하도록 설정한다.
         */
        when(orderService.checkout(
                eq(memberId),
                anyList(),
                same(command)   // 전달된 객체가 command와 메모리상 정확히 같은 객체인지 검사 (== 비교랑 비슷)
        )).thenReturn(orderId);

        // when
        Long result = orderCheckoutService.createOrderFromCart(
                memberId,
                session,
                command
        );

        // then
        assertThat(result).isEqualTo(orderId);

        /**
         * orderService.checkout()에 실제로 전달된 주문 상품 목록을 가로채서
         * 목록 안의 내용을 자세히 검증하기 위한 Mockito 코드
         *
         * orderService.checkout()이 호출됐는지 확인하고, 두 번째 인자로 전달된
         * List<CheckoutItemCommand>를 itemCaptor에 저장한다.
         */

        // 컴파일러의 unchecked 경고를 숨긴다.
        // ArgumentCaptor : mock 메서드에 실제로 전달된 인자를 포착하는 도구
        // ArgumentCaptor<List<CheckoutItemCommand>> itemCaptor : 이 Captor가 포착할 타입은 List<CheckoutItemCommand>
        // forClass(List.class) : Mockito에 포착할 클래스가 List라고 알려준다.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CheckoutItemCommand>> itemCaptor =
                ArgumentCaptor.forClass(List.class);

        /**
         * OrderCheckoutService가 만든 List
         * → orderService.checkout()에 전달
         * → ArgumentCaptor가 포착
         * → 테스트에서 꺼내 검증
         */
        verify(orderService).checkout(
                eq(memberId),
                itemCaptor.capture(),   // 아무 List나 허용한다에서 끝나는 게 아니라 실제 값을 나중에 꺼낼 수 있게 한다.
                same(command)       // command와 정확히 같은 객체인지 검증한다. 필드값만 같은 다른 Command 객체는 통과 X
        );

        List<CheckoutItemCommand> capturedItems =
                itemCaptor.getValue();

        assertThat(capturedItems).containsExactly(
                new CheckoutItemCommand(10L, 2),
                new CheckoutItemCommand(20L, 3)
        );

        verify(sessionCartRepository).clear(session);
    }

    @Test
    void 주문에_성공하면_세션_장바구니를_비운다() {

        // given
        Long memberId = 1L;
        Long orderId = 100L;

        HttpSession session = mock(HttpSession.class);
        CreateOrderCommand command = createOrderCommand();

        CartItemView cartItem = mock(CartItemView.class);
        CartViewResult cartView = mock(CartViewResult.class);

        when(cartItem.getProductId()).thenReturn(10L);
        when(cartItem.getQuantity()).thenReturn(2);
        when(cartView.getItems())
                .thenReturn(List.of(cartItem));

        when(cartService.getCartView(session))
                .thenReturn(cartView);

        when(orderService.checkout(
                eq(memberId),
                anyList(),
                same(command)
        )).thenReturn(orderId);

        // when
        Long result = orderCheckoutService.createOrderFromCart(
                memberId,
                session,
                command
        );

        // then
        assertThat(result).isEqualTo(orderId);

        verify(sessionCartRepository).clear(session);
    }

    @Test
    void 주문에_실패하면_세션_장바구니를_비우지_않는다() {

        // given
        Long memberId = 1L;

        HttpSession session = mock(HttpSession.class);  // 가짜 세션 생성
        CreateOrderCommand command = createOrderCommand();  // 주문 정보 생성

        CartItemView cartItem = mock(CartItemView.class);
        CartViewResult cartView = mock(CartViewResult.class);

        when(cartItem.getProductId()).thenReturn(10L);  // 가짜 장바구니 항목에서 상품 ID를 조회하면 10L을 반환하도록 설정한다.
        when(cartItem.getQuantity()).thenReturn(2);
        when(cartView.getItems())
                .thenReturn(List.of(cartItem));

        when(cartService.getCartView(session))
                .thenReturn(cartView);

        when(orderService.checkout(
                eq(memberId),
                anyList(),
                same(command)
        )).thenThrow(
                new ConflictException("재고가 부족합니다.")
        );

        // when & then
        assertThatThrownBy(
                /**
                 * createOrderFromCart 내부 실행 과정
                 * 1. 장바구니 조회 결과 가져오기
                 * List<CheckoutItemCommand> items =
                 *         cartView.getItems().stream()
                 *                 .map(item ->
                 *                         new CheckoutItemCommand(
                 *                                 item.getProductId(),
                 *                                 item.getQuantity()
                 *                         )
                 *                 )
                 *                 .toList();
                 *
                 *  2. 장바구니 항목을 주문용 Command로 변환
                 *  Long orderId =
                 *         orderService.checkout(
                 *                 memberId,
                 *                 items,
                 *                 command
                 *         );
                 *
                 *  3. orderService는 mock이고, 앞에서 thenThrow()로 설정했으니 이 지점에서 예외가 발생한다.
                 *  -> ConflictException: 재고가 부족합니다.
                 */
                () -> orderCheckoutService.createOrderFromCart( // 테스트 대상 실행
                        memberId,
                        session,
                        command
                )
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage("재고가 부족합니다.");

        verify(sessionCartRepository, never())
                .clear(any(HttpSession.class));
    }

    @Test
    void 장바구니가_비어있으면_주문을_생성하지_않는다() {

        // given
        Long memberId = 1L;

        HttpSession session = mock(HttpSession.class);
        CreateOrderCommand command = createOrderCommand();
        CartViewResult emptyCart = mock(CartViewResult.class);

        when(emptyCart.getItems()).thenReturn(List.of());

        when(cartService.getCartView(session))
                .thenReturn(emptyCart);

        // when & then
        assertThatThrownBy(
                () -> orderCheckoutService.createOrderFromCart(
                        memberId,
                        session,
                        command
                )
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage("장바구니가 비어있습니다.");

        verifyNoInteractions(orderService);
        verifyNoInteractions(sessionCartRepository);
    }

    @Test
    void 세션이_null이면_주문을_생성할_수_없다() {

        // given
        Long memberId = 1L;
        CreateOrderCommand command = createOrderCommand();

        // when & then
        assertThatThrownBy(
                () -> orderCheckoutService.createOrderFromCart(
                        memberId,
                        null,
                        command
                )
        )
                // 현재 테스트는 Service 단위 테스트임
                // Controller도 거치지 않고, HTTP 요청도 없으며, Spring Security 필터도 거치지 않음
                // 세션 객체의 부재는 인증 실패가 아니라 메서드 호출 계약 위반으로 보고 IllegalArgumentException으로 유지하는 것이 좋음.
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("session은 필수입니다.");

        verifyNoInteractions(cartService);
        verifyNoInteractions(orderService);
        verifyNoInteractions(sessionCartRepository);
    }

    private CreateOrderCommand createOrderCommand() {
        return new CreateOrderCommand(
                "수령인",
                "010-1234-5678",
                "01234",
                "서울특별시 중구",
                "101호",
                "문 앞에 놓아주세요.",
                "member-1-request-1"
        );
    }
}