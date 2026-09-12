package io.github.takgeun.shop.category.api.admin;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryStatus;
import io.github.takgeun.shop.global.error.api.ApiGlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AdminCategoryApiControllerTest {

    private CategoryService categoryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        /**
         * 가짜 CategoryService 생성
         * → 실제 AdminCategoryApiController 생성
         * → Bean Validation 설정
         * → API 예외 처리기 연결
         * → MockMvc 생성
         */
        // 가짜 CategoryService 객체를 만든다.
        // 단위테스트에서는 Controller가 Service를 올바르게 호출하는지만 확인하면 되고, 그 외 의존성은 사용할 필요가 없음.
        categoryService = mock(CategoryService.class);

        AdminCategoryApiController controller =
                new AdminCategoryApiController(categoryService);

        // DTO 요청 검증 처리용
        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();

        validator.afterPropertiesSet(); // Validator 초기화 (Spring Application에서는 Spring이 생명주기 메서드를 자동 호출)

        // 전체 Spring Boot 애플리케이션을 실행하지 않고 지정한 Controller만으로 테스트용 Spring MVC 환경을 만든다.
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(   // 예외 처리기 연결
                        new ApiGlobalExceptionHandler()
                )
                .setValidator(validator)
                .build();
    }

    @Test
    void 카테고리_생성에_성공하면_201을_반환한다()
            throws Exception {

        // given
        Long categoryId = 2L;

        Category category = category(
                categoryId,
                "노트북",
                "notebook",
                1L,
                CategoryStatus.ACTIVE
        );

        when(categoryService.create(
                "노트북",
                1L
        )).thenReturn(categoryId);

        when(categoryService.getAdmin(categoryId))
                .thenReturn(category);

        // when & then
        mockMvc.perform(
                        post("/api/v1/admin/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "노트북",
                                          "parentId": 1
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/admin/categories/2"
                ))
                .andExpect(content()
                        .contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        ))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name")
                        .value("노트북"))
                .andExpect(jsonPath("$.slug")
                        .value("notebook"))
                .andExpect(jsonPath("$.parentId")
                        .value(1))
                .andExpect(jsonPath("$.status")
                        .value("ACTIVE"));

        verify(categoryService).create(
                "노트북",
                1L
        );

        verify(categoryService).getAdmin(categoryId);
    }

    @Test
    void 최상위_카테고리는_parentId없이_생성할_수_있다()
            throws Exception {

        // given
        Long categoryId = 1L;

        Category category = category(
                categoryId,
                "전자",
                "electronics",
                null,
                CategoryStatus.ACTIVE
        );

        when(categoryService.create(
                "전자",
                null
        )).thenReturn(categoryId);

        when(categoryService.getAdmin(categoryId))
                .thenReturn(category);

        // when & then
        mockMvc.perform(
                        post("/api/v1/admin/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "전자",
                                          "parentId": null
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.parentId")
                        .doesNotExist());

        verify(categoryService).create(
                "전자",
                null
        );
    }

    @Test
    void 카테고리명이_공백이면_400을_반환한다()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/admin/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": " ",
                                          "parentId": null
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors")
                        .isArray())
                .andExpect(jsonPath(
                        "$.fieldErrors[*].field"
                ).value(hasItem("name")));

        verifyNoInteractions(categoryService);
    }

    @Test
    void 상위_카테고리_ID가_양수가_아니면_400을_반환한다()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/admin/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "노트북",
                                          "parentId": 0
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath(
                        "$.fieldErrors[*].field"
                ).value(hasItem("parentId")));

        verifyNoInteractions(categoryService);
    }

    @Test
    void 카테고리_수정에_성공하면_수정된_카테고리를_반환한다()
            throws Exception {

        // given
        Long categoryId = 2L;

        Category updatedCategory = category(
                categoryId,
                "게이밍 노트북",
                "gaming-notebook",
                1L,
                CategoryStatus.ACTIVE
        );

        when(categoryService.getAdmin(categoryId))
                .thenReturn(updatedCategory);

        // when & then
        mockMvc.perform(
                        put(
                                "/api/v1/admin/categories/{categoryId}",
                                categoryId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "게이밍 노트북",
                                          "parentId": 1
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name")
                        .value("게이밍 노트북"))
                .andExpect(jsonPath("$.slug")
                        .value("gaming-notebook"))
                .andExpect(jsonPath("$.parentId")
                        .value(1))
                .andExpect(jsonPath("$.status")
                        .value("ACTIVE"));

        verify(categoryService).update(
                categoryId,
                "게이밍 노트북",
                1L
        );

        verify(categoryService).getAdmin(categoryId);
    }

    @Test
    void 카테고리_상태_변경에_성공한다()
            throws Exception {

        // given
        Long categoryId = 2L;

        Category updatedCategory = category(
                categoryId,
                "노트북",
                "notebook",
                1L,
                CategoryStatus.INACTIVE
        );

        when(categoryService.getAdmin(categoryId))
                .thenReturn(updatedCategory);

        // when & then
        mockMvc.perform(
                        patch(
                                "/api/v1/admin/categories/{categoryId}/status",
                                categoryId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "INACTIVE"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.status")
                        .value("INACTIVE"));

        verify(categoryService).changeStatus(
                categoryId,
                CategoryStatus.INACTIVE
        );

        verify(categoryService).getAdmin(categoryId);
    }

    @Test
    void 카테고리_상태가_null이면_400을_반환한다()
            throws Exception {

        mockMvc.perform(
                        patch(
                                "/api/v1/admin/categories/{categoryId}/status",
                                2L
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": null
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath(
                        "$.fieldErrors[*].field"
                ).value(hasItem("status")));

        verifyNoInteractions(categoryService);
    }

    @Test
    void 지원하지_않는_카테고리_상태이면_400을_반환한다()
            throws Exception {

        /**
         * PATCH /api/v1/admin/categories/2/status
         * Content-Type: application/json
         *         ↓
         * DispatcherServlet이 Controller 메서드 탐색
         *         ↓
         * @RequestBody 처리를 위해 Jackson 실행
         *         ↓
         * JSON의 "UNKNOWN"을 CategoryStatus로 변환 시도
         *         ↓
         * CategoryStatus에 UNKNOWN 상수가 없음
         *         ↓
         * InvalidFormatException 발생
         *         ↓
         * HttpMessageNotReadableException으로 감싸짐
         *         ↓
         * ApiGlobalExceptionHandler가 처리
         *         ↓
         * 400 Bad Request + MALFORMED_JSON 반환
         */
        mockMvc.perform(
                        patch(
                                "/api/v1/admin/categories/{categoryId}/status",
                                2L
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "UNKNOWN"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("MALFORMED_JSON"));

        verifyNoInteractions(categoryService);
    }

    @Test
    void 잘못된_JSON이면_400을_반환한다()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/admin/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name":
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("MALFORMED_JSON"));

        verifyNoInteractions(categoryService);
    }

    @Test
    void 지원하지_않는_ContentType이면_415를_반환한다()
            throws Exception {

        /**
         * POST /api/v1/admin/categories
         * Content-Type: text/plain
         *         ↓
         * 경로와 HTTP 메서드로 Controller 선택
         *         ↓
         * Handler = AdminCategoryApiController.create()
         *         ↓
         * @RequestBody 변환 시도
         *         ↓
         * text/plain을 CreateCategoryRequest로 변환할
         * HttpMessageConverter가 없음 --> create()메서드의 매개변수 타입이 String이 아닌 CreateCategoryRequest타입이기 때문
         *                               StringHttpMessageConverter는 문자열을 읽을 수 있지만, 그 문자열을 임의의 DTO로 조립하는 책임까지 갖고 있지 않음.
         *         ↓
         * HttpMediaTypeNotSupportedException 발생
         *         ↓
         * 선택된 Handler에 @ApiController가 있는지 확인 가능
         *         ↓
         * ApiGlobalExceptionHandler 적용
         *         ↓
         * 415 + UNSUPPORTED_MEDIA_TYPE JSON 반환
         */
        mockMvc.perform(
                        post("/api/v1/admin/categories")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("category")
                )
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.code")
                        .value("UNSUPPORTED_MEDIA_TYPE"));

        verifyNoInteractions(categoryService);
    }



    private Category category(
            Long id,
            String name,
            String slug,
            Long parentId,
            CategoryStatus status
    ) {
        Category category = mock(Category.class);

        when(category.getId()).thenReturn(id);
        when(category.getName()).thenReturn(name);
        when(category.getSlug()).thenReturn(slug);
        when(category.getParentId()).thenReturn(parentId);
        when(category.getStatus()).thenReturn(status);

        return category;
    }
}
