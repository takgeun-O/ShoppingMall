package io.github.takgeun.shop.category.application.result;

import lombok.Getter;

/**
 * 수정 화면 표시용 DTO
 */
@Getter
public class AdminCategoryEditResult {

    private final Long id;
    private final String name;
    private final String slug;
    private final Long parentId;
    private final String parentName;

    private AdminCategoryEditResult(Long id, String name, String slug, Long parentId, String parentName) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.parentId = parentId;
        this.parentName = parentName;
    }

    public static AdminCategoryEditResult of(
            Long id,
            String name,
            String slug,
            Long parentId,
            String parentName) {
        return new AdminCategoryEditResult(id, name, slug, parentId, parentName);
    }
}
