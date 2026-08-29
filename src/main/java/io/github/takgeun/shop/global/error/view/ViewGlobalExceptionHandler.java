package io.github.takgeun.shop.global.error.view;

import io.github.takgeun.shop.global.error.exception.BusinessException;
import io.github.takgeun.shop.global.view.ViewController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

/**
 * 예외를 HTML 오류 화면으로 변환
 */
@Slf4j
@ControllerAdvice(annotations = ViewController.class)
public class ViewGlobalExceptionHandler {

    private ModelAndView render(HttpStatus status, String viewName, String message, HttpServletRequest request) {
        ModelAndView mv = new ModelAndView(viewName);
        mv.setStatus(status);

        mv.addObject("status", status.value());
        mv.addObject("error", status.getReasonPhrase());
        mv.addObject("message", message);
        mv.addObject("path", request.getRequestURI());

        return mv;
    }

    // 1) DTO Validation 실패 (@Valid @ModelAttribute)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ModelAndView handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // 메시지 정책: 필드 에러를 "field: message" 형태로 한 줄
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("요청 값이 올바르지 않습니다.");

        logClientError(e, status, request);
        return render(status, "error/400", message, request);
    }

    // 2) 파라미터 Validation 실패 (@RequestParam @PathVariable) 그러니까 숫자 타입에 문자가 들어온다던지 등등
    @ExceptionHandler(ConstraintViolationException.class)
    public ModelAndView handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // getByCategory.categoryId: categoryId는 필수입니다.
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .findFirst()
                .orElse("요청 값이 올바르지 않습니다.");

        logClientError(e, status, request);
        return render(status, "error/400", message, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ModelAndView handleMethodValidation(HandlerMethodValidationException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        logClientError(e, status, request);
        return render(status, "error/400", "요청 값이 올바르지 않습니다.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        logClientError(e, status, request);
        return render(status, "error/400", "요청 값의 형식이 올바르지 않습니다.", request);
    }

    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusinessException(BusinessException e, HttpServletRequest request) {
        HttpStatus status = e.getErrorCode().getStatus();
        String message = status.is5xxServerError()
                ? "서버 오류가 발생했습니다."
                : e.getMessage();

        logClientError(e, status, request);
        return render(status, resolveView(status), message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        logClientError(e, status, request);
        return render(status, "error/400", e.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ModelAndView handleNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        logClientError(e, status, request);
        return render(status, "error/400", "요청 본문이 올바르지 않습니다.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ModelAndView handleMediaType(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        logClientError(e, status, request);
        return render(status, "error/400", "지원하지 않는 Content-Type 입니다.", request);
    }

    /**
     * 그 외 예상 못한 예외 (서버 오류)
     * 운영에서는 message를 고정하는 게 보안상 더 안전함
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error(
                "Unexpected view exception: method={}, path={}",
                request.getMethod(),
                request.getRequestURI(),
                e
        );
        return render(status, "error/500", "서버 오류가 발생했습니다.", request);
    }

    private String resolveView(HttpStatus status) {
        return switch (status.value()) {
            case 400 -> "error/400";
            case 401 -> "error/401";
            case 403 -> "error/403";
            case 404 -> "error/404";
            case 409 -> "error/409";
            case 415 -> "error/400";
            default -> status.is4xxClientError() ? "error/400" : "error/500";
        };
    }

    private void logClientError(Exception e, HttpStatus status, HttpServletRequest request) {
        log.warn(
                "View request rejected: status={}, exception={}, method={}, path={}, message={}",
                status.value(),
                e.getClass().getSimpleName(),
                request.getMethod(),
                request.getRequestURI(),
                e.getMessage()
        );
    }
}
