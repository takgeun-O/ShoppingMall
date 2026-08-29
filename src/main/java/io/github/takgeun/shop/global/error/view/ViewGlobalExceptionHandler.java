package io.github.takgeun.shop.global.error.view;

import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.ForbiddenException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.global.error.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * 예외를 HTML 오류 화면으로 변환
 */
@Slf4j
@ControllerAdvice(
        basePackages = "io.github.takgeun.shop",
        annotations = org.springframework.stereotype.Controller.class
)
public class ViewGlobalExceptionHandler {

    // 공통: 상태코드 + 뷰 + 메시지 세팅
    private ModelAndView render(HttpStatus status, String viewName, String message, HttpServletRequest request, Exception e) {

        log.error("render viewName={}", viewName);
        log.error("Unhandled exception, status={}, path={}", status.value(), request.getRequestURI(), e);

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

        return render(status, "error/400", message, request, e);
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

        return render(status, "error/400", message, request, e);
    }

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView handleNotFound(NotFoundException e, HttpServletRequest request) {
        return render(HttpStatus.NOT_FOUND, "error/404", e.getMessage(), request, e);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ModelAndView handleBadRequest(RuntimeException e, HttpServletRequest request) {
        return render(HttpStatus.BAD_REQUEST, "error/400", e.getMessage(), request, e);
    }

    @ExceptionHandler(ConflictException.class)
    public ModelAndView handleConflict(ConflictException e, HttpServletRequest request) {
        return render(HttpStatus.CONFLICT, "error/409", e.getMessage(), request, e);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ModelAndView handleUnauthorized(UnauthorizedException e, HttpServletRequest request) {
        return render(HttpStatus.UNAUTHORIZED, "error/401", e.getMessage(), request, e);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ModelAndView handleForbidden(ForbiddenException e, HttpServletRequest request) {
        return render(HttpStatus.FORBIDDEN, "error/403", e.getMessage(), request, e);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ModelAndView handleNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return render(HttpStatus.BAD_REQUEST, "error/400", "요청 본문이 올바르지 않습니다.", request, e);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ModelAndView handleMediaType(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        return render(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "error/415", "지원하지 않는 Content-Type 입니다.", request, e);
    }

    /**
     * 그 외 예상 못한 예외 (서버 오류)
     * 운영에서는 message를 고정하는 게 보안상 더 안전함
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception e, HttpServletRequest request) {
        // 예외 처리 중 발생한 예외는 스프링 기본 에러 처리에 맡겨서 무한 루프를 끊을 것.

        // 스프링에서 에러 처리 시도 -> /error 포워드 -> /error 처리 과정에서 또 예외 발생
        // --> ViewGlobalExceptionHandler의 @ExceptionHandler(Exception.class)로 에러 처리 중 발생한 예외까지 다시 잡아서 error/500 렌더링
        // --> 그 렌더링도 실패 --> 다시 /error ... 무한 재귀에 빠져서 StackOverflowError 가 터짐.
        if("/error".equals(request.getRequestURI())) {
            // /error 처리 중이면 여기서 또 error/500 렌더링 시도하지 말고 그대로 던져서 StackOverflowError 방지
            throw new RuntimeException(e);
        }

        return render(HttpStatus.INTERNAL_SERVER_ERROR, "error/500", "서버 오류가 발생했습니다.", request, e);
    }
}
