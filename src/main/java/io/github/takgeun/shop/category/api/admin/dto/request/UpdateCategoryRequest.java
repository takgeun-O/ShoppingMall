package io.github.takgeun.shop.category.api.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자 카테고리 수정 요청")
public record UpdateCategoryRequest(

        @Schema(
                description = "변경할 카테고리명",
                example = "게이밍 노트북",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "카테고리명은 필수입니다.")
        @Size(
                max = 50,
                message = "카테고리명은 50자 이하이어야 합니다."
        )
        String name,

        @Schema(
                description = "변경할 상위 카테고리 ID. 최상위로 변경하려면 null",
                example = "1",
                nullable = true
        )
        @Positive(message = "상위 카테고리 ID는 양수여야 합니다.")
        Long parentId
) {
}
