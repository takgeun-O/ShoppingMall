package io.github.takgeun.shop.category.api.dto.response;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 내부 정보는 공개 응답에서 제외
 * nameKey
 * 내부 상태 관리 정보
 * MyBatis/JPA 구현 관련 정보
 */
public record CategoryResponse(
        Long id,
        String name,
        String slug,
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
