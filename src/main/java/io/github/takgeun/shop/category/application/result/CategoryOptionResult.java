package io.github.takgeun.shop.category.application.result;

import lombok.Getter;

/**
 * 상위 카테고리 select 용
 */
@Getter
public class CategoryOptionResult {

    private final Long id;
    private final String name;

    private CategoryOptionResult(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static CategoryOptionResult of(Long id, String name) {
        return new CategoryOptionResult(id, name);
    }
}
