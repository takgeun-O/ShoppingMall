package io.github.takgeun.shop.category.domain;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(Long id);

    List<Category> findAll();

    void deleteById(Long id);

    boolean existsByName(String name);

    boolean existsByParentId(Long parentId);
}
