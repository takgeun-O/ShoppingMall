package io.github.takgeun.shop.global.error.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * HTTP 상태, 외부 공개 코드, 기본 메시지 정의
 */

@Getter
public enum ErrorCode {

    // 공통 요청 오류
    INVALID_INPUT(
            HttpStatus.BAD_REQUEST,
            "INVALID_INPUT",
            "요청 값이 올바르지 않습니다."
    ),
    MALFORMED_JSON(
            HttpStatus.BAD_REQUEST,
            "MALFORMED_JSON",
            "요청 본문을 읽을 수 없습니다."
    ),
    TYPE_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "TYPE_MISMATCH",
            "요청 값의 형식이 올바르지 않습니다."
    ),
    UNSUPPORTED_MEDIA_TYPE(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "UNSUPPORTED_MEDIA_TYPE",
            "지원하지 않는 Content-Type입니다."
    ),

    // 인증·인가
    AUTHENTICATION_REQUIRED(
            HttpStatus.UNAUTHORIZED,
            "AUTHENTICATION_REQUIRED",
            "로그인이 필요합니다."
    ),
    ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "ACCESS_DENIED",
            "접근 권한이 없습니다."
    ),

    // 기존 예외 호환용
    RESOURCE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "요청한 리소스를 찾을 수 없습니다."
    ),
    RESOURCE_CONFLICT(
            HttpStatus.CONFLICT,
            "RESOURCE_CONFLICT",
            "요청이 현재 리소스 상태와 충돌합니다."
    ),

    // Category
    CATEGORY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "CATEGORY_NOT_FOUND",
            "카테고리가 존재하지 않습니다."
    ),
    CATEGORY_NAME_DUPLICATED(
            HttpStatus.CONFLICT,
            "CATEGORY_NAME_DUPLICATED",
            "이미 존재하는 카테고리명입니다."
    ),
    CATEGORY_HAS_CHILDREN(
            HttpStatus.CONFLICT,
            "CATEGORY_HAS_CHILDREN",
            "하위 카테고리가 존재하여 삭제할 수 없습니다."
    ),
    CATEGORY_HAS_PRODUCTS(
            HttpStatus.CONFLICT,
            "CATEGORY_HAS_PRODUCTS",
            "상품이 연결된 카테고리는 삭제할 수 없습니다."
    ),

    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "서버 내부 오류가 발생했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String code, String defaultMessage) {
        this.status = status;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
