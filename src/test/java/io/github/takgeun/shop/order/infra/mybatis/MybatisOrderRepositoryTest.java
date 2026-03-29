package io.github.takgeun.shop.order.infra.mybatis;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryStatus;
import io.github.takgeun.shop.category.infra.mybatis.CategoryMapper;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.infra.mybatis.MemberMapper;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderRepository;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import io.github.takgeun.shop.product.infra.mybatis.ProductMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@MybatisTest
@Import(MybatisOrderRepository.class)
@ActiveProfiles({"test", "mybatis"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MybatisOrderRepositoryTest {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private OrderRepository orderRepository;

    private Long memberId;
    private Long categoryId;
    private Long productId1;
    private Long productId2;

    @BeforeEach
    void setUp() {

        orderItemMapper.deleteAll();
        orderMapper.deleteAll();
        productMapper.deleteAll();
        categoryMapper.deleteAll();
        memberMapper.deleteAll();

        // 회원 생성
        Member member = Member.create(
                "test@test.com",
                "test1234!",
                "테스트유저",
                "010-1234-5555"
        );

        memberMapper.insert(member);
        memberId = member.getId();

        // 카테고리 생성
        Category category = Category.create(
                "전자",
                "electronics",
                null
        );
        categoryMapper.insert(category);
        categoryId = category.getId();

        // 상품 생성 (2개)
        Product product1 = Product.create(
                categoryId,
                "상품A",
                10000,
                10,
                "상품상품",
                ProductStatus.ON_SALE,
                12000,
                "image1.png"
        );
        Product product2 = Product.create(
                categoryId,
                "상품B",
                12000,
                5,
                "상품상품",
                ProductStatus.ON_SALE,
                15000,
                "image1.png"
        );
        productMapper.insert(product1);
        productMapper.insert(product2);

        productId1 = product1.getId();
        productId2 = product2.getId();
    }

    @Test
    @DisplayName("신규 주문 저장 시에 orders와 order_items 함께 저장")
    void saveNew() {

        // given
        Order order = createOrder("ORDER-20260329-0001", "req-key-1");

        // when
        Order savedOrder = orderRepository.save(order);

        // then
        assertThat(savedOrder.getId()).isNotNull();

        Order foundOrder = orderMapper.findById(savedOrder.getId());
        assertThat(foundOrder).isNotNull();
        assertThat(foundOrder.getOrderNumber()).isEqualTo("ORDER-20260329-0001");
        assertThat(foundOrder.getMemberId()).isEqualTo(memberId);

        List<OrderItem> foundItems = orderItemMapper.findAllByOrderId(foundOrder.getId());
        assertThat(foundItems).hasSize(2);
    }

    @Test
    @DisplayName("orderId로 조회하면 주문상품까지 모두 조회")
    void findById() {

        // given
        Order order = createOrder("ORDER-20260329-0001", "req-key-1");
        orderRepository.save(order);

        // when
        Optional<Order> result = orderRepository.findById(order.getId());

        // then
        assertThat(result).isPresent();

        Order found = result.get();
        assertThat(found.getOrderItems()).hasSize(2);
    }

    @Test
    void findByOrderNumber() {

        // given
        Order order = createOrder("ORDER-20260329-0001", "req-key-1");
        orderRepository.save(order);

        // when
        Optional<Order> result = orderRepository.findByOrderNumber(order.getOrderNumber());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getOrderNumber()).isEqualTo("ORDER-20260329-0001");
        assertThat(result.get().getOrderItems()).hasSize(2);
    }

    @Test
    void findByRequestKey() {

        // given
        Order order = createOrder("ORDER-20260329-0001", "req-key-1");
        orderRepository.save(order);

        // when
        Optional<Order> result = orderRepository.findByRequestKey(order.getRequestKey());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getRequestKey()).isEqualTo("req-key-1");
        assertThat(result.get().getOrderItems()).hasSize(2);
    }

    @Test
    void findAllByMemberId() {

        // given
        Order order1 = createOrder("ORDER-20260329-0001", "req-key-1");
        orderRepository.save(order1);
        Order order2 = createOrder("ORDER-20260329-0002", "req-key-2");
        orderRepository.save(order2);

        // when
        List<Order> orders = orderRepository.findAllByMemberId(memberId);

        // then
        assertThat(orders).hasSize(2);      // 주문이 2개
        assertThat(orders).allSatisfy(
                order ->
                        assertThat(order.getOrderItems()).hasSize(2));
    }

    @Test
    void countByMemberId() {

        // given
        Order order1 = createOrder("ORDER-20260329-0001", "req-key-1");
        orderRepository.save(order1);
        Order order2 = createOrder("ORDER-20260329-0002", "req-key-2");
        orderRepository.save(order2);

        // when
        int count = orderRepository.countByMemberId(memberId);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void findAll() {

        // given
        Order order1 = createOrder("ORDER-20260329-0001", "req-key-1");
        orderRepository.save(order1);
        Order order2 = createOrder("ORDER-20260329-0002", "req-key-2");
        orderRepository.save(order2);

        // when
        List<Order> orders = orderRepository.findAll();

        // then
        assertThat(orders).hasSize(2);
        assertThat(orders).allSatisfy(order ->
                assertThat(order.getOrderItems()).hasSize(2));
    }

    @Test
    void existsByRequestKey() {

        // given
        Order order = createOrder("ORDER-20260329-0001", "req-key-1");
        orderRepository.save(order);

        // when
        boolean exists = orderRepository.existsByRequestKey("req-key-1");
        boolean notExists = orderRepository.existsByRequestKey("ddddd");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }


    private Order createOrder(String orderNumber, String requestKey) {
        return Order.create(
                memberId,
                orderNumber,
                requestKey,
                List.of(
                        createOrderItem(productId1, "상품A", 10000, 12000, 1, "image1.png"),
                        createOrderItem(productId2, "상품B", 15000, 18000, 1, "image2.png")
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

    private OrderItem createOrderItem(Long productId,
                                      String productName,
                                      int unitPrice,
                                      Integer originalPrice,
                                      int quantity,
                                      String imageUrl) {
        return OrderItem.of(
                productId,
                productName,
                unitPrice,
                originalPrice,
                quantity,
                imageUrl
        );
    }
}