package io.github.takgeun.shop.global.error.api;

import io.github.takgeun.shop.global.error.code.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 모든 API 오류의 공통 JSON 형식
 */
@Schema(description = "API 공통 오류 응답")
public record ApiErrorResponse(

        @Schema(
                description = "오류 발생 시각",
                example = "2026-08-29T10:30:00+09:00"
        )
        OffsetDateTime timestamp,

        @Schema(
                description = "HTTP 상태 코드",
                example = "404"
        )
        int status,

        @Schema(
                description = "애플리케이션 오류 코드",
                example = "CATEGORY_NOT_FOUND"
        )
        String code,

        @Schema(
                description = "클라이언트에 제공되는 오류 메시지",
                example = "카테고리가 존재하지 않습니다."
        )
        String message,

        @Schema(
                description = "오류가 발생한 요청 경로",
                example = "/api/v1/categories/999"
        )
        String path,

        @Schema(description = "필드 단위 검증 오류 항목")
        List<FieldErrorResponse> fieldErrors
) {

    public static ApiErrorResponse of(
            ErrorCode errorCode,
            String message,
            String path
    ) {
        return new ApiErrorResponse(
                OffsetDateTime.now(),
                errorCode.getStatus().value(),
                errorCode.getCode(),
                message,
                path,
                List.of()
        );
    }

    public static ApiErrorResponse validation(
            ErrorCode errorCode,
            String path,
            List<FieldErrorResponse> fieldErrors
    ) {
        return new ApiErrorResponse(
                OffsetDateTime.now(),
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                path,
                List.copyOf(fieldErrors)
        );
    }
}
