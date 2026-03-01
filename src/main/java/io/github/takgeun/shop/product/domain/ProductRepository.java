package io.github.takgeun.shop.product.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    List<Product> findAll();

    List<Product> findAllByCategoryId(Long categoryId);

    List<Product> findAllByCategoryIdIn(List<Long> categoryIds);

    boolean existsByCategoryId(Long categoryId);

    List<Product> findAllPublicByCategoryId(Long categoryId);

    List<Product> findAllPublicByCategoryIds(Set<Long> categoryIds);

    List<Product> findAllAdminByCategoryIds(Set<Long> categoryIds);

    List<Product> findAllPublic();

    List<Product> findAllAdminByCategoryId(Long categoryId);

    List<Product> findAllAdmin();
}
