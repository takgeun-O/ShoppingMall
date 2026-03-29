package io.github.takgeun.shop.order.infra.mybatis;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.infra.mybatis.MemberMapper;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
import io.github.takgeun.shop.order.domain.OrderStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private MemberMapper memberMapper;

    private Long memberId;

    @BeforeEach
    void setUp() {

        Member member = new Member(
                "test@test.com",
                "test1234!",
                "테스트유저",
                "010-1234-5555"
        );

        memberMapper.insert(member);
        memberId = member.getId();
    }

    @Test
    void insert_and_findById() {

        // given
        Order order = createOrder("ORDER-20260329-0001", "req-key-1");

        // when
        int affectedRow = orderMapper.insert(order);
        Order found = orderMapper.findById(order.getId());

        // then
        assertThat(affectedRow).isEqualTo(1);
        assertThat(order.getId()).isNotNull();

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(order.getId());
        assertThat(found.getOrderNumber()).isEqualTo("ORDER-20260329-0001");
        // 순수 도메인 생성 자체에서 ORDERED
        assertThat(found.getStatus()).isEqualTo(OrderStatus.ORDERED);
        assertThat(found.getRequestKey()).isEqualTo("req-key-1");

        assertThat(found.getRecipientName()).isEqualTo("테스트");

        assertThat(found.getSubtotal()).isEqualTo(20000);
        assertThat(found.getShippingFee()).isEqualTo(3000);
        assertThat(found.getTotalPrice()).isEqualTo(23000);

        assertThat(found.getOrderedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getCanceledAt()).isNull();
    }

    @Test
    void findByOrderNumber() {

        // given
        Order order = createOrder("ORDER-20260329-0001", "req-key-1");
        orderMapper.insert(order);

        // when
        Order found = orderMapper.findByOrderNumber(order.getOrderNumber());

        // then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(order.getId());
    }

    @Test
    void findByRequestKey() {

        // given
        Order order = createOrder("ORDER-20260329-0001", "req-key-1");
        orderMapper.insert(order);

        // when
        Order found = orderMapper.findByRequestKey(order.getRequestKey());

        // then
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(order.getId());
    }

    @Test
    void findAllByMemberId() {

        // given
        Order order1 = createOrder("ORDER-20260329-0001", "req-key-1");
        Order order2 = createOrder("ORDER-20260329-0002", "req-key-2");

        orderMapper.insert(order1);
        orderMapper.insert(order2);

        // when
        List<Order> orders = orderMapper.findAllByMemberId(memberId);

        // then
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(Order::getOrderNumber)
                .contains("ORDER-20260329-0001", "ORDER-20260329-0002");
    }

    @Test
    void countByMemberId() {

        // given
        Order order1 = createOrder("ORDER-20260329-0001", "req-key-1");
        Order order2 = createOrder("ORDER-20260329-0002", "req-key-2");

        orderMapper.insert(order1);
        orderMapper.insert(order2);

        // when
        int count = orderMapper.countByMemberId(memberId);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void findALl() {

        // given
        Order order1 = createOrder("ORDER-20260329-0001", "req-key-1");
        Order order2 = createOrder("ORDER-20260329-0002", "req-key-2");

        orderMapper.insert(order1);
        orderMapper.insert(order2);

        // when
        List<Order> orders = orderMapper.findAll();

        // then
        assertThat(orders).hasSize(2);
    }

    @Test
    void existsByRequestKey() {

        // given
        Order order = createOrder("ORDER-20260329-0001", "req-key-1");

        orderMapper.insert(order);

        // when
        boolean exists = orderMapper.existsByRequestKey(order.getRequestKey());
        boolean notExists = orderMapper.existsByRequestKey("not-exists");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void update() {

        // given
        Order order = createOrder("ORDER-20260329-0001", "req-key-1");
        orderMapper.insert(order);

        order.markPaymentCompleted();

        // when
        int updated = orderMapper.update(order);
        Order found = orderMapper.findById(order.getId());

        // then
        assertThat(updated).isEqualTo(1);
        assertThat(found.getStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);
    }


    //-----------------------------------------------------------------------

    private Order createOrder(String orderNumber, String requestKey) {
        return Order.create(
                memberId,
                orderNumber,
                requestKey,
                List.of(
                        createOrderItem(1L),
                        createOrderItem(2L)
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

    private OrderItem createOrderItem(Long productId) {
        return OrderItem.of(
                productId,
                "상품A",
                10000,
                12000,
                1,
                "image1.png"
        );
    }
}