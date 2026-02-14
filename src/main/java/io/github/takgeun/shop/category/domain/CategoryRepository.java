package io.github.takgeun.shop.category.domain;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(Long id);

    List<Category> findAll();

    void deleteById(Long id);

    // 하위 카테고리 존재 여부 (삭제에 사용)
    boolean existsByParentId(Long parentId);

    // 중복 체크 (case-insensitive)
    // 서비스에서 Category.normalizeKey(name)로 키 만들고 전달하는 방식
    boolean existsByNameKey(String nameKey);

    // 수정 시 내 자신 제외 중복 체크
    boolean existsByNameKeyExceptId(String name, Long excludeId);
}
