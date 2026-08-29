package io.github.takgeun.shop.category.api;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.global.error.api.ApiGlobalExceptionHandler;
import io.github.takgeun.shop.global.error.code.ErrorCode;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryApiControllerTest {

    private CategoryService categoryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        categoryService = mock(CategoryService.class);

        CategoryApiController controller =
                new CategoryApiController(categoryService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new ApiGlobalExceptionHandler()
                )
                .build();
    }

    @Test
    void 공개_카테고리_목록_조회_성공() throws Exception {

        // given
        Category electronics = category(
            1L,
                "전자",
                "electronics",
                null
        );

        Category computer = category(
                2L,
                "컴퓨터",
                "computer",
                1L
        );

        when(categoryService.getAllPublic())
                .thenReturn(List.of(
                        electronics,
                        computer
                ));

        // when & then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name")
                        .value("전자"))
                .andExpect(jsonPath("$[0].slug")
                        .value("electronics"))
                .andExpect(jsonPath("$[0].parentId")
                        .doesNotExist())

                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name")
                        .value("컴퓨터"))
                .andExpect(jsonPath("$[1].slug")
                        .value("computer"))
                .andExpect(jsonPath("$[1].parentId")
                        .value(1))

                // 공개 응답에서 내부 필드가 제외되는지 검증
                .andExpect(jsonPath("$[0].nameKey").doesNotExist())
                .andExpect(jsonPath("$[0].status").doesNotExist());

        verify(categoryService).getAllPublic(); // Mockito를 이용한 테스트 실행 중 categoryService.getAllPublic()이 실제로 한번만 호출됐는지 검증
    }

    @Test
    void 공개_카테고리_목록이_없으면_빈_배열을_반환한다() throws Exception {

        // given
        when(categoryService.getAllPublic()).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(categoryService).getAllPublic();
    }

    @Test
    void 공개_카테고리_단건_조회_성공() throws Exception {

        // given
        Long categoryId = 1L;

        Category electronics = category(
                categoryId,
                "전자",
                "electronics",
                null
        );

        when(categoryService.getPublic(categoryId))
                .thenReturn(electronics);

        // when & then
        mockMvc.perform(get("/api/v1/categories/{categoryId}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name")
                        .value("전자"))
                .andExpect(jsonPath("$.slug")
                        .value("electronics"))
                .andExpect(jsonPath("$.parentId")
                        .doesNotExist())

                // 공개 응답에서 내부 필드가 제외되는지 검증
                .andExpect(jsonPath("$.nameKey")
                        .doesNotExist())
                .andExpect(jsonPath("$.status")
                        .doesNotExist());

        verify(categoryService).getPublic(categoryId);
    }

    @Test
    void 공개_카테고리_단건_조회_실패_카테고리가_없음() throws Exception{

        // given
        Long categoryId = 999L;

        when(categoryService.getPublic(categoryId))
                .thenThrow(new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/categories/{categoryId}", categoryId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code")
                        .value("CATEGORY_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("카테고리가 존재하지 않습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/categories/999"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());    // BusinessException 처리기의 ApiErrorResponse.of(...) 로 인해 빈 리스트로 들어감

        verify(categoryService).getPublic(categoryId);
    }

    @Test
    void 카테고리_ID_타입이_잘못되면_400을_반환한다() throws Exception {

        // when & then
        mockMvc.perform(get("/api/v1/categories/{categoryId}", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message")
                        .value("요청 값의 형식이 올바르지 않습니다."))
                .andExpect(jsonPath("$.path")
                        .value(
                                "/api/v1/categories/not-a-number"
                        ))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    /**
     * Controller 응답 변환에 사용할 테스트용 Category 생성
     *
     * 카테고리 생성 규칙이랑 저장 로직은 CategoryServiceTest에서 이미 검증하니까 이 테스트에서는 Mock 사용
     */
    private Category category(
            Long id,
            String name,
            String slug,
            Long parentId
    ) {
        Category category = mock(Category.class);

        when(category.getId()).thenReturn(id);  // 이 가짜 객체의 getId()가 호출되면 매개변수로 받은 id를 반환
        when(category.getName()).thenReturn(name);
        when(category.getSlug()).thenReturn(slug);
        when(category.getParentId()).thenReturn(parentId);

        return category;
    }
}