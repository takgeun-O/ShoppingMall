package io.github.takgeun.shop.category.view.dto.admin;

import lombok.Getter;

/**
 * 상위 카테고리 select 용
 */
@Getter
public class CategoryOptionView {

    private final Long id;
    private final String name;

    private CategoryOptionView(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static CategoryOptionView of(Long id, String name) {
        return new CategoryOptionView(id, name);
    }
}
