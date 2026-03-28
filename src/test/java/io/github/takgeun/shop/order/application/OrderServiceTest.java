package io.github.takgeun.shop.order.application;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.infra.memory.MemoryCategoryRepository;
import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.ForbiddenException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.global.error.UnauthorizedException;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.member.infra.memory.MemoryMemberRepository;
import io.github.takgeun.shop.order.application.dto.CreateOrderCommand;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderRepository;
import io.github.takgeun.shop.order.domain.OrderStatus;
import io.github.takgeun.shop.order.dto.request.CheckoutItem;
import io.github.takgeun.shop.order.dto.response.OrderResponse;
import io.github.takgeun.shop.order.infra.memory.MemoryOrderRepository;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.ProductStatus;
import io.github.takgeun.shop.product.infra.memory.MemoryProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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

        this.orderRepository = orderRepository;
        this.categoryService = new CategoryService(categoryRepository, productRepository);
        this.productService = new ProductService(productRepository, categoryService);
        this.memberService = new MemberService(memberRepository);
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
        int beforeStock = productService.getForOrderPublic(productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, quantity));
        CreateOrderCommand cmd = defaultCreateOrderCommand();

        Long orderId = orderService.checkout(memberId, checkoutItems, cmd);

        int afterStock = productService.getForOrderPublic(productId).getStock();

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

        assertEquals(cmd.getRecipientName(), saved.getRecipientName());
        assertEquals(cmd.getPhoneNumber(), saved.getRecipientPhone());
        assertEquals(cmd.getZipCode(), saved.getShippingZipCode());
        assertEquals(cmd.getAddress(), saved.getShippingAddress());
        assertEquals(cmd.getAddressDetail(), saved.getShippingAddressDetail());
        assertEquals(cmd.getRequestMessage(), saved.getRequestMessage());
        assertEquals(cmd.getRequestKey(), saved.getRequestKey());

        assertEquals(beforeStock - quantity, afterStock);
    }

    @Test
    void 주문_생성_실패_로그인_필요() {
        Long memberId = null;
        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.ON_SALE);
        int beforeStock = productService.getForOrderPublic(productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 1));
        CreateOrderCommand cmd = defaultCreateOrderCommand();

        assertThrows(UnauthorizedException.class,
                () -> orderService.checkout(memberId, checkoutItems, cmd));

        int afterStock = productService.getForOrderPublic(productId).getStock();
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
        int beforeStock = productService.getForDetail(true, productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 1));
        CreateOrderCommand cmd = defaultCreateOrderCommand();

        assertThrows(ForbiddenException.class,
                () -> orderService.checkout(memberId, checkoutItems, cmd));

        int afterStock = productService.getForOrderPublic(productId).getStock();
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_판매_중이_아닌_상품() {
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.DISCONTINUED);
        int beforeStock = productService.getForDetail(true, productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 1));
        CreateOrderCommand cmd = defaultCreateOrderCommand();

        assertThrows(ConflictException.class,
                () -> orderService.checkout(memberId, checkoutItems, cmd));

        int afterStock = productService.getForDetail(true, productId).getStock();
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_주문수량_1미만() {
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.ON_SALE);
        int beforeStock = productService.getForOrderPublic(productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 0));
        CreateOrderCommand cmd = defaultCreateOrderCommand();

        assertThrows(ConflictException.class,
                () -> orderService.checkout(memberId, checkoutItems, cmd));

        int afterStock = productService.getForOrderPublic(productId).getStock();
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_재고_부족() {
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.ON_SALE);
        int beforeStock = productService.getForOrderPublic(productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 11));
        CreateOrderCommand cmd = defaultCreateOrderCommand();

        assertThrows(ConflictException.class,
                () -> orderService.checkout(memberId, checkoutItems, cmd));

        int afterStock = productService.getForOrderPublic(productId).getStock();
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

        Long orderId = createOrder(memberId, productId, 2);

        OrderResponse response = orderService.getDetail(memberId, orderId);

        assertNotNull(response);
        assertEquals(orderId, response.getOrderId());
        assertEquals(OrderStatus.PAYMENT_COMPLETED, response.getStatus());
        assertNotNull(response.getItems());
        assertEquals(1, response.getItems().size());
        assertEquals(2000, response.getSubtotal());
        assertEquals(3000, response.getShippingFee());
        assertEquals(5000, response.getTotalPrice());
        assertEquals("테스트", response.getRecipientName());
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
    void 주문_생성_실패_동일_requestKey_중복_제출() {
        Long memberId = memberService.signup(
                "dup@test.com", "pw12341234!", "중복테스트", "010-9999-8888"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = createProduct(categoryId, "노트북", 1000, 10, ProductStatus.ON_SALE);

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 1));

        String requestKey = "duplicate-key-1";

        CreateOrderCommand firstCmd = defaultCreateOrderCommand();
        firstCmd.setRequestKey(requestKey);

        CreateOrderCommand secondCmd = defaultCreateOrderCommand();
        secondCmd.setRequestKey(requestKey);

        Long firstOrderId = orderService.checkout(memberId, checkoutItems, firstCmd);

        assertNotNull(firstOrderId);
        assertThrows(ConflictException.class,
                () -> orderService.checkout(memberId, checkoutItems, secondCmd));
    }

    private Long createOrder(Long memberId, Long productId, int quantity) {
        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, quantity));
        CreateOrderCommand cmd = defaultCreateOrderCommand();
        return orderService.checkout(memberId, checkoutItems, cmd);
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
        return createOrderCommand(
                "테스트",
                "010-1234-5678",
                "12345",
                "서울시 영등포구",
                "101동 202호",
                "문 앞"
        );
    }

    private CreateOrderCommand createOrderCommand(String recipientName,
                                                  String phoneNumber,
                                                  String zipCode,
                                                  String address,
                                                  String addressDetail,
                                                  String requestMessage) {
        CreateOrderCommand cmd = new CreateOrderCommand(
                recipientName,
                phoneNumber,
                zipCode,
                address,
                addressDetail,
                requestMessage,
                "test-request-" + UUID.randomUUID()
        );
        cmd.setRequestKey("test-request-" + UUID.randomUUID());
        cmd.setRecipientName(recipientName);
        cmd.setPhoneNumber(phoneNumber);
        cmd.setZipCode(zipCode);
        cmd.setAddress(address);
        cmd.setAddressDetail(addressDetail);
        cmd.setRequestMessage(requestMessage);
        return cmd;
    }
}