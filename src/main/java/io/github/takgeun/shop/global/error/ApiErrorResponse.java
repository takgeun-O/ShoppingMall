package io.github.takgeun.shop.global.error;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApiErrorResponse {

    private final String code;      // BAD_REQUEST, NOT_FOUND ...
    private final String message;   // 사용자에게 보여줄 메세지
    private final int status;       // HTTP status code
    private final String path;      // 요청 경로
    private final LocalDateTime timestamp;

    // 생성자는 private 으로 막음
    private ApiErrorResponse(String code, String message, int status, String path) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }

    public static ApiErrorResponse of(String code, String message, int status, String path) {
        return new ApiErrorResponse(code, message, status, path);
    }
}
