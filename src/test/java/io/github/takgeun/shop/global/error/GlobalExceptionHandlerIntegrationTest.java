package io.github.takgeun.shop.global.error;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.global.error.api.ApiGlobalExceptionHandler;
import io.github.takgeun.shop.global.error.code.ErrorCode;
import io.github.takgeun.shop.global.error.exception.BusinessException;
import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.global.error.view.ViewGlobalExceptionHandler;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.global.view.GlobalViewModelAdvice;
import io.github.takgeun.shop.global.view.ViewController;
import io.github.takgeun.shop.member.domain.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * DataSource와 ApplicationContext 없이 실제 ControllerAdvice 선택 경계를 검증한다.
 */
class GlobalExceptionHandlerIntegrationTest {

    /**
     * DB와 전체 ApplicationContext를 생성하지 않고
     * API/View Controller와 각 예외 처리기의 경계를 검증한다.
     */

    private MockMvc mockMvc;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = mock(CategoryService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestApiController(), new TestViewController())
                .setControllerAdvice(
                        new ApiGlobalExceptionHandler(),
                        new ViewGlobalExceptionHandler(),
                        new GlobalViewModelAdvice(categoryService)
                )
                .build();
    }

    @Test
    void REST_BusinessException은_JSON으로_반환하고_View_Model_Advice를_실행하지_않는다() throws Exception {
        mockMvc.perform(get("/test/global-exception/api/not-found")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(handler().handlerType(TestApiController.class))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("카테고리가 존재하지 않습니다."))
                .andExpect(jsonPath("$.path").value("/test/global-exception/api/not-found"));

        verifyNoInteractions(categoryService);
    }

    @Test
    void View_NotFoundException은_HTML_오류_View와_Model_계약을_유지한다() throws Exception {
        mockMvc.perform(get("/test/global-exception/view/not-found")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(handler().handlerType(TestViewController.class))
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("status", 404))
                .andExpect(model().attribute("error", "Not Found"))
                .andExpect(model().attribute("message", "카테고리가 존재하지 않습니다."))
                .andExpect(model().attribute("path", "/test/global-exception/view/not-found"));
    }

    @Test
    void View의_직접_BusinessException도_ErrorCode_상태로_처리한다() throws Exception {
        mockMvc.perform(get("/test/global-exception/view/business"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"))
                .andExpect(model().attribute("status", 400))
                .andExpect(model().attribute("error", "Bad Request"))
                .andExpect(model().attribute("message", "유효하지 않은 상위 카테고리 설정입니다."))
                .andExpect(model().attribute("path", "/test/global-exception/view/business"));
    }

    @Test
    void View_IllegalStateException은_내부_메시지를_노출하지_않고_500으로_처리한다() throws Exception {
        mockMvc.perform(get("/test/global-exception/view/illegal-state"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/500"))
                .andExpect(model().attribute("status", 500))
                .andExpect(model().attribute("error", "Internal Server Error"))
                .andExpect(model().attribute("message", "서버 오류가 발생했습니다."))
                .andExpect(model().attribute("message", not("노출되면 안 되는 내부 메시지")))
                .andExpect(model().attribute("path", "/test/global-exception/view/illegal-state"));
    }

    @Test
    void View_ConflictException은_409_View로_처리한다() throws Exception {
        mockMvc.perform(get("/test/global-exception/view/conflict"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error/409"))
                .andExpect(model().attribute("status", 409))
                .andExpect(model().attribute("error", "Conflict"))
                .andExpect(model().attribute("message", "이미 처리된 요청입니다."))
                .andExpect(model().attribute("path", "/test/global-exception/view/conflict"));
    }

    @Test
    void View의_지원하지_않는_ContentType은_415와_error400_View를_사용한다() throws Exception {
        mockMvc.perform(post("/test/global-exception/view/content-type")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(view().name("error/400"))
                .andExpect(model().attribute("status", 415))
                .andExpect(model().attribute("error", "Unsupported Media Type"))
                .andExpect(model().attribute("message", "지원하지 않는 Content-Type 입니다."))
                .andExpect(model().attribute("path", "/test/global-exception/view/content-type"));
    }

    @Test
    void View_Controller에는_필요한_전역_Model을_공급한다() throws Exception {
        when(categoryService.getTopCategories()).thenReturn(List.of());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, 1L);
        session.setAttribute(SessionConst.LOGIN_MEMBER_NAME, "관리자");
        session.setAttribute(SessionConst.LOGIN_ROLE, MemberRole.ADMIN);

        mockMvc.perform(get("/test/global-exception/view/model").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("test/view"))
                .andExpect(model().attribute("loginMemberId", 1L))
                .andExpect(model().attribute("loginMemberName", "관리자"))
                .andExpect(model().attribute("loginRole", MemberRole.ADMIN))
                .andExpect(model().attribute("isAdmin", true))
                .andExpect(model().attribute("treeMode", "admin"))
                .andExpect(model().attribute("rootCategories", List.of()));

        verify(categoryService).getTopCategories();
        verifyNoMoreInteractions(categoryService);
    }

    @Test
    void 오류_View_템플릿은_공통_layout과_함께_렌더링된다() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();
        JakartaServletWebApplication webApplication =
                JakartaServletWebApplication.buildApplication(servletContext);
        WebContext context = new WebContext(webApplication.buildExchange(request, response));
        context.setVariable("isAdmin", false);
        context.setVariable("rootCategories", List.of());

        for (String template : List.of(
                "error/400",
                "error/401",
                "error/403",
                "error/404",
                "error/409",
                "error/500"
        )) {
            String rendered = templateEngine.process(template, context);
            assertFalse(rendered.isBlank(), template + " 렌더링 결과가 비어 있습니다.");
        }
    }

    @RestController
    @RequestMapping("/test/global-exception/api")
    static class TestApiController {

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    @ViewController
    @RequestMapping("/test/global-exception/view")
    static class TestViewController {

        @GetMapping("/not-found")
        String notFound() {
            throw new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        @GetMapping("/business")
        String business() {
            throw new BusinessException(ErrorCode.INVALID_CATEGORY_PARENT);
        }

        @GetMapping("/illegal-state")
        String illegalState() {
            throw new IllegalStateException("노출되면 안 되는 내부 메시지");
        }

        @GetMapping("/conflict")
        String conflict() {
            throw new ConflictException(ErrorCode.RESOURCE_CONFLICT, "이미 처리된 요청입니다.");
        }

        @PostMapping("/content-type")
        String contentType(@RequestBody TestRequest request) {
            return "test/view";
        }

        @GetMapping("/model")
        String model() {
            return "test/view";
        }
    }

    record TestRequest(String value) {
    }
}
