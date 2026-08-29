package io.github.takgeun.shop.global.error.exception;

import io.github.takgeun.shop.global.error.code.ErrorCode;

public class ConflictException extends BusinessException {

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    // 기존 SSR 방식으로 구현한 부분 호환용
    public ConflictException(String message) {
        super(ErrorCode.RESOURCE_CONFLICT, message);
    }
}
