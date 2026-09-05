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

    // 회원 비밀번호 변경 도메인 오류
    INVALID_CURRENT_PASSWORD(
            HttpStatus.BAD_REQUEST,
            "INVALID_CURRENT_PASSWORD",
            "현재 비밀번호가 올바르지 않습니다."
    ),
    PASSWORD_REUSE_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST,
            "PASSWORD_REUSE_NOT_ALLOWED",
            "새 비밀번호는 현재 비밀번호와 달라야 합니다."
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
    PARENT_CATEGORY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "PARENT_CATEGORY_NOT_FOUND",
            "상위 카테고리가 존재하지 않습니다."
    ),
    CATEGORY_NAME_DUPLICATED(
            HttpStatus.CONFLICT,
            "CATEGORY_NAME_DUPLICATED",
            "이미 존재하는 카테고리명입니다."
    ),
    CATEGORY_SLUG_DUPLICATED(
            HttpStatus.CONFLICT,
            "CATEGORY_SLUG_DUPLICATED",
            "이미 존재하는 카테고리 URL 식별자입니다."
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

    INVALID_CATEGORY_PARENT(
            HttpStatus.BAD_REQUEST,
            "INVALID_CATEGORY_PARENT",
            "유효하지 않은 상위 카테고리 설정입니다."
    ),
    CATEGORY_DEPTH_EXCEEDED(
            HttpStatus.BAD_REQUEST,
            "CATEGORY_DEPTH_EXCEEDED",
            "카테고리는 2단까지만 설정할 수 있습니다."
    ),
    CATEGORY_CIRCULAR_REFERENCE(
            HttpStatus.CONFLICT,
            "CATEGORY_CIRCULAR_REFERENCE",
            "순환 참조가 발생하는 카테고리 구조는 허용되지 않습니다."
    ),

    // PRODUCT
    PRODUCT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "PRODUCT_NOT_FOUND",
            "존재하지 않는 상품입니다."
    ),

    // 서버 오류
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
