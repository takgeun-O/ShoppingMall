package io.github.takgeun.shop.product.api;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Validated
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/products")
public class AdminProductViewController {

    private final ProductService productService;
    private final CategoryService categoryService;

    /**
     * 상품 목록 페이지
     * GET /products?categoryId={categoryId}
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) @Positive Long categoryId,
            Model model
    ) {

        // 카테고리 목록 (필터 UI용)
        List<Category> categories = categoryService.getAllAdmin();
        model.addAttribute("categories", categories);

        // 선택된 카테고리 표시용
        model.addAttribute("selectedCategoryId", categoryId);

        // 상품 목록
        List<Product> products;
        if(categoryId == null) {
            products = productService.getAllPublic();
        } else {
            products = productService.getAllPublicByCategoryId(categoryId);
        }
        model.addAttribute("products", products);

        return "products/list";
    }

    /**
     * 상품 상세 페이지
     * GET /products/{productId}
     */
    @GetMapping("/{productId}")
    public String detail(@PathVariable @Positive Long productId, Model model) {

        Product product = productService.getAdmin(productId);
        model.addAttribute("product", product);

        return "products/detail";
    }
}
