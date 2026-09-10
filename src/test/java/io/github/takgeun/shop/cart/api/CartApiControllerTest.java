package io.github.takgeun.shop.cart.api;

import io.github.takgeun.shop.cart.application.CartService;
import io.github.takgeun.shop.cart.application.dto.CartItemResult;
import io.github.takgeun.shop.cart.application.dto.CartResult;
import io.github.takgeun.shop.cart.application.dto.CartSummaryResult;
import io.github.takgeun.shop.global.error.api.ApiGlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 검증 내역
 * - Controller와 Service 연결
 * - 세션 전달
 * - 요청 JSON 검증
 * - Application DTO → API 응답 DTO 변환
 * - HTTP 상태 코드
 */

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CartApiControllerTest {

    private CartService cartService;
    private MockMvc mockMvc;    // MockMvc : 실제 Tomcat 서버 실행하지 않고도 HTTP 요청을 흉내낼 수 있게 한다.

    @BeforeEach
    void setUp() {
        cartService = mock(CartService.class);

        // @Valid와 요청 DTO의 검증 애노테이션을 실제로 작동시키기 위해 등록한 Spring용 Validator
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                // 전체 Spring ApplicationContext 실행하지 않고 Controller만 직접 new로 만든다.
                .standaloneSetup(
                        new CartApiController(cartService)
                )
                // API 공통 예외 처리기를 테스트용 Spring MVC 환경에 등록
                // 전체 ApplicationContext를 사용하지 않으므로 @RestControllerAdvice가 붙어있다고 해서 자동으로 발견되는 게 아니기 떄문에
                // 테스트에서 직접 객체를 만들어 등록
                .setControllerAdvice(
                        new ApiGlobalExceptionHandler()
                )
                .setValidator(validator)
                .build();
    }

    @Test
    void 장바구니를_조회한다() throws Exception {
        // given
        MockHttpSession session =
                new MockHttpSession();

        CartItemResult item = new CartItemResult(
                1L,
                "무선 키보드",
                10_000,
                12_000,
                2,
                "/images/keyboard.jpg",
                20_000
        );

        CartSummaryResult summary =
                new CartSummaryResult(
                        24_000,
                        4_000,
                        20000,
                        3_000,
                        23_000
                );

        CartResult result =
                new CartResult(
                        List.of(item),
                        summary
                );

        when(cartService.getCart(session))
                .thenReturn(result);

        // when & then
        mockMvc.perform(
                        get("/api/v1/cart")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))

                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()")
                        .value(1))

                .andExpect(jsonPath("$.items[0].productId")
                        .value(1L))
                .andExpect(jsonPath("$.items[0].quantity")
                        .value(2))

                .andExpect(jsonPath("$.summary.subtotal")
                        .value(20_000))
                .andExpect(jsonPath("$.summary.discountTotal")
                        .value(4_000))
                .andExpect(jsonPath("$.summary.shippingFee")
                        .value(3_000))
                .andExpect(jsonPath("$.summary.totalPrice")
                        .value(23_000));

        verify(cartService).getCart(session);
    }

    @Test
    void 빈_장바구니를_조회한다() throws Exception {
        // given
        MockHttpSession session =
                new MockHttpSession();

        when(cartService.getCart(session))
                .thenReturn(CartResult.empty());

        // when & then
        mockMvc.perform(
                        get("/api/v1/cart")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.summary.subtotal")
                        .value(0))
                .andExpect(jsonPath("$.summary.discountTotal")
                        .value(0))
                .andExpect(jsonPath("$.summary.shippingFee")
                        .value(0))
                .andExpect(jsonPath("$.summary.totalPrice")
                        .value(0));

        verify(cartService).getCart(session);
    }

    @Test
    void 장바구니에_상품을_추가한다() throws Exception {
        // given
        MockHttpSession session =
                new MockHttpSession();

        // when & then
        mockMvc.perform(
                        post("/api/v1/cart/items")
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "productId": 10,
                                          "quantity": 2
                                        }
                                        """)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(cartService).add(
                session,
                10L,
                2
        );
    }

    @Test
    void 추가할_상품의_요청값이_잘못되면_400을_반환한다()
            throws Exception {

        // given
        MockHttpSession session =
                new MockHttpSession();

        // when & then
        mockMvc.perform(
                        post("/api/v1/cart/items")
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "productId": null,
                                          "quantity": 0
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray())
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("productId")))
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("quantity")));

        verify(cartService, never()).add(
                any(),
                any(),
                anyInt()
        );
    }

    @Test
    void 장바구니_상품의_수량을_변경한다()
            throws Exception {

        // given
        MockHttpSession session =
                new MockHttpSession();

        Long productId = 10L;

        // when & then
        mockMvc.perform(
                        patch(
                                "/api/v1/cart/items/{productId}",
                                productId
                        )
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "quantity": 5
                                        }
                                        """)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(cartService).updateQuantity(
                session,
                productId,
                5
        );
    }

    @Test
    void 수량을_0으로_변경하면_장바구니_상품을_삭제한다()
            throws Exception {

        // given
        MockHttpSession session =
                new MockHttpSession();

        Long productId = 10L;

        // when & then
        mockMvc.perform(
                        patch(
                                "/api/v1/cart/items/{productId}",
                                productId
                        )
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "quantity": 0
                                    }
                                    """)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(cartService).updateQuantity(
                session,
                productId,
                0
        );
    }

    @Test
    void 변경할_수량이_음수이면_400을_반환한다()
            throws Exception {

        // given
        MockHttpSession session =
                new MockHttpSession();

        /**
         * quantity = -1
         * → @Valid가 요청 DTO 검증
         * → @PositiveOrZero 실패
         * → MethodArgumentNotValidException 발생
         * → ApiGlobalExceptionHandler
         * → 400 INVALID_INPUT
         */
        // when & then
        mockMvc.perform(
                        patch(
                                "/api/v1/cart/items/{productId}",
                                10L
                        )
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "quantity": -1
                                    }
                                    """)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray())
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("quantity")));

        verify(cartService, never())
                .updateQuantity(
                        any(),
                        any(),
                        anyInt()
                );
    }

    @Test
    void 변경할_상품ID가_양수가_아니면_400을_반환한다()
            throws Exception {

        // given
        MockHttpSession session =
                new MockHttpSession();

        // when & then
        mockMvc.perform(
                        patch(
                                "/api/v1/cart/items/{productId}",
                                0L
                        )
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "quantity": 2
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verify(cartService, never())
                .updateQuantity(
                        any(),
                        any(),
                        anyInt()
                );
    }

    @Test
    void 장바구니에서_상품을_삭제한다()
            throws Exception {

        // given
        MockHttpSession session =
                new MockHttpSession();

        Long productId = 10L;

        // when & then
        mockMvc.perform(
                        delete(
                                "/api/v1/cart/items/{productId}",
                                productId
                        )
                                .session(session)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(cartService).remove(
                session,
                productId
        );
    }

    @Test
    void 삭제할_상품ID가_양수가_아니면_400을_반환한다()
            throws Exception {

        // given
        MockHttpSession session =
                new MockHttpSession();

        // when & then
        mockMvc.perform(
                        delete(
                                "/api/v1/cart/items/{productId}",
                                0L
                        )
                                .session(session)
                )
                .andExpect(status().isBadRequest());

        verify(cartService, never()).remove(
                any(),
                any()
        );
    }

    @Test
    void 장바구니를_전체_비운다()
            throws Exception {

        // given
        MockHttpSession session =
                new MockHttpSession();

        // when & then
        mockMvc.perform(
                        delete("/api/v1/cart")
                                .session(session)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(cartService).clear(session);
    }

    @Test
    void 상품추가_JSON_문법이_잘못되면_400을_반환한다()
            throws Exception {

        // given
        MockHttpSession session =
                new MockHttpSession();

        /**
         * MockMvc 요청
         * → Spring MVC의 DispatcherServlet
         * → CartApiController 메서드 탐색
         * → @RequestBody 처리
         * → HTTP Message Converter가 JSON 해석
         * → JSON 문법 오류 발생
         * → HttpMessageNotReadableException
         * → ApiGlobalExceptionHandler
         * → MALFORMED_JSON, 400 응답
         */
        // when & then
        mockMvc.perform(
                        post("/api/v1/cart/items")
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "productId": 10,
                                          "quantity":
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("MALFORMED_JSON"));

        verify(cartService, never()).add(
                any(),
                any(),
                anyInt()
        );
    }
}