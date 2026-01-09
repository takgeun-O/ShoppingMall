package io.github.takgeun.shop.category.dto.response;

import io.github.takgeun.shop.category.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private Long parentId;
    private boolean active;

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getParentId(),
                category.isActive()
        );
    }
}
