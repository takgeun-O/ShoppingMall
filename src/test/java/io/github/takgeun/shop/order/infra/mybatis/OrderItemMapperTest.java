package io.github.takgeun.shop.order.infra.mybatis;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.infra.mybatis.MemberMapper;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderItem;
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
class OrderItemMapperTest {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private MemberMapper memberMapper;

    private Long memberId;
    @Autowired
    private OrderItemMapper orderItemMapper;

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
    void insert_and_findAllByOrderId() {

        // given
        Order order = createOrder("ORDER-20260329-0001", "req-key-1");
        int orderAffectedRow = orderMapper.insert(order);   // DB에 orderItems 저장 안됨

        OrderItem item1 = createOrderItem(1L, "상품A", "image1.png");
        OrderItem item2 = createOrderItem(2L, "상품B", "image2.png");
        OrderItem item3 = createOrderItem(3L, "상품C", "image3.png");

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
                .containsExactly(1L, 2L, 3L);
        assertThat(foundItems).extracting(OrderItem::getUnitPriceSnapshot)
                .containsExactly(10000, 10000, 10000);
    }

    @Test
    void deleteByOrderId() {

        // given
        Order order = createOrder("ORDER-20260329-0001", "req-key-1");
        int orderAffectedRow = orderMapper.insert(order);   // DB에 orderItems 저장 안됨

        OrderItem item1 = createOrderItem(1L, "상품A", "image1.png");
        OrderItem item2 = createOrderItem(2L, "상품B", "image2.png");
        OrderItem item3 = createOrderItem(3L, "상품C", "image3.png");

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
                        createOrderItem(1L, "상품A", "image1.png"),
                        createOrderItem(2L, "상품B", "image2.png")
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
}