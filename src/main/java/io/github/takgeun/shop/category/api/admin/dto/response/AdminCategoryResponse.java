package io.github.takgeun.shop.category.api.admin.dto.response;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 카테고리 응답")
public record AdminCategoryResponse(

        @Schema(description = "카테고리 ID", example = "2")
        Long id,

        @Schema(description = "카테고리명", example = "노트북")
        String name,

        @Schema(
                description = "URL에 사용되는 카테고리 식별자",
                example = "notebook"
        )
        String slug,

        @Schema(
                description = "상위 카테고리 ID. 최상위 카테고리는 null",
                example = "1",
                nullable = true
        )
        Long parentId,

        @Schema(
                description = "카테고리 상태",
                example = "ACTIVE"
        )
        CategoryStatus status
) {

    public static AdminCategoryResponse from(
            Category category
    ) {
        return new AdminCategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getParentId(),
                category.getStatus()
        );
    }
}
