package io.github.takgeun.shop.product.domain;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    List<Product> findAll();

    List<Product> findAllByCategoryId(Long categoryId);

    void deleteById(Long id);
}
