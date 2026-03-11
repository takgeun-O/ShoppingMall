package io.github.takgeun.shop.category.view.form;

import io.github.takgeun.shop.category.domain.CategoryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 입력 바인딩 + 검증
 */
@Getter
@Setter
@NoArgsConstructor
public class CategoryEditForm {

    @NotBlank(message = "카테고리명은 필수입니다.")
    @Size(max = 50, message = "카테고리명은 50자 이하입니다.")
    private String name;

    private Long parentId;

    public CategoryEditForm(String name, Long parentId) {
        this.name = name;
        this.parentId = parentId;
    }

    public static CategoryEditForm of(String name, Long parentId) {
        return new CategoryEditForm(name, parentId);
    }
}
