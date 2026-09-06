package io.github.takgeun.shop.order.application;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.infra.memory.MemoryCategoryRepository;
import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.ForbiddenException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.global.error.exception.UnauthorizedException;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.member.infra.memory.MemoryMemberRepository;
import io.github.takgeun.shop.order.application.dto.CreateOrderCommand;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderRepository;
import io.github.takgeun.shop.order.domain.OrderStatus;
import io.github.takgeun.shop.order.dto.request.CheckoutItem;
import io.github.takgeun.shop.order.infra.memory.MemoryOrderRepository;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.ProductStatus;
import io.github.takgeun.shop.product.infra.memory.MemoryProductRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderService orderService;
    private CategoryService categoryService;
    private ProductService productService;
    private MemberService memberService;

    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        MemoryOrderRepository orderRepository = new MemoryOrderRepository();
        MemoryCategoryRepository categoryRepository = new MemoryCategoryRepository();
        MemoryProductRepository productRepository = new MemoryProductRepository();
        MemoryMemberRepository memberRepository = new MemoryMemberRepository();
        /**
         * 이 테스트에서 MemberService를 사용하는 케이스는 회원가입과 회원 조회뿐임.
         * 회원가입과 회원 조회에 일반적으로 세션 만료 이벤트를 발행하지 않으므로 Publisher가 실제로 작동할 필요 없음
         * 따라서 eventPublisher는 Mockito Mock으로 충분
         */
        ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);

        this.orderRepository = orderRepository;
        this.categoryService = new CategoryService(categoryRepository, productRepository);
        this.productService = new ProductService(productRepository, categoryService);
        this.memberService = new MemberService(memberRepository, new BCryptPasswordEncoder(4), eventPublisher);
        this.orderService = new OrderService(orderRepository, productService, memberService);
    }

    @Test
    void 주문_생성_성공() {
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.ON_SALE);

        int quantity = 2;
        int beforeStock = productService.getForOrder(productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, quantity));
        CreateOrderCommand cmd = defaultCreateOrderCommand();

        Long orderId = orderService.checkout(memberId, checkoutItems, cmd);

        int afterStock = productService.getForOrder(productId).getStock();

        assertNotNull(orderId);

        Order saved = orderRepository.findById(orderId).orElseThrow();
        assertEquals(memberId, saved.getMemberId());
        assertEquals(OrderStatus.PAYMENT_COMPLETED, saved.getStatus());

        assertNotNull(saved.getOrderItems());
        assertEquals(1, saved.getOrderItems().size());

        OrderItem item = saved.getOrderItems().get(0);
        assertEquals(productId, item.getProductId());
        assertEquals("노트북", item.getProductNameSnapshot());
        assertEquals(1000, item.getUnitPriceSnapshot());
        assertEquals(quantity, item.getQuantity());

        assertEquals(1000 * quantity, saved.getSubtotal());
        assertEquals(3000, saved.getShippingFee());
        assertEquals(1000 * quantity + 3000, saved.getTotalPrice());

        assertEquals(cmd.recipientName(), saved.getRecipientName());
        assertEquals(cmd.phoneNumber(), saved.getRecipientPhone());
        assertEquals(cmd.zipCode(), saved.getShippingZipCode());
        assertEquals(cmd.address(), saved.getShippingAddress());
        assertEquals(cmd.addressDetail(), saved.getShippingAddressDetail());
        assertEquals(cmd.requestMessage(), saved.getRequestMessage());
        assertEquals(cmd.requestKey(), saved.getRequestKey());

        assertEquals(beforeStock - quantity, afterStock);
    }

    @Test
    void 주문_생성_실패_로그인_필요() {
        Long memberId = null;
        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.ON_SALE);
        int beforeStock = productService.getForOrder(productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 1));
        CreateOrderCommand cmd = defaultCreateOrderCommand();

        assertThrows(UnauthorizedException.class,
                () -> orderService.checkout(memberId, checkoutItems, cmd));

        int afterStock = productService.getForOrder(productId).getStock();
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_비활성_회원() {
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        memberService.changeStatus(memberId, MemberStatus.INACTIVE);

        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.ON_SALE);
        int beforeStock = productService.getAdminDetail(productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 1));
        CreateOrderCommand cmd = defaultCreateOrderCommand();

        assertThrows(ForbiddenException.class,
                () -> orderService.checkout(memberId, checkoutItems, cmd));

        int afterStock = productService.getForOrder(productId).getStock();
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_판매_중이_아닌_상품() {
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.DISCONTINUED);
        int beforeStock = productService.getAdminDetail(productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 1));
        CreateOrderCommand cmd = defaultCreateOrderCommand();

        assertThrows(ConflictException.class,
                () -> orderService.checkout(memberId, checkoutItems, cmd));

        int afterStock = productService.getAdminDetail(productId).getStock();
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_주문수량_1미만() {
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.ON_SALE);
        int beforeStock = productService.getForOrder(productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 0));
        CreateOrderCommand cmd = defaultCreateOrderCommand();

        assertThrows(ConflictException.class,
                () -> orderService.checkout(memberId, checkoutItems, cmd));

        int afterStock = productService.getForOrder(productId).getStock();
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_재고_부족() {
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.ON_SALE);
        int beforeStock = productService.getForOrder(productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 11));
        CreateOrderCommand cmd = defaultCreateOrderCommand();

        assertThrows(ConflictException.class,
                () -> orderService.checkout(memberId, checkoutItems, cmd));

        int afterStock = productService.getForOrder(productId).getStock();
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_상품_없음() {
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(999L, 2));
        CreateOrderCommand cmd = defaultCreateOrderCommand();

        assertThrows(NotFoundException.class,
                () -> orderService.checkout(memberId, checkoutItems, cmd));
    }

    @Test
    void 주문_상세조회_성공() {
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.ON_SALE);

        CreateOrderCommand command = defaultCreateOrderCommand();

        Long orderId = createOrder(memberId, productId, 2, command);

        Order order = orderService.getDetail(memberId, orderId);

        assertNotNull(order);
        assertEquals(orderId, order.getId());
        assertEquals(memberId, order.getMemberId());
        assertEquals(OrderStatus.PAYMENT_COMPLETED, order.getStatus());

        assertNotNull(order.getOrderItems());
        assertEquals(1, order.getOrderItems().size());

        assertEquals(2000, order.getSubtotal());
        assertEquals(3000, order.getShippingFee());
        assertEquals(5000, order.getTotalPrice());

        assertEquals(command.recipientName(), order.getRecipientName());
        assertEquals(command.phoneNumber(), order.getRecipientPhone());
        assertEquals(command.zipCode(), order.getShippingZipCode());
        assertEquals(command.address(), order.getShippingAddress());
        assertEquals(command.addressDetail(), order.getShippingAddressDetail());
        assertEquals(command.requestMessage(), order.getRequestMessage());
        assertEquals(command.requestKey(), order.getRequestKey());
    }

    @Test
    void 주문_상세조회_실패_로그인_필요() {
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.ON_SALE);

        Long orderId = createOrder(memberId, productId, 2);

        assertThrows(UnauthorizedException.class,
                () -> orderService.getDetail(null, orderId));
    }

    @Test
    void 주문_상세조회_실패_본인주문_아님() {
        Long memberId1 = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long memberId2 = memberService.signup(
                "userTest2@test.com", "pw12341234!", "테스트2", "010-1111-5678"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.ON_SALE);

        Long orderId = createOrder(memberId1, productId, 2);

        assertThrows(ForbiddenException.class,
                () -> orderService.getDetail(memberId2, orderId));
    }

    @Test
    void 주문_상세조회_실패_존재하지_않는_주문() {
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );

        assertThrows(NotFoundException.class,
                () -> orderService.getDetail(memberId, 999L));
    }

    @Test
    void 주문_상세조회_실패_주문ID가_양수가_아님() {
        Long memberId = memberService.signup(
                "invalid-order-id@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );

        assertThrows(IllegalArgumentException.class,
                () -> orderService.getDetail(memberId, 0L));
    }

    @Test
    void 주문_생성_실패_동일_requestKey_중복_제출() {
        Long memberId = memberService.signup(
                "dup@test.com", "pw12341234!", "중복테스트", "010-9999-8888"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.ON_SALE);

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 1));

        String requestKey = "duplicate-key-1";

        CreateOrderCommand firstCmd = defaultCreateOrderCommand(requestKey);
        CreateOrderCommand secondCmd = defaultCreateOrderCommand(requestKey);

        Long firstOrderId = orderService.checkout(memberId, checkoutItems, firstCmd);

        assertNotNull(firstOrderId);
        assertThrows(ConflictException.class,
                () -> orderService.checkout(memberId, checkoutItems, secondCmd));
    }

    @Test
    void 회원의_주문_목록을_조회한다() {

        // given
        Long memberId = memberService.signup(
                "test@test.com",
                "pw12341234!",
                "테스트",
                "010-1111-2222"
        );

        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.ON_SALE);

        Long orderId1 = createOrder(memberId, productId, 1);
        Long orderId2 = createOrder(memberId, productId, 2);

        // when
        List<Order> result = orderService.getMyOrders(memberId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Order::getId)
                .containsExactlyInAnyOrder(orderId1, orderId2);
    }

    @Test
    void 회원ID가_null이면_주문_목록을_조회할_수_없다() {
        assertThatThrownBy(
                () -> orderService.getMyOrders(null))
                .isInstanceOf(UnauthorizedException.class);
    }

    private Long createOrder(Long memberId, Long productId, int quantity) {
        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, quantity));
        CreateOrderCommand cmd = defaultCreateOrderCommand();
        return orderService.checkout(memberId, checkoutItems, cmd);
    }

    private Long createOrder(
            Long memberId,
            Long productId,
            int quantity,
            CreateOrderCommand command
    ) {
        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, quantity));

        return orderService.checkout(
                memberId,
                checkoutItems,
                command
        );
    }

    private Long createProduct(Long categoryId, String name, int price, int stock, ProductStatus status) {
        return productService.create(
                categoryId,
                name,
                price,
                stock,
                "좋은 상품",
                status,
                null,
                null
        );
    }

    private CreateOrderCommand defaultCreateOrderCommand() {
        return defaultCreateOrderCommand(
                "test-request-" + UUID.randomUUID()
        );
    }

    private CreateOrderCommand createOrderCommand(
            String recipientName,
            String phoneNumber,
            String zipCode,
            String address,
            String addressDetail,
            String requestMessage,
            String requestKey
    ) {

        return new CreateOrderCommand(
                recipientName,
                phoneNumber,
                zipCode,
                address,
                addressDetail,
                requestMessage,
                requestKey
        );
    }

    private CreateOrderCommand defaultCreateOrderCommand(String requestKey) {
        return new CreateOrderCommand(
                "주문회원",
                "010-1111-2222",
                "12345",
                "서울시",
                "101호",
                "문앞",
                requestKey
        );
    }

    private Order order(
            Long memberId,
            Long orderId
    ) {
        Order order = mock(Order.class);

        when(order.getId())
                .thenReturn(orderId);
        when(order.getMemberId())
                .thenReturn(memberId);

        return order;
    }
}
