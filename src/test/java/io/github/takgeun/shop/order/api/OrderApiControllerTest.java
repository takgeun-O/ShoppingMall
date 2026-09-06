package io.github.takgeun.shop.order.api;

import io.github.takgeun.shop.global.error.api.ApiGlobalExceptionHandler;
import io.github.takgeun.shop.global.error.exception.UnauthorizedException;
import io.github.takgeun.shop.global.security.ShopUserPrincipal;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.order.application.OrderService;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
