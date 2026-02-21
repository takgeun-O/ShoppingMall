package io.github.takgeun.shop.product.view;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.view.CategorySidebarService;
import io.github.takgeun.shop.category.view.dto.CategoryNode;
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
@RequestMapping("/products")
public class ProductViewController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CategorySidebarService categorySidebarService;

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
//        List<Category> categories = categoryService.getAllPublic();
//        model.addAttribute("categories", categories);

        // 선택된 카테고리 표시용
        model.addAttribute("selectedCategoryId", categoryId);

        List<CategoryNode> sidebarRoots = categorySidebarService.buildSidebarTrees(categoryId);
        model.addAttribute("sidebarRoots", sidebarRoots);

        // 상품 목록(공개/판매중만)
        List<Product> products;
        if(categoryId == null) {
            products = productService.getAllPublic();
        } else {
            products = productService.getAllPublicByCategoryId(categoryId);
        }
        model.addAttribute("products", products);

        return "public/products/list";
    }

    /**
     * 상품 상세 페이지
     * GET /products/{productId}
     */
    @GetMapping("/{productId}")
    public String detail(@PathVariable @Positive Long productId, Model model) {

        Product product = productService.getPublic(productId);
        model.addAttribute("product", product);

        return "public/products/detail";
    }
}
