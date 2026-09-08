package io.github.takgeun.shop.order.api;

import io.github.takgeun.shop.global.error.api.ApiGlobalExceptionHandler;
import io.github.takgeun.shop.global.error.code.ErrorCode;
import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.ForbiddenException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.order.application.OrderService;
import io.github.takgeun.shop.order.application.dto.CheckoutItemCommand;
import io.github.takgeun.shop.order.application.dto.CreateOrderCommand;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderApiControllerTest {

    private OrderService orderService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrderApiController(orderService))
                .setControllerAdvice(new ApiGlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 로그인_회원의_주문_상세를_조회한다() throws Exception {
        Long memberId = 7L;
        Long orderId = 42L;
        authenticate(memberId);

        Order order = order(memberId, orderId);
        when(orderService.getDetail(memberId, orderId)).thenReturn(order);

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("PAYMENT_COMPLETED"))
                .andExpect(jsonPath("$.items[0].productId").value(10L))
                .andExpect(jsonPath("$.items[0].productName").value("주문 당시 상품명"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(20_000))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(40_000))
                .andExpect(jsonPath("$.subtotal").value(40_000))
                .andExpect(jsonPath("$.shippingFee").value(0))
                .andExpect(jsonPath("$.totalPrice").value(40_000))
                .andExpect(jsonPath("$.recipientName").value("수령인"));

        verify(orderService).getDetail(memberId, orderId);
    }

    @Test
    void 주문ID가_양수가_아니면_400을_반환한다() throws Exception {
        authenticate(7L);

        mockMvc.perform(get("/api/v1/orders/{orderId}", 0))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void 로그인_회원의_주문_목록을_조회한다() throws Exception {

        Long memberId = 7L;
        authenticate(memberId);

        Order firstOrder = order(memberId, 42L);
        Order secondOrder = order(memberId, 41L);

        when(orderService.getMyOrders(memberId))
                .thenReturn(List.of(firstOrder, secondOrder));

        mockMvc.perform(
                        get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders.length()").value(2))
                .andExpect(jsonPath("$.orders[0].orderId").value(42L))
                .andExpect(jsonPath("$.orders[0].status")
                        .value("PAYMENT_COMPLETED"))
                .andExpect(jsonPath(
                        "$.orders[0].representativeProductName"
                ).value("주문 당시 상품명"))
                .andExpect(jsonPath("$.orders[0].itemCount").value(1))
                .andExpect(jsonPath("$.orders[0].totalPrice")
                        .value(40_000));

        verify(orderService).getMyOrders(memberId);
    }

    @Test
    void 주문이_없으면_빈_목록을_반환한다() throws Exception {

        // given
        Long memberId = 7L;
        authenticate(memberId);

        // when
        when(orderService.getMyOrders(memberId))
                .thenReturn(List.of());

        // then
        mockMvc.perform(
                        get("/api/v1/orders"))
                .andExpect(status().isOk())     // 주문 결과가 없는 건 오류가 아니니 200 반환이 맞음. (404 아님!)
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders").isEmpty());

        verify(orderService).getMyOrders(memberId);
    }

    @Test
    void 로그인_회원은_주문을_생성할_수_있다() throws Exception {

        // given
        Long memberId = 7L;
        Long orderId = 100L;

        authenticate(memberId);

        when(orderService.checkout(
                eq(memberId),
                anyList(),
                any(CreateOrderCommand.class)
        )).thenReturn(orderId);

        // when & then
        mockMvc.perform(
                        post("/api/v1/orders")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "items": [
                                            {
                                              "productId": 10,
                                              "quantity": 2
                                            },
                                            {
                                              "productId": 20,
                                              "quantity": 1
                                            }
                                          ],
                                          "recipientName": "수령인",
                                          "phoneNumber": "010-1234-5678",
                                          "zipCode": "12345",
                                          "address": "서울시 테스트구",
                                          "addressDetail": "101호",
                                          "requestMessage": "문 앞에 놓아주세요",
                                          "requestKey": "request-key-100"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/orders/100"
                ))
                .andExpect(jsonPath("$.orderId")
                        .value(orderId));

        /**
         * Controller가 HTTP 요청 DTO를 Application 계층용 Command로 정확하게 변환해서
         * OrderService.checkout()에 전달했는지 검증
         * CreateOrderRequest
         * ├─ items → List<CheckoutItemCommand>
         * └─ 배송·요청 정보 → CreateOrderCommand
         *
         * checkout() 호출 여부만 확인하는 것이 아니라 실제 전달된 두 Command를 포착해
         * 내부 값까지 검사하기 위한 코드
         */
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CheckoutItemCommand>> itemCaptor =
                ArgumentCaptor.forClass(List.class);

        ArgumentCaptor<CreateOrderCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateOrderCommand.class);

        // 테스트 실행 중 checkout 메서드가 한 번이라도 호출됐는지 확인하고, checkoutItems, command 포착
        verify(orderService).checkout(
                eq(memberId),
                itemCaptor.capture(),
                commandCaptor.capture()
        );

        assertThat(itemCaptor.getValue())
                .containsExactly(
                        new CheckoutItemCommand(10L, 2),
                        new CheckoutItemCommand(20L, 1)
                );

        CreateOrderCommand capturedCommand = commandCaptor.getValue();

        assertThat(capturedCommand.recipientName())
                .isEqualTo("수령인");
        assertThat(capturedCommand.phoneNumber())
                .isEqualTo("010-1234-5678");
        assertThat(capturedCommand.zipCode())
                .isEqualTo("12345");
        assertThat(capturedCommand.address())
                .isEqualTo("서울시 테스트구");
        assertThat(capturedCommand.addressDetail())
                .isEqualTo("101호");
        assertThat(capturedCommand.requestMessage())
                .isEqualTo("문 앞에 놓아주세요");
        assertThat(capturedCommand.requestKey())
                .isEqualTo("request-key-100");
    }

    @Test
    void 주문_상품이_없으면_400을_반환한다() throws Exception {

        // given
        Long memberId = 7L;
        authenticate(memberId);

        /**
         * 1. POST /api/v1/orders 요청
         * 2. DispatcherServlet이 OrderApiController 메서드 탐색
         * 3. JSON을 CreateOrderRequest로 역직렬화
         * 4. @Valid로 CreateOrderRequest 검증
         * 5. items의 @NotEmpty 검증 실패
         * 6. MethodArgumentNotValidException 발생
         * 7. Controller 메서드 본문은 실행되지 않음
         * 8. ApiGlobalExceptionHandler가 예외 처리
         * 9. 400 INVALID_INPUT JSON 응답 반환
         */
        // when & then
        mockMvc.perform(
                post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                    "items": [],
                                    "recipientName": "수령인",
                                    "phoneNumber": "010-1234-5678",
                                    "zipCode": "12345",
                                    "address": "서울시 테스트구",
                                    "addressDetail": "101호",
                                    "requestMessage": "문 앞에 놓아주세요",
                                    "requestKey": "request-key-100"
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray())
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("items")));

        verify(orderService, never()).checkout(
                any(),
                anyList(),
                any()
        );
    }

    @Test
    void 주문_상품의_필드가_잘못되면_400을_반환한다() throws Exception {

        // given
        Long memberId = 7L;
        authenticate(memberId);

        // when & then
        mockMvc.perform(
                        post("/api/v1/orders")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "items": [
                                        {
                                          "productId": null,
                                          "quantity": 0
                                        }
                                      ],
                                      "recipientName": "수령인",
                                      "phoneNumber": "010-1234-5678",
                                      "zipCode": "12345",
                                      "address": "서울시 테스트구",
                                      "addressDetail": "101호",
                                      "requestMessage": "문 앞에 놓아주세요",
                                      "requestKey": "request-key-100"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray())
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("items[0].productId")))
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("items[0].quantity")));

        verify(orderService, never()).checkout(
                any(),
                anyList(),
                any()
        );
    }

    @Test
    void 주문_배송정보가_누락되면_400을_반환한다() throws Exception {

        // given
        Long memberId = 7L;
        authenticate(memberId);

        // when & then
        mockMvc.perform(
                        post("/api/v1/orders")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "items": [
                                        {
                                          "productId": 10,
                                          "quantity": 2
                                        }
                                      ],
                                      "recipientName": "",
                                      "phoneNumber": "",
                                      "zipCode": "",
                                      "address": "",
                                      "requestKey": ""
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("recipientName")))
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("phoneNumber")))
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("zipCode")))
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("address")))
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(hasItem("requestKey")));

        verify(orderService, never()).checkout(
                any(),
                anyList(),
                any()
        );
    }

    @Test
    void 주문생성_JSON_문법이_잘못되면_400을_반환한다() throws Exception {

        // given
        Long memberId = 7L;
        authenticate(memberId);

        // when & then
        mockMvc.perform(
                        post("/api/v1/orders")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                    {
                                      "items": [
                                        {
                                          "productId": 10,
                                          "quantity": 2
                                        }
                                      ],
                                      "recipientName": "수령인",
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.code")
                        .value("MALFORMED_JSON"));

        verify(orderService, never()).checkout(
                any(),
                anyList(),
                any()
        );
    }

    @Test
    void 로그인_회원은_본인의_주문을_취소할_수_있다() throws Exception {

        // given
        Long memberId = 7L;
        Long orderId = 42L;

        authenticate(memberId);

        // when & then
        mockMvc.perform(
                patch("/api/v1/orders/{orderId}/cancel", orderId)
        )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(orderService).cancel(memberId, orderId);
    }

    @Test
    void 취소할_주문ID가_양수가_아니면_400을_반환한다() throws Exception {
        // given
        Long memberId = 7L;
        authenticate(memberId);

        // when & then
        mockMvc.perform(
                        patch("/api/v1/orders/{orderId}/cancel", 0)
                )
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(orderService, never()).cancel(
                any(),  // 인자에 null이 전달될 가능성까지 포괄하려면 anyLong보다 any()가 나음.
                any()
        );
    }

    @Test
    void 존재하지_않는_주문을_취소하면_404를_반환한다() throws Exception {
        // given
        Long memberId = 7L;
        Long orderId = 999L;

        authenticate(memberId);

        doThrow(new NotFoundException(ErrorCode.ORDER_NOT_FOUND))
                .when(orderService)
                .cancel(memberId, orderId);

        // when & then
        mockMvc.perform(
                        patch("/api/v1/orders/{orderId}/cancel", orderId)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        verify(orderService).cancel(memberId, orderId);
    }

    @Test
    void 다른_회원의_주문을_취소하면_403을_반환한다() throws Exception {
        // given
        Long memberId = 7L;
        Long orderId = 42L;

        authenticate(memberId);

        doThrow(new ForbiddenException(ErrorCode.ORDER_ACCESS_DENIED))
                .when(orderService)
                .cancel(memberId, orderId);

        // when & then
        mockMvc.perform(
                        patch("/api/v1/orders/{orderId}/cancel", orderId)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ORDER_ACCESS_DENIED"));

        verify(orderService).cancel(memberId, orderId);
    }

    @Test
    void 이미_취소된_주문을_다시_취소하면_409를_반환한다() throws Exception {
        // given
        Long memberId = 7L;
        Long orderId = 42L;

        authenticate(memberId);

        doThrow(new ConflictException(
                "주문완료 및 결제완료 상태에서만 취소할 수 있습니다."
        ))
                .when(orderService)
                .cancel(memberId, orderId);

        // when & then
        mockMvc.perform(
                        patch("/api/v1/orders/{orderId}/cancel", orderId)
                )
                .andExpect(status().isConflict());

        verify(orderService).cancel(memberId, orderId);
    }


    // ------------------------------------------------------------------------------------

    private Order order(Long memberId, Long orderId) {
        OrderItem item = OrderItem.of(
                10L,
                "주문 당시 상품명",
                20_000,
                25_000,
                2,
                "/images/product.png"
        );

        Order order = Order.create(
                memberId,
                "ORD-TEST-42",
                "request-key-42",
                List.of(item),
                "수령인",
                "010-1234-5678",
                "12345",
                "서울시 테스트구",
                "101호",
                "문 앞에 놓아주세요",
                0
        );
        order.assignId(orderId);
        order.markPaymentCompleted();
        return order;
    }

    private void authenticate(Long memberId) {
        ShopUserPrincipal principal = new ShopUserPrincipal(
                memberId,
                "member@test.com",
                "encoded-password",
                "테스트회원",
                MemberRole.USER,
                MemberStatus.ACTIVE
        );

        // 아래와 같이 인증을 완료시키고, 해당 사용자를 SecurityContext에 직접 설정
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );
    }
}
