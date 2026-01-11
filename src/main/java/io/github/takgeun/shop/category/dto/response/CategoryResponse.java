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

    // 도메인 객체(Category)를 응답 DTO(CategoryResponse)로 변환할 때 쓰는 전용 메서드
    // 컨트롤러나 서비스 등 다른 곳에서 아래 코드가 반복되는 걸 방지하기 위함.
    // 또는 컨트롤러나 서비스가 DTO 구조를 몰라도 된다는 장점도 있음. (변경에 강함)
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getParentId(),
                category.isActive()
        );
    }
}
