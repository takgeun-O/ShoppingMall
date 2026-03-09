package io.github.takgeun.shop.product.view.dto;

import lombok.Getter;

@Getter
public class CategoryOptionView {

    private final Long id;
    private final String name;

    public CategoryOptionView(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static CategoryOptionView of(Long id, String name) {
        return new CategoryOptionView(id, name);
    }
}
