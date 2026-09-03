package io.github.takgeun.shop.order.infra.mybatis;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.infra.mybatis.CategoryMapper;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.infra.mybatis.MemberMapper;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import io.github.takgeun.shop.product.infra.mybatis.ProductMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static io.github.takgeun.shop.TestPasswordFixtures.BCRYPT_PASSWORD;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 유효한 OrderItem을 INSERT할 수 있는가?
 * DB 생성 ID가 객체에 반영되는가?
 * orderId로 주문 상품들을 조회할 수 있는가?
 * 해당 주문의 상품만 전부 삭제할 수 있는가?
 */
@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderItemMapperTest {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;

    private Long memberId;
    private Long productId1;
    private Long productId2;
    private Long productId3;

    @BeforeEach
    void setUp() {

        Member member = Member.create(
                "test@test.com",
                BCRYPT_PASSWORD,
                "테스트유저",
                "010-1234-5555"
        );

        memberMapper.insert(member);
        memberId = member.getId();

        Category category = Category.create(
                "테스트 카테고리",
                "테스트-카테고리",
                null
        );
        categoryMapper.insert(category);

        Product product1 = createProduct(category.getId(), "상품A");
        Product product2 = createProduct(category.getId(), "상품B");
        Product product3 = createProduct(category.getId(), "상품C");

        productMapper.insert(product1);
        productMapper.insert(product2);
        productMapper.insert(product3);

        productId1 = product1.getId();
        productId2 = product2.getId();
        productId3 = product3.getId();
    }

    @Test
    void 주문상품_저장_후_주문ID로_전체조회() {
        // given
        Order order = createOrder("ORDER-20260329-0001", "req-key-1");

        int orderAffectedRows = orderMapper.insert(order);

        assertThat(orderAffectedRows).isEqualTo(1);
        assertThat(order.getId()).isNotNull();

        List<OrderItem> orderItems = createOrderItems(order.getId());

        // when
        List<Integer> affectedRows = orderItems.stream()
                .map(orderItemMapper::insert)
                .toList();

        List<OrderItem> foundItems = orderItemMapper.findAllByOrderId(order.getId());

        // then
        assertThat(affectedRows).containsExactly(1, 1, 1);
        assertThat(orderItems).extracting(OrderItem::getId).doesNotContainNull();   // insert 후 ID가 채워지는지 확인 (by Mapper XML)
        assertThat(foundItems).hasSize(3);
        assertThat(foundItems).extracting(OrderItem::getOrderId)
                .containsExactly(order.getId(), order.getId(), order.getId()
                );

        assertThat(foundItems).extracting(OrderItem::getProductId)
                .containsExactly(productId1, productId2, productId3);

        assertThat(foundItems).extracting(OrderItem::getProductNameSnapshot)
                .containsExactly("상품A", "상품B", "상품C");

        assertThat(foundItems).extracting(OrderItem::getUnitPriceSnapshot)
                .containsExactly(10_000, 10_000, 10_000);

        assertThat(foundItems).extracting(OrderItem::getQuantity)
                .containsExactly(1, 1, 1);
    }

    @Test
    void 주문ID에_해당하는_주문상품을_전부_삭제한다() {

        // given
        Order order = createOrder("ORDER-20260329-0002", "req-key-2");

        int orderAffectedRows = orderMapper.insert(order);

        assertThat(orderAffectedRows).isEqualTo(1);
        assertThat(order.getId()).isNotNull();

        List<OrderItem> orderItems = createOrderItems(order.getId());

        orderItems.forEach(orderItemMapper::insert);

        List<OrderItem> beforeDelete = orderItemMapper.findAllByOrderId(order.getId());

        assertThat(beforeDelete).hasSize(3);

        // when
        int deletedCount = orderItemMapper.deleteByOrderId(order.getId());

        List<OrderItem> afterDelete = orderItemMapper.findAllByOrderId(order.getId());

        // then
        assertThat(deletedCount).isEqualTo(3);
        assertThat(afterDelete).isEmpty();
    }

    @Test
    void insert_and_findAllByOrderId() {

        // given
        Order order = createOrder("ORDER-20260329-0001", "req-key-1");
        int orderAffectedRow = orderMapper.insert(order);   // DB에 orderItems 저장 안됨

        OrderItem item1 = createOrderItem(productId1, "상품A", "image1.png");
        OrderItem item2 = createOrderItem(productId2, "상품B", "image2.png");
        OrderItem item3 = createOrderItem(productId3, "상품C", "image3.png");

        // 주문 저장 후 생성된 orderId를 주문 상품에 연결
        item1.assignOrderId(order.getId());
        item2.assignOrderId(order.getId());
        item3.assignOrderId(order.getId());

        // when
        int itemAffectedRow1 = orderItemMapper.insert(item1);
        int itemAffectedRow2 = orderItemMapper.insert(item2);
        int itemAffectedRow3 = orderItemMapper.insert(item3);

        List<OrderItem> foundItems = orderItemMapper.findAllByOrderId(order.getId());

        // then
        assertThat(orderAffectedRow).isEqualTo(1);
        assertThat(order.getId()).isNotNull();

        assertThat(itemAffectedRow1).isEqualTo(1);
        assertThat(itemAffectedRow2).isEqualTo(1);
        assertThat(itemAffectedRow3).isEqualTo(1);

        assertThat(foundItems).hasSize(3);
        assertThat(foundItems).extracting(OrderItem::getOrderId)
                .containsExactly(1L, 1L, 1L);
        assertThat(foundItems).extracting(OrderItem::getProductId)
                .containsExactly(productId1, productId2, productId3);
        assertThat(foundItems).extracting(OrderItem::getUnitPriceSnapshot)
                .containsExactly(10000, 10000, 10000);
    }

    @Test
    void deleteByOrderId() {

        // given
        Order order = createOrder("ORDER-20260329-0001", "req-key-1");
        int orderAffectedRow = orderMapper.insert(order);   // DB에 orderItems 저장 안됨

        OrderItem item1 = createOrderItem(productId1, "상품A", "image1.png");
        OrderItem item2 = createOrderItem(productId2, "상품B", "image2.png");
        OrderItem item3 = createOrderItem(productId3, "상품C", "image3.png");

        item1.assignOrderId(order.getId());
        item2.assignOrderId(order.getId());
        item3.assignOrderId(order.getId());

        orderItemMapper.insert(item1);
        orderItemMapper.insert(item2);
        orderItemMapper.insert(item3);

        // when
        int deletedCount = orderItemMapper.deleteByOrderId(order.getId());
        List<OrderItem> foundItems = orderItemMapper.findAllByOrderId(order.getId());

        // then
        assertThat(foundItems).isEmpty();
        assertThat(deletedCount).isEqualTo(3);
    }


    private Order createOrder(String orderNumber, String requestKey) {
        return Order.create(
                memberId,
                orderNumber,
                requestKey,
                List.of(
                        createOrderItem(productId1, "상품A", "image1.png"),
                        createOrderItem(productId2, "상품B", "image2.png")
                ),
                "테스트",
                "010-1111-2222",
                "12345",
                "서울시",
                "서울시",
                "문 앞",
                3000
        );
    }

    private OrderItem createOrderItem(Long productId, String productName, String imageUrl) {
        return OrderItem.of(
                productId,
                productName,
                10000,
                12000,
                1,
                imageUrl
        );
    }

    private List<OrderItem> createOrderItems(Long orderId) {
        OrderItem item1 = createOrderItem(productId1, "상품A", "/images/product-a.png");
        OrderItem item2 = createOrderItem(productId2, "상품B", "/images/product-b.png");
        OrderItem item3 = createOrderItem(productId3, "상품C", "/images/product-c.png");

        item1.assignOrderId(orderId);
        item2.assignOrderId(orderId);
        item3.assignOrderId(orderId);

        return List.of(item1, item2, item3);
    }

    private Product createProduct(Long categoryId, String name) {
        return Product.create(
                categoryId,
                name,
                10_000,
                10,
                "OrderItemMapper 테스트 실행",
                ProductStatus.ON_SALE,
                12_000,
                "images/no-image.png"
        );
    }
}
