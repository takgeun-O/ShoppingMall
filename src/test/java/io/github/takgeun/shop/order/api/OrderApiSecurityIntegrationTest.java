package io.github.takgeun.shop.order.api;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.order.application.OrderService;
import io.github.takgeun.shop.order.application.dto.CheckoutItemCommand;
import io.github.takgeun.shop.order.application.dto.CreateOrderCommand;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static io.github.takgeun.shop.order.domain.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 이 테스트는 일반적으로 다음 흐름을 실제로 실행
 * <p>
 * MockMvc
 * → SecurityFilterChain
 * → 로그인 세션 확인
 * → OrderApiController
 * → OrderService
 * → OrderRepository
 * → MyBatis Mapper/XML
 * → shoppingmall_order_test
 * → JSON 응답
 * <p>
 * 이로써 다음 문제를 확인할 수 있음.
 * - Security URL 설정 오류
 * - 로그인 Principal 전달 오류
 * - 주문 소유권 검사 오류
 * - MyBatis XML 쿼리 오류
 * - Order와 OrderItem aggregate 매핑 오류
 * - 응답 DTO 직렬화 오류
 * - API 예외 응답 코드 오류
 */
// 주문 중 중간 실패 시 전체 재고가 원복되는 테스트는 나중에 별도의 비트랜잭션 통합 테스트 클래스에서 진행해야함.
// 테스트가 시작한 외부 트랜잭션에 OrderService.checkout()이 참여하기 때문에 정확한 롤백 테스트 확인 불가
@Transactional
public class OrderApiSecurityIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = "password123!";

    @Autowired
    private MemberService memberService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Test
    void 비로그인_사용자가_주문_상세_API를_요청하면_401을_반환한다() throws Exception {

        mockMvc.perform(
                        get("/api/v1/orders/{orderId}", 1L)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code")
                        .value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message")
                        .value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/orders/1"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void 본인의_주문_상세를_조회하면_주문항목을_포함하여_200을_반환한다() throws Exception {

        // given
        String email = uniqueEmail("order-owner");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "주문회원",
                "010-1111-2222"
        );

        Long categoryId = categoryService.create(
                "전자제품",
                null
        );

        Long productId = createProduct(
                categoryId,
                "무선 키보드",
                10_000,
                10
        );

        Long orderId = createOrder(
                memberId,
                productId,
                2
        );

        // 상품 주문 생성 후 해당 상품 정보 변경해도 주문 정보에 있는 상품 정보는 변경 전으로 유지되는지 확인
        Product product = productService.getPublicDetail(productId);
        product.changeName("변경된 상품명");
        product.changePrice(20_000);
        productService.save(product);

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        // when & then
        mockMvc.perform(
                        get("/api/v1/orders/{orderId}", orderId)
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))

                // $ : 응답 JSON 전체
                .andExpect(jsonPath("$.orderId")
                        .value(orderId))
                .andExpect(jsonPath("$.status")
                        .value("PAYMENT_COMPLETED"))

                // DB에 저장된 OrderItem이 주문 상세 조회 결과의 items에 포함되는지 검증
                .andExpect(jsonPath("$.items").isArray())   // items가 JSON 배열로 반환되는지 확인
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId")
                        .value(productId))
                .andExpect(jsonPath("$.items[0].productName")
                        .value("무선 키보드"))
                .andExpect(jsonPath("$.items[0].unitPrice")
                        .value(10_000))
                .andExpect(jsonPath("$.items[0].quantity")
                        .value(2))
                .andExpect(jsonPath("$.items[0].lineTotal")
                        .value(20_000))

                .andExpect(jsonPath("$.subtotal")
                        .value(20_000))
                .andExpect(jsonPath("$.shippingFee")
                        .value(3_000))
                .andExpect(jsonPath("$.totalPrice")
                        .value(23_000))

                .andExpect(jsonPath("$.recipientName")
                        .value("주문회원"))
                .andExpect(jsonPath("$.recipientPhone")
                        .value("010-1111-2222"))
                .andExpect(jsonPath("$.shippingZipCode")
                        .value("12345"))
                .andExpect(jsonPath("$.shippingAddress")
                        .value("서울시 테스트구"))
                .andExpect(jsonPath("$.shippingAddressDetail")
                        .value("101호"))

                // 내부 데이터가 노출되지 않는지 검증
                .andExpect(jsonPath("$.memberId")
                        .doesNotExist())
                .andExpect(jsonPath("$.requestKey")
                        .doesNotExist())
                .andExpect(jsonPath("$.items[0].stock")
                        .doesNotExist())
                .andExpect(jsonPath("$.items[0].status")
                        .doesNotExist());

    }

    @Test
    void 다른_회원의_주문을_조회하면_403을_반환한다() throws Exception {

        // given : 주문 소유자
        Long ownerId = memberService.signup(
                uniqueEmail("owner"),
                PASSWORD,
                "주문소유자",
                "010-2222-3333"
        );

        Long categoryId = categoryService.create(
                "생활용품",
                null
        );

        Long productId = createProduct(
                categoryId,
                "테스트 상품",
                15_000,
                10
        );

        Long orderId = createOrder(
                ownerId,
                productId,
                1
        );

        // given: 주문에 접근하는 다른 회원
        String otherEmail = uniqueEmail("other-member");

        memberService.signup(
                otherEmail,
                PASSWORD,
                "다른회원",
                "010-1111-2222"
        );

        MockHttpSession otherSession = loginAndGetSession(otherEmail, PASSWORD);

        // when & then
        mockMvc.perform(
                        get("/api/v1/orders/{orderId}", orderId)
                                .session(otherSession)
                )
                .andExpect(status().isForbidden())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code")
                        .value("ORDER_ACCESS_DENIED"))
                .andExpect(jsonPath("$.message")
                        .value("본인 주문만 처리할 수 있습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/orders/" + orderId))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void 존재하지_않는_주문을_조회하면_404를_반환한다() throws Exception {

        // given
        String email = uniqueEmail("missing-order");

        memberService.signup(
                email,
                PASSWORD,
                "조회회원",
                "010-1111-2222"
        );

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        long missingOrderId = 999_999_999L;

        // when & then
        mockMvc.perform(
                        get("/api/v1/orders/{orderId}", missingOrderId)
                                .session(session)
                )
                .andExpect(status().isNotFound())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code")
                        .value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("주문이 존재하지 않습니다."))
                .andExpect(jsonPath("$.path")
                        .value(
                                "/api/v1/orders/" + missingOrderId
                        ))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void 비로그인_사용자가_주문_목록_API를_요청하면_401을_반환한다() throws Exception {

        mockMvc.perform(
                        get("/api/v1/orders")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code")
                        .value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/orders"));
    }

    @Test
    void 로그인_회원의_주문_목록을_최신순으로_조회한다() throws Exception {

        // given
        String email = uniqueEmail("order-list-member");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "목록조회회원",
                "010-1111-2222"
        );

        Long categoryId = categoryService.create(
                "주문목록 카테고리",
                null
        );

        Long productId = createProduct(
                categoryId,
                "주문 당시 상품명",
                10_000,
                20
        );

        Long firstOrderId = createOrder(
                memberId,
                productId,
                1
        );

        Long secondOrderId = createOrder(
                memberId,
                productId,
                2
        );

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        // when & then
        mockMvc.perform(
                        get("/api/v1/orders")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))

                // 목록 형태
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders.length()").value(2))

                // 가장 나중에 생성한 주문이 먼저 반환되는지 검증
                .andExpect(jsonPath("$.orders[0].orderId")
                        .value(secondOrderId))
                .andExpect(jsonPath("$.orders[1].orderId")
                        .value(firstOrderId))

                // 요약 응답 필드 검증
                .andExpect(jsonPath("$.orders[0].status")
                        .value("PAYMENT_COMPLETED"))
                .andExpect(jsonPath(
                        "$.orders[0].representativeProductName"
                ).value("주문 당시 상품명"))
                .andExpect(jsonPath("$.orders[0].itemCount")
                        .value(1))
                .andExpect(jsonPath("$.orders[0].totalPrice")
                        .value(23_000))
                .andExpect(jsonPath("$.orders[0].orderedAt")
                        .exists())

                // 목록에서 불필요한 상세·내부 정보가 노출되지 않는지 검증
                .andExpect(jsonPath("$.orders[0].memberId")
                        .doesNotExist())
                .andExpect(jsonPath("$.orders[0].requestKey")
                        .doesNotExist())
                .andExpect(jsonPath("$.orders[0].items")
                        .doesNotExist())
                .andExpect(jsonPath("$.orders[0].recipientName")
                        .doesNotExist())
                .andExpect(jsonPath("$.orders[0].shippingAddress")
                        .doesNotExist());
    }

    @Test
    void 주문_목록에는_로그인_회원의_주문만_포함된다() throws Exception {

        // given : 회원 A
        String memberAEmail = uniqueEmail("member-a");

        Long memberAId = memberService.signup(
                memberAEmail,
                PASSWORD,
                "회원A",
                "010-1111-1111"
        );

        // given : 회원 B
        String memberBEmail = uniqueEmail("member-b");

        Long memberBId = memberService.signup(
                memberBEmail,
                PASSWORD,
                "회원B",
                "010-2222-2222"
        );

        Long categoryId = categoryService.create(
                "회원별 주문 카테고리",
                null
        );

        Long productId = createProduct(
                categoryId,
                "회원별 주문 상품",
                15_000,
                20
        );

        Long memberAOrderId = createOrder(
                memberAId,
                productId,
                1
        );

        Long memberBOrderId = createOrder(
                memberBId,
                productId,
                1
        );

        // 로그인은 A가 했음
        MockHttpSession memberASession =
                loginAndGetSession(memberAEmail, PASSWORD);

        // when & then
        mockMvc.perform(
                        get("/api/v1/orders")
                                .session(memberASession)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders.length()").value(1))

                // 회원 A의 주문은 포함
                .andExpect(jsonPath("$.orders[0].orderId")
                        .value(memberAOrderId))

                // 회원 B의 주문은 포함되지 않음
                .andExpect(jsonPath(
                        // 배열에 조건 적용한 것.
                        "$.orders[?(@.orderId == "
                                + memberBOrderId + ")]"
                ).isEmpty());
    }

    @Test
    void 주문이_없는_회원은_빈_주문_목록을_조회한다() throws Exception {

        // given
        String email = uniqueEmail("empty-order-member");

        memberService.signup(
                email,
                PASSWORD,
                "주문없는회원",
                "010-1111-2222"
        );

        MockHttpSession session = loginAndGetSession(email, PASSWORD);

        // when & then
        mockMvc.perform(
                        get("/api/v1/orders")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders").isEmpty());
    }

    @Test
    void 비로그인_사용자가_주문을_생셩하면_401을_반환한다() throws Exception {

        /**
         * POST /api/v1/orders
         * → SecurityFilterChain
         * → CSRF 검사 통과
         * → 인증 정보 확인
         * → 로그인 정보 없음
         * → API용 AuthenticationEntryPoint 실행
         * → 401 AUTHENTICATION_REQUIRED JSON 반환
         */
        mockMvc.perform(
                        post("/api/v1/orders")
                                .with(csrf())
                                .contentType(APPLICATION_JSON)
                                .content(createOrderJson(
                                        1L,
                                        1,
                                        "unauthenticated-request"
                                ))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status")
                        .value(401))
                .andExpect(jsonPath("$.code")
                        .value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message")
                        .value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/orders"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray())
                .andExpect(jsonPath("$.fieldErrors")
                        .isEmpty());
    }

    @Test
    void 로그인_회원이_주문을_생성하면_주문을_저장하고_재고를_차감한다() throws Exception {

        // given
        String email = uniqueEmail("order-create");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "주문생성회원",
                "010-1111-2222"
        );

        Long categoryId = categoryService.create(
                "주문생성 카테고리",
                null
        );

        Long productId = createProduct(
                categoryId,
                "주문생성 상품",
                10_000,
                10
        );

        MockHttpSession session =
                loginAndGetSession(email, PASSWORD);

        String requestKey =
                "create-order-" + UUID.randomUUID();

        // when
        MvcResult result = mockMvc.perform(
                        post("/api/v1/orders")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(createOrderJson(
                                        productId,
                                        2,
                                        requestKey
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.orderId").isNumber())
                .andReturn();

        // then
        // 서버가 새로 생성한 주문 리소스의 주소
        String location =
                result.getResponse().getHeader("Location");

        assertThat(location).isNotNull();
        assertThat(location)
                .startsWith("/api/v1/orders/");

        // 마지막 슬래시 뒤가 orderId
        Long orderId = extractOrderId(location);

        assertThat(result.getResponse().getContentAsString())
                .contains("\"orderId\":" + orderId);

        // 주문 상태 점검
        Order savedOrder =
                orderService.getDetail(memberId, orderId);

        assertThat(savedOrder.getMemberId())
                .isEqualTo(memberId);
        assertThat(savedOrder.getStatus())
                .isEqualTo(
                        PAYMENT_COMPLETED
                );
        assertThat(savedOrder.getRequestKey())
                .isEqualTo(requestKey);

        assertThat(savedOrder.getOrderItems())
                .hasSize(1);

        // 주문 아이템 점검
        OrderItem savedItem =
                savedOrder.getOrderItems().getFirst();

        assertThat(savedItem.getProductId())
                .isEqualTo(productId);
        assertThat(savedItem.getProductNameSnapshot())
                .isEqualTo("주문생성 상품");
        assertThat(savedItem.getUnitPriceSnapshot())
                .isEqualTo(10_000);
        assertThat(savedItem.getQuantity())
                .isEqualTo(2);

        // 주문정보 점검
        assertThat(savedOrder.getSubtotal())
                .isEqualTo(20_000);
        assertThat(savedOrder.getShippingFee())
                .isEqualTo(3_000);
        assertThat(savedOrder.getTotalPrice())
                .isEqualTo(23_000);

        Product updatedProduct =
                productService.getPublicDetail(productId);

        // 재고 차감 확인
        assertThat(updatedProduct.getStock())
                .isEqualTo(8);
    }

    @Test
    void 동일한_requestKey로_주문을_다시_생성하면_409를_반환한다() throws Exception {

        // given
        String email = uniqueEmail("duplicate-request");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "중복주문회원",
                "010-1111-2222"
        );

        Long categoryId = categoryService.create(
                "중복주문 카테고리",
                null
        );

        Long productId = createProduct(
                categoryId,
                "중복주문 상품",
                10_000,
                10
        );

        MockHttpSession session =
                loginAndGetSession(email, PASSWORD);

        String requestKey =
                "duplicate-order-" + UUID.randomUUID();

        String requestBody = createOrderJson(
                productId,
                2,
                requestKey
        );

        // 첫 번쨰 요청은 성공
        mockMvc.perform(
                        post("/api/v1/orders")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(requestBody)
                )
                .andDo(print())
                .andExpect(status().isCreated());

        Product productAfterFirstOrder = productService.getPublicDetail(productId);

        // 재고 차감 검증
        assertThat(productAfterFirstOrder.getStock())
                .isEqualTo(8);


        // when & then : 같은 requestKey 재사용
        mockMvc.perform(
                        post("/api/v1/orders")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(requestBody)       // 여기에 같은 requestKey 재사용
                )
                .andExpect(status().isConflict())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status")
                        .value(409))
                .andExpect(jsonPath("$.message")
                        .value("이미 처리된 주문 요청입니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/orders"));

        List<Order> orders =
                orderService.getMyOrders(memberId);

        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().getRequestKey())
                .isEqualTo(requestKey);

        Product productAfterDuplicateRequest =
                productService.getPublicDetail(productId);

        // 중복 요청에서는 재고 차감 안됨
        assertThat(productAfterDuplicateRequest.getStock())
                .isEqualTo(8);
    }

    @Test
    void 상품_재고가_부족하면_409를_반환하고_주문을_저장하지_않는다() throws Exception {

        // given
        String email = uniqueEmail("insufficient-stock");

        Long memberId = memberService.signup(
                email,
                PASSWORD,
                "재고부족회원",
                "010-1111-2222"
        );

        Long categoryId = categoryService.create(
                "재고부족 카테고리",
                null
        );

        Long productId = createProduct(
                categoryId,
                "재고부족 상품",
                10_000,
                1
        );

        MockHttpSession session =
                loginAndGetSession(email, PASSWORD);

        String requestKey =
                "insufficient-stock-" + UUID.randomUUID();

        /**
         * POST /api/v1/orders
         * → SecurityFilterChain
         * → OrderApiController
         * → OrderService.checkout()
         * → Product.decreaseStock()
         * → ConflictException 발생
         * → 트랜잭션 롤백
         * → ApiGlobalExceptionHandler
         * → ResponseEntity의 409 JSON 응답
         */
        // when & then
        mockMvc.perform(
                        post("/api/v1/orders")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(createOrderJson(
                                        productId,
                                        2,
                                        requestKey
                                ))
                )
                .andExpect(status().isConflict())       // Product 도메인에서 체크 후 예외 발생
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status")
                        .value(409))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/orders"));

        /*
         * 주문이 저장되지 않아야 한다.
         */
        assertThat(orderService.getMyOrders(memberId))
                .isEmpty();

        /*
         * 재고 부족으로 차감에 실패했으므로 기존 재고가 유지되어야 한다.
         */
        Product unchangedProduct =
                productService.getPublicDetail(productId);

        assertThat(unchangedProduct.getStock())
                .isEqualTo(1);
    }


    //------------------------------------------------------------------------------------------------------//

    private Long createOrder(Long memberId, Long productId, int quantity) {

        CheckoutItemCommand checkoutItem = createCheckoutItem(productId, quantity);

        CreateOrderCommand command = createOrderCommand();

        return orderService.checkout(
                memberId,
                List.of(checkoutItem),  // 주의 : List.of()는 수정할 수 없는 리스트를 반환함. 수정 시도 시 UnsupportedOperationException 발생
                command
        );
    }

    private String uniqueEmail(String prefix) {
        return prefix
                + "-"
                + System.nanoTime()
                + "@test.com";
    }

    private Long createProduct(Long categoryId, String name, int price, int stock) {
        Product product = Product.create(
                categoryId,
                name,
                price,
                stock,
                "통합 테스트 상품 설명",
                ProductStatus.ON_SALE,
                null,
                "/images/test-product.jpg"
        );

        productService.save(product);

        return product.getId();
    }

    private CheckoutItemCommand createCheckoutItem(
            Long productId,
            int quantity
    ) {
        return new CheckoutItemCommand(productId, quantity);
    }

    private CreateOrderCommand createOrderCommand() {

        return new CreateOrderCommand(
                "주문회원",
                "010-1111-2222",
                "12345",
                "서울시 테스트구",
                "101호",
                "문앞",
                "order-test-" + UUID.randomUUID()
        );
    }

    private MockHttpSession loginAndGetSession(String email, String password) throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("email", email)
                                .param("password", password)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        Object session = result.getRequest().getSession(false);

        assertThat(session)
                .as("로그인 성공 후 세션이 생성되어야 한다.")
                .isInstanceOf(MockHttpSession.class);

        return (MockHttpSession) session;
    }

    private String createOrderJson(
            Long productId,
            int quantity,
            String requestKey
    ) {
        return """
                {
                  "items": [
                    {
                      "productId": %d,
                      "quantity": %d
                    }
                  ],
                  "recipientName": "주문회원",
                  "phoneNumber": "010-1111-2222",
                  "zipCode": "12345",
                  "address": "서울시 테스트구",
                  "addressDetail": "101호",
                  "requestMessage": "문 앞에 놓아주세요",
                  "requestKey": "%s"
                }
                """.formatted(
                productId,
                quantity,
                requestKey
        );
    }

    private Long extractOrderId(String location) {
        int lastSlashIndex = location.lastIndexOf('/');

        return Long.valueOf(
                location.substring(lastSlashIndex + 1)
        );
    }
}
