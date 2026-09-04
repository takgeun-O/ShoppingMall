package io.github.takgeun.shop.global.error.api;

import io.github.takgeun.shop.global.api.ApiController;
import io.github.takgeun.shop.global.error.code.ErrorCode;
import io.github.takgeun.shop.global.error.exception.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.*;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link ApiGlobalExceptionHandler} 단위 테스트.
 *
 * Spring ApplicationContext와 SecurityFilterChain을 구성하지 않고
 * standalone MockMvc를 사용해 API 예외 처리와 JSON 응답 계약만 검증한다.
 *
 * Spring Security 필터에서 발생하는 인증·인가 예외는
 * AuthenticationMigrationIntegrationTest에서 별도로 검증한다.
 */
class ApiGlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        /*
         * @Valid와 Bean Validation이 standalone MockMvc에서도
         * 작동하도록 Validator를 직접 등록한다.
         */
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new ApiGlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void 비즈니스_예외는_ErrorCode에_정의된_응답을_반환한다() throws Exception {

        /*
         * BusinessException의 ErrorCode를 기준으로
         * HTTP 상태와 표준 오류 응답이 생성되는지 검증한다.
         */
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code")
                        .value("CATEGORY_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("카테고리가 존재하지 않습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/test/business"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void RequestBody_검증_실패_시_모든_필드_오류를_반환한다() throws Exception {

        /*
         * JSON 역직렬화
         * → TestRequest 생성
         * → @Valid 검증
         * → MethodArgumentNotValidException 발생
         * → ApiGlobalExceptionHandler가 400 응답 생성
         */
        String requestBody = """
                {
                    "name": "",
                    "price": 0
                }
                """;

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message")
                        .value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/test/validation"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
                .andExpect(jsonPath("$.fieldErrors[*].field",
                        containsInAnyOrder("name", "price")
                ));
    }

    @Test
    void PathVariable_타입_변환_실패_시_400을_반환한다() throws Exception {

        mockMvc.perform(get("/test/type/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message")
                        .value("요청 값의 형식이 올바르지 않습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/test/type/not-a-number"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void 잘못된_JSON을_전달하면_400을_반환한다() throws Exception {

        String malformedJson = """
                {
                    "name":
                """;

        /*
         * 잘못된 JSON은 DTO로 역직렬화되지 못하므로
         * HttpMessageNotReadableException이 발생한다.
         */
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.message")
                        .value("요청 본문을 읽을 수 없습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/test/validation"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());    // JSON 문법 오류는 특정 DTO 필드의 Validation 실패가 아니기 때문에 비어있음.
    }

    @Test
    void 지원하지_않는_ContentType이면_415를_반환한다() throws Exception {

        /*
         * JSON을 요구하는 엔드포인트에 text/plain을 보내면
         * HttpMediaTypeNotSupportedException이 발생한다.
         */
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.code")
                        .value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message")
                        .value("지원하지 않는 Content-Type입니다."))
                .andExpect(jsonPath("$.path")
                        .value("/test/validation"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void IllegalArgumentException은_400을_반환한다() throws Exception {

        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message")
                        .value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/test/illegal-argument"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void 예상하지_못한_예외는_내부_내용을_노출하지_않고_500을_반환한다() throws Exception {

        /**
         * IllegalStateException 전용 처리기 있나? -> 없음
         * RuntimeException 처리기 있나? -> 없음
         * Exception 처리기 있나? -> 있음
         * 따라서 ApiGlobalExceptionHandler에서 handleUnexpectedException 실행
         */
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code")
                        .value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("서버 내부 오류가 발생했습니다."))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.not(
                                "DB 접속 비밀번호가 노출될 수 있는 내부 메시지"
                        )))
                .andExpect(jsonPath("$.path")
                        .value("/test/unexpected"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @ApiController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/business")
        void businessException() {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        @PostMapping(value = "/validation")
        public void validation(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/type/{id}")
        public void typeMismatch(@PathVariable Long id) {}

        @GetMapping("/illegal-argument")
        public void illegalArgument() {
            throw new IllegalArgumentException("클라이언트에 그대로 노출하지 않을 메시지");
        }

        @GetMapping("/unexpected")
        public void unexpectedException() {
            throw new IllegalStateException("DB 접속 비밀번호가 노출될 수 있는 내부 메시지");
        }

    }

    record TestRequest(
            @NotBlank(message = "이름은 필수입니다.") String name,
            @Positive(message = "가격은 양수여야 합니다.") int price
    ) {
    }
}