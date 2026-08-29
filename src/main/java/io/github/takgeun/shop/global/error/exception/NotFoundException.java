package io.github.takgeun.shop.global.error.exception;

import io.github.takgeun.shop.global.error.code.ErrorCode;

public class NotFoundException extends BusinessException {


    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    // 기존 SSR 방식으로 구현한 부분 호환용
    public NotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public NotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
