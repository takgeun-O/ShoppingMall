package io.github.takgeun.shop.global.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 잘못된 요청(검증 실패/규칙 위반 등)
     * 현재 서비스에서 IllegalArgumentException을 많이 쓰고 있으니 400으로 통일
     */
    // 서비스 컨트롤러 호출 결과 IllegalArgumentException 발생
    // ExceptionResolver 작동 --> 가장 우선순위가 높은 ExceptionHandlerExceptionResolver 실행
    // ExceptionHandlerExceptionResolver 가 예외처리 컨트롤러에 IllegalArgumentException을 처리할 수 있는 @ExceptionHandler가 있는지 확인
    // handleIllegalArgument() 실행
    // @RestController이므로 handleIllegalArgument()에도 @ResponseBody가 적용된다. 따라서 HTTP컨버터가 사용되고 응답이 JSON으로 반환된다.
    // @ResponseStatus(HttpStatus.BAD_REQUEST)를 사용하는 방법도 있으나, 이러한 방식은 HTTP 제어가 static하다. (상태코드가 컴파일 시점에 고정되므로 추후 조건에 따른 변경이 어려움)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse body = ApiErrorResponse.of(
                "BAD_REQUEST",
                e.getMessage(),
                status.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlerNotFound(NotFoundException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;

        ApiErrorResponse body = ApiErrorResponse.of(
                "NOT_FOUND",
                e.getMessage(),
                status.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(body);
    }

    /**
     * 그 외 예상 못한 예외 (서버 오류)
     * 운영에서는 message를 고정하는 게 보안상 더 안전함
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ApiErrorResponse body = ApiErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "서버 오류가 발생했습니다.",
                status.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(body);
    }
}
