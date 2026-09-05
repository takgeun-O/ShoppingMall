package io.github.takgeun.shop.global.error;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.global.api.ApiController;
import io.github.takgeun.shop.global.error.code.ErrorCode;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.global.view.ViewController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 실제 ApplicationContext에 등록된 API/View 예외 처리기가
 * Controller 종류에 따라 올바르게 분리되는지 검증한다.
 *
 * 이 테스트의 관심사는 Spring MVC 예외 처리이므로
 * MockMvc의 Security Filter는 적용하지 않는다.
 *
 * Spring Security에서 발생하는 인증·인가 예외는
 * AuthenticationMigrationIntegrationTest에서 검증한다.
 */
@ActiveProfiles({"test", "mybatis"})
/**
 * MockMvc 요청
 * → SecurityFilterChain 생략
 * → DispatcherServlet
 * → 테스트 Controller
 * → 예외 발생
 * → 실제 GlobalExceptionHandler
 * → 응답 검증
 */
@AutoConfigureMockMvc(addFilters = false)   // MockMvc 요청에서 Servlet Filter를 적용하지 않겠다
@Import({
        GlobalExceptionHandlerIntegrationTest.TestApiController.class,
        GlobalExceptionHandlerIntegrationTest.TestViewController.class
})
class GlobalExceptionHandlerIntegrationTest extends IntegrationTestSupport {

    @Test
    void REST_Controller의_BusinessException은_JSON으로_처리한다()
            throws Exception {

        mockMvc.perform(
                        get("/test/global-exception/api/not-found")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(handler().handlerType(
                        TestApiController.class
                ))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code")
                        .value("CATEGORY_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("카테고리가 존재하지 않습니다."))
                .andExpect(jsonPath("$.path")
                        .value(
                                "/test/global-exception/api/not-found"
                        ))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void View_Controller의_BusinessException은_HTML_오류화면으로_처리한다()
            throws Exception {

        mockMvc.perform(
                        get("/test/global-exception/view/not-found")
                                .accept(MediaType.TEXT_HTML)
                )
                .andExpect(status().isNotFound())
                .andExpect(handler().handlerType(
                        TestViewController.class
                ))
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("status", 404))
                .andExpect(model().attribute(
                        "error",
                        "Not Found"
                ))
                .andExpect(model().attribute(
                        "message",
                        "카테고리가 존재하지 않습니다."
                ))
                .andExpect(model().attribute(
                        "path",
                        "/test/global-exception/view/not-found"
                ));
    }

    @Test
    void REST_Controller의_예상하지_못한_예외는_JSON_500으로_처리한다()
            throws Exception {

        mockMvc.perform(
                        get("/test/global-exception/api/unexpected")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(handler().handlerType(
                        TestApiController.class
                ))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code")
                        .value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("서버 내부 오류가 발생했습니다."))
                .andExpect(jsonPath("$.message")
                        .value(not(
                                "노출되면 안 되는 내부 API 메시지"
                        )))
                .andExpect(jsonPath("$.path")
                        .value(
                                "/test/global-exception/api/unexpected"
                        ))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void View_Controller의_예상하지_못한_예외는_HTML_500으로_처리한다()
            throws Exception {

        mockMvc.perform(
                        get("/test/global-exception/view/unexpected")
                                .accept(MediaType.TEXT_HTML)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(handler().handlerType(
                        TestViewController.class
                ))
                .andExpect(view().name("error/500"))
                .andExpect(model().attribute(
                        "status",
                        500
                ))
                .andExpect(model().attribute(
                        "error",
                        "Internal Server Error"
                ))
                .andExpect(model().attribute(
                        "message",
                        "서버 오류가 발생했습니다."
                ))
                .andExpect(model().attribute(
                        "message",
                        not("노출되면 안 되는 내부 View 메시지")
                ))
                .andExpect(model().attribute(
                        "path",
                        "/test/global-exception/view/unexpected"
                ));
    }

    @ApiController
    @RequestMapping("/test/global-exception/api")
    static class TestApiController {

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException(
                    ErrorCode.CATEGORY_NOT_FOUND
            );
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException(
                    "노출되면 안 되는 내부 API 메시지"
            );
        }
    }

    /**
     * 테스트 ApplicationContext에만 등록되는 View Controller.
     *
     * 실제 View Controller와 동일하게 @ViewController를 사용한다.
     */
    @ViewController
    @RequestMapping("/test/global-exception/view")
    static class TestViewController {

        @GetMapping("/not-found")
        String notFound() {
            throw new NotFoundException(
                    ErrorCode.CATEGORY_NOT_FOUND
            );
        }

        @GetMapping("/unexpected")
        String unexpected() {
            throw new IllegalStateException(
                    "노출되면 안 되는 내부 View 메시지"
            );
        }
    }
}