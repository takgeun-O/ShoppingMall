package io.github.takgeun.shop.global.error.exception;

import io.github.takgeun.shop.global.error.code.ErrorCode;
import lombok.Getter;

/**
 * 모든 비즈니스 예외의 부모
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

}
