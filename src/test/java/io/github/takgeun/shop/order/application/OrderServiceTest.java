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
import io.github.takgeun.shop.order.domain.OrderRepository;
import io.github.takgeun.shop.order.dto.response.OrderResponse;
import io.github.takgeun.shop.order.infra.MemoryOrderRepository;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.ProductStatus;
import io.github.takgeun.shop.product.infra.MemoryProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        int beforeStock = productService.get(productId).getStock();
        String recipientName = "테스트";
        String recipientPhone = "010-1111-2222";
        String shippingZipCode = "12345";
        String shippingAddress = "서울시 영등포구";
        String requestMessage = "문 앞에 두세요.";

        // when
        Long orderId = orderService.create(memberId, productId, quantity, recipientName, recipientPhone,
                shippingZipCode, shippingAddress, requestMessage);

        // then
        int afterStock = productService.get(productId).getStock();
        assertNotNull(orderId);

        Order saved = orderRepository.findById(orderId).orElseThrow();
        assertEquals(memberId, saved.getMemberId());
        assertEquals(productId, saved.getProductId());
        assertEquals(quantity, saved.getQuantity());
        assertEquals(1000 * 2, saved.getTotalPrice());
        // 재고 차감 확인
        assertNotEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_로그인_필요() {

        // given
        Long memberId = null;
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(categoryId, "노트북", 1000, 10, "좋은 노트북");
        productService.changeStatus(productId, ProductStatus.ON_SALE);
        int beforeStock = productService.get(productId).getStock();

        // when


        // then
        assertThrows(UnauthorizedException.class,
                () -> orderService.create(
                        memberId, productId, 1, "테스트", "010-1234-5678",
                        "12345", "서울 영등포구", null)
                );
        int afterStock = productService.get(productId).getStock();
        // 주문 실패 시 상품재고 수량 변화 없음 확인
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_비활성_회원() {

        // given
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );
        memberService.deactivate(memberId);     // 비활성
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(categoryId, "노트북", 1000, 10, "좋은 노트북");
        productService.changeStatus(productId, ProductStatus.ON_SALE);
        int beforeStock = productService.get(productId).getStock();

        // when


        // then
        assertThrows(ForbiddenException.class,
                () -> orderService.create(
                        memberId, productId, 1, "테스트", "010-1234-5678",
                        "12345", "서울 영등포구", null)
        );
        int afterStock = productService.get(productId).getStock();
        // 주문 실패 시 상품재고 수량 변화 없음 확인
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
        int beforeStock = productService.get(productId).getStock();

        // when


        // then
        assertThrows(ConflictException.class,
                () -> orderService.create(
                        memberId, productId, 1, "테스트", "010-1234-5678",
                        "12345", "서울 영등포구", null)
        );
        int afterStock = productService.get(productId).getStock();
        // 주문 실패 시 상품재고 수량 변화 없음 확인
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
        int beforeStock = productService.get(productId).getStock();

        // when


        // then
        assertThrows(IllegalArgumentException.class,
                () -> orderService.create(
                        memberId, productId, 0, "테스트", "010-1234-5678",
                        "12345", "서울 영등포구", null)
        );
        int afterStock = productService.get(productId).getStock();
        // 주문 실패 시 상품재고 수량 변화 없음 확인
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
        int beforeStock = productService.get(productId).getStock();

        // when


        // then
        assertThrows(ConflictException.class,
                () -> orderService.create(
                        memberId, productId, 11, "테스트", "010-1234-5678",
                        "12345", "서울 영등포구", null)
        );
        int afterStock = productService.get(productId).getStock();
        // 주문 실패 시 상품재고 수량 변화 없음 확인
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void 주문_생성_실패_상품_없음() {

        // given
        Long memberId = memberService.signup(
                "userTest@test.com", "pw12341234!", "테스트", "010-1111-2222"
        );

        // when


        // then
        assertThrows(NotFoundException.class,
                () -> orderService.create(
                        memberId, 999L, 2, "테스트", "010-1234-5678",
                        "12345", "서울 영등포구", null)
        );
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

        Long orderId = orderService.create(memberId, productId, 2, "테스트",
                "010-1234-5678", "12345", "서울시 영등포구", null);

        // when
        OrderResponse response = orderService.getDetail(memberId, orderId);

        // then
        assertNotNull(response);
        assertEquals(orderId, response.getOrderId());
        assertEquals(productId, response.getProductId());
        assertEquals(2, response.getQuantity());
        assertEquals(2000, response.getTotalPrice());
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

        Long orderId = orderService.create(memberId, productId, 2, "테스트",
                "010-1234-5678", "12345", "서울시 영등포구", null);

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

        Long orderId = orderService.create(memberId1, productId, 2, "테스트",
                "010-1234-5678", "12345", "서울시 영등포구", null);

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
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(categoryId, "노트북", 1000, 10, "좋은 노트북");
        productService.changeStatus(productId, ProductStatus.ON_SALE);

        Long orderId = orderService.create(memberId, productId, 2, "테스트",
                "010-1234-5678", "12345", "서울시 영등포구", null);

        // when


        // then
        assertThrows(NotFoundException.class,
                () -> orderService.getDetail(memberId, 999L));
    }
}