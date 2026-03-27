package io.github.takgeun.shop.category.infra.mybatis;

import io.github.takgeun.shop.category.domain.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {

    int insert(Category category);

    int update(Category category);

    Category findById(@Param("id") Long id);

    List<Category> findAll();

    int deleteById(@Param("id") Long id);

    boolean existsByParentId(@Param("parentId") Long parentId);

    boolean existsByNameKey(@Param("nameKey") String nameKey);

    boolean existsBySlug(@Param("slug") String slug);

    boolean existsByNameKeyExceptId(@Param("nameKey") String nameKey,
                                    @Param("excludeId") Long excludeId);

    boolean existsBySlugExceptId(@Param("slug") String slug,
                                 @Param("categoryId") Long categoryId);
}
