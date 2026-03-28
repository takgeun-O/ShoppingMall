package io.github.takgeun.shop.order.infra.mybatis;

import io.github.takgeun.shop.order.domain.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    int insert(Order order);

    int update(Order order);

    Order findById(@Param("id") Long id);

    Order findByOrderNumber(@Param("orderNumber") String orderNumber);

    Order findByRequestKey(@Param("requestKey") String requestKey);

    List<Order> findAllByMemberId(@Param("memberId") Long memberId);

    int countByMemberId(@Param("memberId") Long memberId);

    List<Order> findAll();

    boolean existsByRequestKey(@Param("requestKey") String requestKey);
}
