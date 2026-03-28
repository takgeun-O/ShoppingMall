package io.github.takgeun.shop.order.infra.mybatis;

import io.github.takgeun.shop.order.domain.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    int insert(OrderItem orderItem);

    List<OrderItem> findAllByOrderId(@Param("orderId") Long orderId);

    int deleteByOrderId(@Param("orderId") Long orderId);
}
