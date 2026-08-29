package io.github.takgeun.shop.global.error.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Bean Validation 필드별 오류
 */
@Schema(description = "필드 검증 오류")
public record FieldErrorResponse(

        @Schema(
                description = "검증에 실패한 필드",
                example = "name"
        )
        String field,

        @Schema(
                description = "필드 검증 실패 사유",
                example = "이름은 필수입니다."
        )
        String reason
) {
}
