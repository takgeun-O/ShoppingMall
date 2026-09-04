package io.github.takgeun.shop.category.api;

import io.github.takgeun.shop.category.api.dto.response.CategoryResponse;
import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.global.api.ApiController;
import io.github.takgeun.shop.global.error.api.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(           // 해당 컨트롤러를 API 그룹으로 분류
        name = "Public Category",
        description = "일반 사용자에게 공개되는 카테고리 조회 API"
)
// @Validated: AOP 프록시가 존재해야 작동함. 최근 스프링MVC는 컨트롤러 메서드 파라미터의 제약조건을 자체적으로 검증하기 때문에 제거해도 된다.
// @Validated AOP 검증 방식의 경우 주로 ConstraintViolationException 예외가 발생하며
// 이번에 바꾼 Spring MVC 기본 검증은 HandlerMethodValidationException 예외가 주로 발생한다.
//@Validated
@ApiController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryApiController {

    private final CategoryService categoryService;

    /**
     * 공개 카테고리 목록 조회
     */
    @Operation(     // API 한 개의 목적 설명
            summary = "공개 카테고리 목록 조회",
            description = "공개 상태인 카테고리 목록을 부모 카테고리부터 순서대로 조회합니다."
    )
    @ApiResponse(   // 가능한 HTTP 응답 설명
            responseCode = "200",
            description = "카테고리 목록 조회 성공"
    )
    @GetMapping
    public List<CategoryResponse> findAll() {
        return categoryService.getAllPublic().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /**
     * 공개 카테고리 단건 조회
     */
    @Operation(
            summary = "공개 카테고리 단건 조회",
            description = "카테고리 ID로 공개 카테고리 한 건을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "카테고리 조회 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "카테고리 ID 형식 또는 값이 올바르지 않음",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "공개된 카테고리를 찾을 수 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class)   // @Schema : 응답에 사용되는 DTO 타입 지정
                    )
            )
    })
    @GetMapping("/{categoryId}")
    public CategoryResponse findOne(
            @Parameter(
                    description = "조회할 카테고리 ID",
                    example = "1",
                    required = true
            )
            @PathVariable @Positive(message = "카테고리 ID는 양수여야 합니다.") Long categoryId
    ) {
        Category category = categoryService.getPublic(categoryId);

        return CategoryResponse.from(category);
    }
}
