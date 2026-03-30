package io.github.takgeun.shop.product.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProductRepository {

    Product save(Product product);

    // Public
    Optional<Product> findPublicById(Long id);

    List<Product> findAllPublic();

    List<Product> findAllPublicByCategoryId(Long categoryId);

    List<Product> findAllPublicByCategoryIds(Set<Long> categoryIds);

    // Admin
    Optional<Product> findById(Long id);

    List<Product> findAllAdmin();

    List<Product> findAllAdminByCategoryId(Long categoryId);

    List<Product> findAllAdminByCategoryIds(Set<Long> categoryIds);

    void deleteById(Long productId);

    // 기타
    boolean existsAdminByCategoryId(Long categoryId);

    int countAdminByCategoryId(Long categoryId);
}
