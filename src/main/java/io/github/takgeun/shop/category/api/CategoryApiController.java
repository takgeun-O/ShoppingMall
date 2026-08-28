package io.github.takgeun.shop.category.api;

import io.github.takgeun.shop.category.api.dto.response.CategoryResponse;
import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.domain.Category;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryApiController {

    private final CategoryService categoryService;

    /**
     * 공개 카테고리 목록 조회
     */
    @GetMapping
    public List<CategoryResponse> findAll() {
        return categoryService.getAllPublic().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /**
     * 공개 카테고리 단건 조회
     */
    @GetMapping("/{categoryId}")
    public CategoryResponse findOne(
            @PathVariable @Positive(message = "카테고리 ID는 양수여야 합니다.") Long categoryId
    ) {
        Category category = categoryService.getPublic(categoryId);

        return CategoryResponse.from(category);
    }
}
