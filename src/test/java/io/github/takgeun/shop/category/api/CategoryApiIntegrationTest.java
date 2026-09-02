package io.github.takgeun.shop.category.api;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.category.application.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공개 카테고리 API 통합 테스트
 *
 * 실제 흐름:
 * MockMvc
 * -> CategoryApiController
 * -> CategoryService
 * -> MyBatisCategoryRepository
 * -> CategoryMapper
 * -> 테스트 DB
 * -> ApiGlobalExceptionHandler
 */
@Transactional      // 실제 DB에 카테고리 저장하기 때문에 사용 + 각 테스트 끝날 때 해당 테스트에서 생성한 데이터 롤백
@ActiveProfiles({"test", "mybatis"})
public class CategoryApiIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CategoryService categoryService;

    @Test
    void 공개_카테고리_목록_조회_통합_성공() throws Exception {

        // given
        String rootName = "전자-" + System.nanoTime();        // nanoTime : 카테고리 이름과 slug에 중복 제약이 있기 때문에 충돌 방지하기 위함

        String childName = "컴퓨터-" + System.nanoTime();

        Long rootId = categoryService.create(rootName, null);

        Long childId = categoryService.create(childName, rootId);

        // when & then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())

                // 생성한 카테고리가 실제 목록에 포함되는지 검증
                .andExpect(jsonPath(
                        "$[*].name",
                        hasItem(rootName)
                ))
                .andExpect(jsonPath(
                        "$[*].name",
                        hasItem(childName)
                ))

                // 공개 응답에서 내부 필드가 제외되는지 검증
                .andExpect(jsonPath("$[0].nameKey")
                        .doesNotExist())
                .andExpect(jsonPath("$[0].status")
                        .doesNotExist());
    }

    @Test
    void 공개_카테고리_단건_조회_통합_성공() throws Exception {

        // given
        String categoryName = "가전-" + System.nanoTime();

        Long categoryId = categoryService.create(categoryName, null);

        // when & then
        mockMvc.perform(get("/api/v1/categories/{categoryId}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(categoryId))
                .andExpect(jsonPath("$.name")
                        .value(categoryName))
                .andExpect(jsonPath("$.slug").exists())
                .andExpect(jsonPath("$.parentId")
                        .doesNotExist())

                // 공개 응답에서 내부 필드가 제외되는지 검증
                .andExpect(jsonPath("$.nameKey")
                        .doesNotExist())
                .andExpect(jsonPath("$.status")
                        .doesNotExist());
    }

    @Test
    void 공개_하위_카테고리_단건_조회_통합_성공() throws Exception {

        // given
        String parentName = "전자-" + System.nanoTime();

        String childName = "노트북-" + System.nanoTime();

        Long parentId = categoryService.create(parentName, null);

        Long childId = categoryService.create(childName, parentId);

        // when & then
        mockMvc.perform(get("/api/v1/categories/{categoryId}", childId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(childId))
                .andExpect(jsonPath("$.name")
                        .value(childName))
                .andExpect(jsonPath("$.parentId")
                        .value(parentId));
    }

    @Test
    void 공개_카테고리_단건_조회_통합_실패_카테고리_없음() throws Exception {

        // given
        long notExistingCategoryId = Long.MAX_VALUE;

        // when & then
        mockMvc.perform(get("/api/v1/categories/{categoryId}", notExistingCategoryId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code")
                        .value("CATEGORY_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("카테고리가 존재하지 않습니다."))
                .andExpect(jsonPath("$.path")
                        .value(
                                "/api/v1/categories/"
                                        + notExistingCategoryId
                        ))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void 카테고리_ID가_양수가_아니면_400을_반환한다(long invalidCategoryId) throws Exception {

        // when & then
        mockMvc.perform(get("/api/v1/categories/{categoryId}", invalidCategoryId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message")
                        .value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.path")
                        .value(
                                "/api/v1/categories/"
                                        + invalidCategoryId
                        ))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}
