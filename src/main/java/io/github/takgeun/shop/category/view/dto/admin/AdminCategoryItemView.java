package io.github.takgeun.shop.category.view.dto.admin;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * categoryId
 * categoryName
 * slug (URL에 들어가는 문자열 ID)
 * productCount
 * parentId
 * List<AdminCategoryItemView> children
 */
@Getter
public class AdminCategoryItemView {

    private final Long id;
    private final String name;
    private final String slug;      // 공백은 '-'로 전환, 소문자 사용
    private final int productCount;
    private final Long parentId;
    private final List<AdminCategoryItemView> children;

    private AdminCategoryItemView(Long id,
                                 String name,
                                 String slug,
                                 int productCount,
                                 Long parentId,
                                 List<AdminCategoryItemView> children) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.productCount = productCount;
        this.parentId = parentId;
        this.children = children;
    }

    public static AdminCategoryItemView of(
            Long id,
            String name,
            String slug,
            int productCount,
            Long parentId
    ) {
        return new AdminCategoryItemView(
                id,
                name,
                slug,
                productCount,
                parentId,
                new ArrayList<>()
        );
    }

    public void addChild(AdminCategoryItemView child) {
        this.children.add(child);
    }
}
