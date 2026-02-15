package io.github.takgeun.shop.category.view;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.domain.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryViewController {

    private final CategoryService categoryService;

    // public 카테고리 목록 조회
    @GetMapping
    public String categories(Model model) {
        List<Category> categories = categoryService.getAllPublic();
        model.addAttribute("categories", categories);
        return "public/categories/list";
    }
}
