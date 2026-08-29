package io.github.takgeun.shop.category.api.dto.response;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 내부 정보는 공개 응답에서 제외
 * nameKey
 * 내부 상태 관리 정보
 * MyBatis/JPA 구현 관련 정보
 */
@Schema(description = "공개 카테고리 응답")
public record CategoryResponse(

        @Schema(
                description = "카테고리 ID",
                example = "1"
        )
        Long id,

        @Schema(
                description = "카테고리명",
                example = "전자"
        )
        String name,

        @Schema(
                description = "URL 등에 사용하는 카테고리 식별 문자열",
                example = "electronics"
        )
        String slug,

        @Schema(
                description = "부모 카테고리 ID. 루트 카테고리이면 null",
                example = "1",
                nullable = true
        )
        Long parentId
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getParentId()
        );
    }
}
