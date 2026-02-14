package io.github.takgeun.shop.category.view.form;

import io.github.takgeun.shop.category.domain.CategoryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CategoryEditForm {

    @NotBlank
    @Size(max = 50, message = "카테고리명은 50자 이하입니다.")
    private String name;

    private Long parentId;

    private CategoryStatus status;
}
