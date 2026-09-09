package io.github.takgeun.shop.cart.application;

import io.github.takgeun.shop.cart.application.dto.CartItemResult;
import io.github.takgeun.shop.cart.application.dto.CartResult;
import io.github.takgeun.shop.cart.domain.CartRepository;
import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.order.application.dto.CheckoutItemCommand;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class CartServiceTest {

    private CartRepository cartRepository;
    private ProductService productService;
    private CartService cartService;

    private HttpSession session;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        productService = mock(ProductService.class);

        cartService = new CartService(cartRepository, productService);

        session = new MockHttpSession();
    }

    @Test
    void 빈_장바구니를_조회한다() {

        // given
        when(cartRepository.findAll(session))
                .thenReturn(Map.of());

        // when
        CartResult result = cartService.getCart(session);

        // then
        assertThat(result.items()).isEmpty();
        assertThat(result.summary().subtotal()).isZero();
        assertThat(result.summary().discountTotal()).isZero();
        assertThat(result.summary().shippingFee()).isZero();
        assertThat(result.summary().totalPrice()).isZero();

        verify(cartRepository).findAll(session);
        verifyNoInteractions(productService);
    }

    @Test
    void 장바구니_상품과_요약금액을_조회한다() {

        // given
        Long firstProductId = 2L;
        Long secondProductId = 1L;

        Map<Long, Integer> cart = new LinkedHashMap<>();
        cart.put(firstProductId, 2);
        cart.put(secondProductId, 1);

        when(cartRepository.findAll(session))
                .thenReturn(cart);

        Product firstProduct = product(
                firstProductId,
                "키보드",
                10_000,
                12_000,
                10
        );

        Product secondProduct = product(
                secondProductId,
                "마우스",
                15_000,
                null,
                10
        );

        when(productService.getForOrder(firstProductId))
                .thenReturn(firstProduct);

        when(productService.getForOrder(secondProductId))
                .thenReturn(secondProduct);

        // when
        CartResult result = cartService.getCart(session);

        // then
        assertThat(result.items()).hasSize(2);

        // productId 오름차순 정렬
        assertThat(result.items())
                .extracting(CartItemResult::productId)
                .containsExactly(
                        secondProductId,
                        firstProductId
                );

        CartItemResult mouseItem = result.items().get(0);

        assertThat(mouseItem.productName())
                .isEqualTo("마우스");
        assertThat(mouseItem.unitPrice())
                .isEqualTo(15_000);
        assertThat(mouseItem.quantity())
                .isEqualTo(1);
        assertThat(mouseItem.lineTotal())
                .isEqualTo(15_000);

        CartItemResult keyboardItem = result.items().get(1);

        assertThat(keyboardItem.productName())
                .isEqualTo("키보드");
        assertThat(keyboardItem.unitPrice())
                .isEqualTo(10_000);
        assertThat(keyboardItem.originalPrice())
                .isEqualTo(12_000);
        assertThat(keyboardItem.quantity())
                .isEqualTo(2);
        assertThat(keyboardItem.lineTotal())
                .isEqualTo(20_000);

        // 판매가 합계: 20,000 + 15,000
        assertThat(result.summary().subtotal())
                .isEqualTo(35_000);

        // 키보드 할인: (12,000 - 10,000) × 2
        assertThat(result.summary().discountTotal())
                .isEqualTo(4_000);

        // 판매가 합계가 30,000원 이상이므로 무료배송
        assertThat(result.summary().shippingFee())
                .isZero();

        assertThat(result.summary().totalPrice())
                .isEqualTo(35_000);
    }

    @Test
    void 상품금액이_무료배송_기준보다_작으면_배송비를_추가한다() {
        // given
        Long productId = 1L;

        Product product = product(
                productId,
                "상품",
                10_000,
                12_000,
                10
        );

        when(cartRepository.findAll(session))
                .thenReturn(Map.of(productId, 2));

        when(productService.getForOrder(productId))
                .thenReturn(product);

        // when
        CartResult result =
                cartService.getCart(session);

        // then

        assertThat(result.summary().subtotal())
                .isEqualTo(20_000);

        assertThat(result.summary().discountTotal())
                .isEqualTo(4_000);

        assertThat(result.summary().shippingFee())
                .isEqualTo(3_000);

        assertThat(result.summary().totalPrice())
                .isEqualTo(23_000);
    }

    @Test
    void 장바구니에_새로운_상품을_추가한다() {
        // given
        Long productId = 1L;
        int quantity = 3;

        Product product = product(
                productId,
                "키보드",
                10_000,
                null,
                10
        );

        when(productService.getForOrder(productId))
                .thenReturn(product);

        when(cartRepository.findAll(session))
                .thenReturn(Map.of());

        // when
        cartService.add(
                session,
                productId,
                quantity
        );

        // then
        verify(cartRepository).put(
                session,
                productId,
                quantity
        );
    }

    @Test
    void 이미_담긴_상품은_수량을_누적한다() {
        // given
        Long productId = 1L;

        Product product = product(
                productId,
                "키보드",
                10_000,
                null,
                10
        );

        when(productService.getForOrder(productId))
                .thenReturn(product);

        when(cartRepository.findAll(session))
                .thenReturn(Map.of(productId, 2));

        // when
        cartService.add(
                session,
                productId,
                3
        );

        // then
        verify(cartRepository).put(
                session,
                productId,
                5
        );
    }

    @Test
    void 수량이_1보다_작으면_상품을_추가할_수_없다() {
        // when & then
        assertThatThrownBy(
                () -> cartService.add(
                        session,
                        1L,
                        0
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수량은 1 이상이어야 합니다.");

        verifyNoInteractions(
                cartRepository,
                productService
        );
    }

    @Test
    void 상품ID가_양수가_아니면_상품을_추가할_수_없다() {
        assertThatThrownBy(
                () -> cartService.add(
                        session,
                        0L,
                        1
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("productId는 양수여야 합니다.");

        verifyNoInteractions(
                cartRepository,
                productService
        );
    }

    @Test
    void 누적_수량이_상품_재고보다_많으면_추가할_수_없다() {
        // given
        Long productId = 1L;

        Product product = product(
                productId,
                "키보드",
                10_000,
                null,
                10
        );
        when(productService.getForOrder(productId))
                .thenReturn(product);

        // 기존 수량 7개
        when(cartRepository.findAll(session))
                .thenReturn(Map.of(productId, 7));

        // when & then: 4개 추가 시 최종 11개
        assertThatThrownBy(
                () -> cartService.add(
                        session,
                        productId,
                        4
                )
        )
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("현재 재고: 10");

        // put까지 안 감
        verify(cartRepository, never()).put(
                any(),
                any(),
                anyInt()
        );
    }

    @Test
    void 장바구니_상품_수량을_증가시킨다() {
        // given
        Long productId = 1L;

        when(cartRepository.findAll(session))
                .thenReturn(Map.of(productId, 2));

        Product product = product(
                productId,
                "키보드",
                10_000,
                null,
                10
        );
        when(productService.getForOrder(productId))
                .thenReturn(product);

        // when
        cartService.changeQuantity(
                session,
                productId,
                1
        );

        // then
        verify(cartRepository).put(
                session,
                productId,
                3
        );

        verify(cartRepository, never())
                .remove(any(), any());
    }

    @Test
    void 장바구니_상품_수량을_감소시킨다() {
        // given
        Long productId = 1L;

        when(cartRepository.findAll(session))
                .thenReturn(Map.of(productId, 3));

        Product product = product(
                productId,
                "키보드",
                10_000,
                null,
                10
        );
        when(productService.getForOrder(productId))
                .thenReturn(product);

        // when
        cartService.changeQuantity(
                session,
                productId,
                -1
        );

        // then
        verify(cartRepository).put(
                session,
                productId,
                2
        );
    }

    @Test
    void 수량이_0이_되면_장바구니에서_상품을_삭제한다() {
        // given
        Long productId = 1L;

        when(cartRepository.findAll(session))
                .thenReturn(Map.of(productId, 1));

        // when
        cartService.changeQuantity(
                session,
                productId,
                -1
        );

        // then
        verify(cartRepository).remove(
                session,
                productId
        );

        verify(cartRepository, never()).put(
                any(),
                any(),
                anyInt()
        );

        verifyNoInteractions(productService);
    }

    @Test
    void delta가_0이면_장바구니를_변경하지_않는다() {
        // when
        cartService.changeQuantity(
                session,
                1L,
                0
        );

        // then
        verifyNoInteractions(
                cartRepository,
                productService
        );
    }

    @Test
    void 변경한_수량이_재고보다_많으면_수량을_변경할_수_없다() {
        // given
        Long productId = 1L;

        when(cartRepository.findAll(session))
                .thenReturn(Map.of(productId, 9));

        Product product = product(
                productId,
                "키보드",
                10_000,
                null,
                10
        );
        when(productService.getForOrder(productId))
                .thenReturn(product);

        // when & then: 9 + 2 = 11
        assertThatThrownBy(
                () -> cartService.changeQuantity(
                        session,
                        productId,
                        2
                )
        )
                .isInstanceOf(ConflictException.class);

        verify(cartRepository, never()).put(
                any(),
                any(),
                anyInt()
        );
    }

    @Test
    void 장바구니_상품을_요청한_수량으로_변경한다() {
        // given
        Long productId = 1L;

        when(cartRepository.findAll(session))
                .thenReturn(Map.of(productId, 2));

        Product product = product(
                productId,
                "키보드",
                10_000,
                null,
                10
        );
        when(productService.getForOrder(productId))
                .thenReturn(product);

        // when
        cartService.updateQuantity(
                session,
                productId,
                5
        );

        // then
        verify(cartRepository).put(
                session,
                productId,
                5
        );
    }

    @Test
    void 수량을_0으로_변경하면_장바구니에서_상품을_삭제한다() {
        // 현재 CartService 구현 기준 테스트
        Long productId = 1L;

        when(cartRepository.findAll(session))
                .thenReturn(Map.of(productId, 2));

        // when
        cartService.updateQuantity(
                session,
                productId,
                0
        );

        // then
        verify(cartRepository).remove(
                session,
                productId
        );

        verifyNoInteractions(productService);
    }

    @Test
    void 장바구니에서_상품을_삭제한다() {
        // given
        Long productId = 1L;

        when(cartRepository.findAll(session))
                .thenReturn(Map.of(productId, 2));

        // when
        cartService.remove(
                session,
                productId
        );

        // then
        verify(cartRepository).remove(
                session,
                productId
        );
    }

    @Test
    void 빈_장바구니에서_상품을_삭제해도_오류가_발생하지_않는다() {
        // given
        when(cartRepository.findAll(session))
                .thenReturn(Map.of());

        // when
        cartService.remove(session, 1L);

        // then
        verify(cartRepository, never()).remove(
                any(),
                any()
        );
    }

    @Test
    void 장바구니를_전체_비운다() {
        // when
        cartService.clear(session);

        // then
        verify(cartRepository).clear(session);
    }

    @Test
    void 장바구니_항목을_주문_Command로_변환한다() {
        // given
        Map<Long, Integer> cart = new LinkedHashMap<>();
        cart.put(1L, 2);
        cart.put(2L, 3);

        when(cartRepository.findAll(session))
                .thenReturn(cart);

        // when
        List<CheckoutItemCommand> result =
                cartService.getCheckoutItems(session);

        // then
        assertThat(result)
                .containsExactly(
                        new CheckoutItemCommand(1L, 2),
                        new CheckoutItemCommand(2L, 3)
                );
    }

    @Test
    void 빈_장바구니는_빈_주문_Command_목록을_반환한다() {
        // given
        when(cartRepository.findAll(session))
                .thenReturn(Map.of());

        // when
        List<CheckoutItemCommand> result =
                cartService.getCheckoutItems(session);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void 잘못된_수량이_저장되어_있으면_주문_Command로_변환할_수_없다() {
        // given
        when(cartRepository.findAll(session))
                .thenReturn(Map.of(1L, 0));

        // when & then
        assertThatThrownBy(
                () -> cartService.getCheckoutItems(session)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수량은 1 이상이어야 합니다.");
    }

    @Test
    void 세션이_null이면_장바구니를_조회할_수_없다() {
        assertThatThrownBy(
                () -> cartService.getCart(null)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("session은 필수입니다.");

        verifyNoInteractions(
                cartRepository,
                productService
        );
    }


    private Product product(
            Long productId,
            String name,
            int price,
            Integer originalPrice,
            int stock
    ) {
        Product product = mock(Product.class);

        when(product.getId())
                .thenReturn(productId);
        when(product.getName())
                .thenReturn(name);
        when(product.getPrice())
                .thenReturn(price);
        when(product.getOriginalPrice())
                .thenReturn(originalPrice);
        when(product.getStock())
                .thenReturn(stock);
        when(product.getImageUrl())
                .thenReturn("/images/" + productId + ".jpg");

        return product;
    }
}
