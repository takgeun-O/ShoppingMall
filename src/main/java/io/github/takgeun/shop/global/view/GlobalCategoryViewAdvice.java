package io.github.takgeun.shop.global.view;

import io.github.takgeun.shop.category.api.dto.response.CategoryResponse;
import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.domain.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalCategoryViewAdvice {

    private final CategoryService categoryService;

    /**
     * 헤더 상단에 노출할 대표 카테고리 (1뎁스)
     */
    @ModelAttribute("rootCategories")
    public List<Category> rootCategories() {
        return categoryService.getTopCategories();
    }

    /**
     * 햄버거 드로어에 노출할 전체 카테고리
     */
    @ModelAttribute("allCategories")
    public List<Category> allCategories() {
        return categoryService.getAllPublic();
    }
}
