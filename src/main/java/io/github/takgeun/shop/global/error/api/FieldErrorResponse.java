package io.github.takgeun.shop.global.error.api;

/**
 * Bean Validation 필드별 오류
 */
public record FieldErrorResponse(
        String field,
        String reason
) {
}
