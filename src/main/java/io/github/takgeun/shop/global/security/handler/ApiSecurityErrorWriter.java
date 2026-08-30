package io.github.takgeun.shop.global.security.handler;

import io.github.takgeun.shop.global.error.api.ApiErrorResponse;
import io.github.takgeun.shop.global.error.code.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * API JSON 처리기
 * API 처리기에서 중복되는 JSON 응답 생성 분리
 */
@Component
@RequiredArgsConstructor
public class ApiSecurityErrorWriter {

    // ObjectMapper : Jackson 라이브러리의 객체. Java 객체와 JSON 사이의 변환 담당
    private final ObjectMapper objectMapper;

    public void write(
            HttpServletResponse response,
            ErrorCode errorCode,
            String path
    ) throws IOException {

        ApiErrorResponse body = ApiErrorResponse.of(
                errorCode,
                errorCode.getDefaultMessage(),
                path
        );

        response.setStatus(errorCode.getStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        /**
         * Java 객체인 body를 JSON으로 변환해서 HTTP 응답 본문에 직접 작성
         *
         * body Java 객체 -> JSON 직렬화
         * -> JSON 데이터 -> OutputStream 에 기록
         * -> HTTP 응답 본문
         */
        objectMapper.writeValue(
                response.getOutputStream(), // getOutputStream() : 클라이언트에게 보낼 HTTP 응답 본문을 작성할 수 있는 출력 통로 반환
                body
        );
    }
}
