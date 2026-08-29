package io.github.takgeun.shop.global.error.api;

import io.github.takgeun.shop.global.error.code.ErrorCode;
import io.github.takgeun.shop.global.error.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * 예외를 JSON 응답으로 변환
 */
// 서비스 컨트롤러 호출 결과 IllegalArgumentException 발생
// ExceptionResolver 작동 --> 가장 우선순위가 높은 ExceptionHandlerExceptionResolver 실행
// ExceptionHandlerExceptionResolver 가 예외처리 컨트롤러에 IllegalArgumentException을 처리할 수 있는 @ExceptionHandler가 있는지 확인
// handleIllegalArgument() 실행
// @RestControllerAdvice는 @ControllerAdvice와 @ResponseBody를 결합한 애노테이션임
    // 따라서 @ExceptionHandler의 반환 객체는 HTTP Message Converter를 통해 JSON으로 변환된다.
// @ResponseStatus(HttpStatus.BAD_REQUEST)를 사용하는 방법도 있으나, 이러한 방식은 HTTP 제어가 static하다. (상태코드가 컴파일 시점에 고정되므로 추후 조건에 따른 변경이 어려움)
@Slf4j
@RestControllerAdvice(
        annotations = RestController.class  // @RestController가 붙은 컨트롤러만 대상
)
public class ApiGlobalExceptionHandler {

    /**
     * 서비스, 도메인에서 발생한 비즈니스 예외
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(
            BusinessException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn(
                "Business exception: code={}, method={}, path={}, message={}",
                errorCode.getCode(),
                request.getMethod(),
                request.getRequestURI(),
                e.getMessage()
        );

        ApiErrorResponse response = ApiErrorResponse.of(
                errorCode,
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    /**
     * @RequestBody DTO의 Bean Validation 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        List<FieldErrorResponse> fieldErrors = e.getBindingResult()
                .getFieldErrors().stream()
                .map(this::toFieldErrorResponse)
                .toList();

        ApiErrorResponse response = ApiErrorResponse.validation(
                errorCode,
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    /**
     * @PathVariable, @RequestParam의 제약조건 실패
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        List<FieldErrorResponse> fieldErrors = e.getConstraintViolations().stream()
                .map(this::toFieldErrorResponse)
                .toList();

        ApiErrorResponse response = ApiErrorResponse.validation(
                errorCode,
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    /**
     * 요청값 타입 변환 실패
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.TYPE_MISMATCH;

        log.warn(
                "요청값 타입 변환 실패: parameter={}, value={}, requiredType={}, path={}",
                e.getName(),
                e.getValue(),
                e.getRequiredType() != null
                        ? e.getRequiredType().getSimpleName()
                        : null,
                request.getRequestURI()
        );

        ApiErrorResponse response = ApiErrorResponse.of(
                errorCode,
                errorCode.getDefaultMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    /**
     * JSON 문법 오류 또는 역직렬화 실패
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.MALFORMED_JSON;
        Throwable cause = e.getMostSpecificCause();

        log.warn(
                "JSON 읽기 실패: cause={}, method={}, path={}",
                cause.getClass().getSimpleName(),
                request.getMethod(),
                request.getRequestURI()
        );

        ApiErrorResponse response = ApiErrorResponse.of(
                errorCode,
                errorCode.getDefaultMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    /**
     * 지원하지 않는 Content-Type
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.UNSUPPORTED_MEDIA_TYPE;

        ApiErrorResponse response = ApiErrorResponse.of(
                errorCode,
                errorCode.getDefaultMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    private FieldErrorResponse toFieldErrorResponse(FieldError error) {
        return new FieldErrorResponse(
                error.getField(),
                error.getDefaultMessage()
        );
    }

    /**
     * 기존 도메인 코드에서 발생하는 잘못된 인자 예외
     *
     * TODO: 클라이언트 입력 또는 비즈니스 규칙 위반에 해당하는 예외는 BusinessException 계열로 점진적으로 전환한다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        log.warn(
                "잘못된 인자: method={}, path={}, message={}",
                request.getMethod(),
                request.getRequestURI(),
                e.getMessage()
        );

        ApiErrorResponse response = ApiErrorResponse.of(
                errorCode,
                errorCode.getDefaultMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    /**
     * 컨트롤러 파라미터 검증 실패
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidation(
            HandlerMethodValidationException e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        ApiErrorResponse response = ApiErrorResponse.of(
                errorCode,
                errorCode.getDefaultMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    /**
     * 예상하지 못한 서버 내부 오류
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception e,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        log.error(
                "예상하지 못한 서버 오류: method={}, path={}",
                request.getMethod(),
                request.getRequestURI(),
                e           // 스택 트레이스 전체가 로그에 기록되게끔 (클라이언트에만 반환하지 않게 주의!)
        );

        ApiErrorResponse response = ApiErrorResponse.of(
                errorCode,
                errorCode.getDefaultMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    private FieldErrorResponse toFieldErrorResponse(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath().toString();

        return new FieldErrorResponse(
                field,
                violation.getMessage()
        );
    }
}
