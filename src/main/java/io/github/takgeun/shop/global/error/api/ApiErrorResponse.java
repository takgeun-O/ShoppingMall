package io.github.takgeun.shop.global.error.api;

import io.github.takgeun.shop.global.error.code.ErrorCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 모든 API 오류의 공통 JSON 형식
 */
public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
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
