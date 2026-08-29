package io.github.takgeun.shop.global.error.exception;

import io.github.takgeun.shop.global.error.code.ErrorCode;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(ErrorCode.AUTHENTICATION_REQUIRED, message);
    }

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
