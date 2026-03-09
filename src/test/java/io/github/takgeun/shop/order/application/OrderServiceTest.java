package io.github.takgeun.shop.order.application;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.infra.MemoryCategoryRepository;
import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.ForbiddenException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.global.error.UnauthorizedException;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.infra.MemoryMemberRepository;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderRepository;
import io.github.takgeun.shop.order.domain.OrderStatus;
import io.github.takgeun.shop.order.dto.request.CheckoutItem;
import io.github.takgeun.shop.order.dto.response.OrderResponse;
import io.github.takgeun.shop.order.infra.MemoryOrderRepository;
import io.github.takgeun.shop.order.view.form.CheckoutForm;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.ProductStatus;
import io.github.takgeun.shop.product.infra.MemoryProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    private OrderService orderService;
    private CategoryService categoryService;
    private ProductService productService;
    private MemberService memberService;

    private OrderRepository orderRepository;        // 검증용(조회)

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

        // given
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(categoryId, "노트북", 1000, 10, "좋은 노트북");
        productService.changeStatus(productId, ProductStatus.ON_SALE);

        int quantity = 2;
        int beforeStock = productService.getForOrderPublic(productId).getStock();   // 10
        String recipientName = "테스트";
        String recipientPhone = "010-1111-2222";
        String shippingZipCode = "12345";
        String shippingAddress = "서울시 영등포구";
        String shippingAddressDetail = "101동 202호";
        String requestMessage = "문 앞에 두세요.";

        // when
        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, quantity));

        CheckoutForm form = new CheckoutForm();
        form.setRecipientName(recipientName);
        form.setPhoneNumber(recipientPhone);
        form.setZipCode(shippingZipCode);
        form.setAddress(shippingAddress);
        form.setAddressDetail(shippingAddressDetail);
        form.setRequestMessage(requestMessage);

        Long orderId = orderService.checkout(memberId, checkoutItems, form);

        // then
        int afterStock = productService.getForOrderPublic(productId).getStock();    // 8 (2개 주문했으니까)

        assertNotNull(orderId);
        Order saved = orderRepository.findById(orderId).orElseThrow();
        assertEquals(memberId, saved.getMemberId());        // 주문아이디 일치 확인
        assertEquals(OrderStatus.PAYMENT_COMPLETED, saved.getStatus()); // 배송상태 확인

        assertNotNull(saved.getItems());
        assertEquals(1, saved.getItems().size());   // 주문아이템 종류 개수 확인

        OrderItem item = saved.getItems().get(0);
        assertEquals(productId, item.getProductId());   // 주문 당시 스냅샷 확인
        assertEquals("노트북", item.getProductNameSnapshot());
        assertEquals(1000, item.getUnitPriceSnapshot());
        assertEquals(quantity, item.getQuantity());

        assertEquals(1000 * quantity, saved.getSubtotal()); // 주문 금액 확인
        assertEquals(3000, saved.getShippingFee());
        assertEquals(1000 * quantity + 3000, saved.getTotalPrice());

        assertEquals(recipientName, saved.getRecipientName());
        assertEquals(recipientPhone, saved.getRecipientPhone());
        assertEquals(shippingZipCode, saved.getShippingZipCode());
        assertEquals(shippingAddress, saved.getShippingAddress());
        assertEquals(shippingAddressDetail, saved.getShippingAddressDetail());
        assertEquals(requestMessage, saved.getRequestMessage());

        assertEquals(beforeStock - quantity, afterStock);       // 주문성공 후 재고 확인
    }

    @Test
    void 주문_생성_실패_로그인_필요() {

        // given
        Long memberId = null;
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(categoryId, "노트북", 1000, 10, "좋은 노트북");
        productService.changeStatus(productId, ProductStatus.ON_SALE);
        int beforeStock = productService.getForOrderPublic(productId).getStock();   // 10

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 1));
        CheckoutForm form = defaultCheckoutForm();

        // when


        // then
        assertThrows(ForbiddenException.class,
                () -> orderService.checkout(memberId, checkoutItems, form));

        int afterStock = productService.getForOrderPublic(productId).getStock();
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_비활성_회원() {

        // given
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(categoryId, "노트북", 1000, 10, "좋은 노트북");
        productService.changeStatus(productId, ProductStatus.ON_SALE);
        int beforeStock = productService.getForDetail(true, productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 1));
        CheckoutForm form = defaultCheckoutForm();

        // when


        // then
        assertThrows(ForbiddenException.class,
                () -> orderService.checkout(memberId, checkoutItems, form));

        int afterStock = productService.getForOrderPublic(productId).getStock();
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_판매_중이_아닌_상품() {

        // given
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(categoryId, "노트북", 1000, 10, "좋은 노트북");
        productService.changeStatus(productId, ProductStatus.DISCONTINUED);
        int beforeStock = productService.getForDetail(true, productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 1));
        CheckoutForm form = defaultCheckoutForm();

        // when


        // then
        assertThrows(NotFoundException.class,
                () -> orderService.checkout(memberId, checkoutItems, form));

        int afterStock = productService.getForDetail(true, productId).getStock();
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_주문수량_1미만() {

        // given
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(categoryId, "노트북", 1000, 10, "좋은 노트북");
        productService.changeStatus(productId, ProductStatus.ON_SALE);
        int beforeStock = productService.getForOrderPublic(productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 0));
        CheckoutForm form = defaultCheckoutForm();

        // when


        // then
        assertThrows(ConflictException.class,
                () -> orderService.checkout(memberId, checkoutItems, form));

        int afterStock = productService.getForOrderPublic(productId).getStock();
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_재고_부족() {

        // given
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(categoryId, "노트북", 1000, 10, "좋은 노트북");
        productService.changeStatus(productId, ProductStatus.ON_SALE);
        int beforeStock = productService.getForOrderPublic(productId).getStock();

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, 11));
        CheckoutForm form = defaultCheckoutForm();

        // when


        // then
        assertThrows(ConflictException.class,
                () -> orderService.checkout(memberId, checkoutItems, form));

        int afterStock = productService.getForOrderPublic(productId).getStock();
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_상품_없음() {

        // given
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );

        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(999L, 2));
        CheckoutForm form = defaultCheckoutForm();

        // when


        // then
        assertThrows(NotFoundException.class,
                () -> orderService.checkout(memberId, checkoutItems, form));
    }

    @Test
    void 주문_상세조회_성공() {

        // given
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(categoryId, "노트북", 1000, 10, "좋은 노트북");
        productService.changeStatus(productId, ProductStatus.ON_SALE);

        Long orderId = createOrder(memberId, productId, 2);

        // when
        OrderResponse response = orderService.getDetail(memberId, orderId);

        // then
        assertNotNull(response);
        assertEquals(orderId, response.getOrderId());
        assertEquals(OrderStatus.PAYMENT_COMPLETED, response.getStatus());

        assertNotNull(response.getItems()); // 빈 주문 성공은 X
        assertEquals(1, response.getItems().size());

        assertEquals(2000, response.getSubtotal());
        assertEquals(3000, response.getShippingFee());
        assertEquals(5000, response.getTotalPrice());
        assertEquals("테스트", response.getRecipientName());
    }

    @Test
    void 주문_상세조회_실패_로그인_필요() {

        // given
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(categoryId, "노트북", 1000, 10, "좋은 노트북");
        productService.changeStatus(productId, ProductStatus.ON_SALE);

        Long orderId = createOrder(memberId, productId, 2);

        // when

        // then
        assertThrows(UnauthorizedException.class,
                () -> orderService.getDetail(null, orderId));
    }

    @Test
    void 주문_상세조회_실패_본인주문_아님() {

        // given
        Long memberId1 = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        Long memberId2 = memberService.signup(
                "userTest2@test.com", "pw12341234!", "테스트2", "010-1111-5678"
        );
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(categoryId, "노트북", 1000, 10, "좋은 노트북");
        productService.changeStatus(productId, ProductStatus.ON_SALE);

        Long orderId = createOrder(memberId1, productId, 2);

        // when


        // then
        assertThrows(ForbiddenException.class,
                () -> orderService.getDetail(memberId2, orderId));
    }

    @Test
    void 주문_상세조회_실패_존재하지_않는_주문() {
        // given
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );

        // when


        // then
        assertThrows(NotFoundException.class,
                () -> orderService.getDetail(memberId, 999L));
    }

    private Long createOrder(Long memberId, Long productId, int quantity) {
        List<CheckoutItem> checkoutItems = List.of(CheckoutItem.of(productId, quantity));
        CheckoutForm form = defaultCheckoutForm();
        return orderService.checkout(memberId, checkoutItems, form);
    }

    private CheckoutForm defaultCheckoutForm() {
        return createCheckoutForm(
                "테스트",
                "010-1234-5678",
                "12345",
                "서울시 영등포구",
                "101동 202호",
                "문 앞"
        );
    }

    private CheckoutForm createCheckoutForm(String recipientName, String phoneNumber, String zipCode, String address, String addressDetail, String requestMessage) {
        CheckoutForm form = new CheckoutForm();
        form.setRecipientName(recipientName);
        form.setPhoneNumber(phoneNumber);
        form.setZipCode(zipCode);
        form.setAddress(address);
        form.setAddressDetail(addressDetail);
        form.setRequestMessage(requestMessage);
        return form;
    }
}