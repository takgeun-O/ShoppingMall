package io.github.takgeun.shop.global.error.exception;

import io.github.takgeun.shop.global.error.code.ErrorCode;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(ErrorCode.ACCESS_DENIED, message);
    }

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
