package io.github.takgeun.shop.product.infra.mybatis;

import io.github.takgeun.shop.product.domain.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mapper
public interface ProductMapper {

    int insert(Product product);

    int update(Product product);

    // Public
    Product findPublicById(@Param("id") Long id);

    List<Product> findAllPublic();

    List<Product> findAllPublicByCategoryId(@Param("categoryId") Long categoryId);

    List<Product> findAllPublicByCategoryIds(@Param("categoryIds") Set<Long> categoryIds);

    // Admin
    Optional<Product> findById(@Param("id") Long id);

    List<Product> findAllAdmin();

    List<Product> findAllAdminByCategoryId(@Param("categoryId") Long categoryId);

    List<Product> findAllAdminByCategoryIds(@Param("categoryIds") Set<Long> categoryIds);

    // 기타
    boolean existsAdminByCategoryId(@Param("categoryId") Long categoryId);

    int countAdminByCategoryId(@Param("categoryId") Long categoryId);

    int deleteById(@Param("id") Long id);

    int deleteAll();    // 테스트용
}
