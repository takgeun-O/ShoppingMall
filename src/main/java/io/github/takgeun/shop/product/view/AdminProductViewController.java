package io.github.takgeun.shop.product.view;

import io.github.takgeun.shop.category.api.dto.response.CategoryResponse;
import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.domain.Category;
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
import java.util.Map;
import java.util.Set;

@Validated
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/products")
public class AdminProductViewController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CategorySidebarService categorySidebarService;

    /**
     * 상품 목록 페이지
     * GET /admin/products?categoryId={categoryId}
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) @Positive Long categoryId,
            Model model
    ) {

        // admin -> 전체 카테고리(숨김/비공개 포함)
        List<CategoryResponse> categories = categoryService.getAllAdminCategories();

        // 트리 렌더링 데이터
        Map<Long, List<CategoryResponse>> childrenByParent = categorySidebarService.groupByParent(categories);
        Set<Long> openIds = categorySidebarService.buildOpenIds(categoryId, categories);

        // 상품 조회 (관리자용 전체/필터)
        Set<Long> categoryIdsForProducts = (categoryId == null)
                ? null
                : categorySidebarService.buildSubtreeIds(categoryId, childrenByParent);
        List<Product> products = (categoryId == null)
                ? productService.getAllAdmin()
                : productService.getAllAdminByCategoryIds(categoryIdsForProducts);

        model.addAttribute("products", products);
        model.addAttribute("selectedCategoryName",
                categories.stream()
                        .filter(c -> categoryId != null && categoryId.equals(c.getId()))
                        .map(CategoryResponse::getName)
                        .findFirst()
                        .orElse(null)
        );

        model.addAttribute("selectedCategoryId", categoryId);

        // sidebar model
        model.addAttribute("categories", categories);
        model.addAttribute("childrenByParent", childrenByParent);
        model.addAttribute("openIds", openIds);
        model.addAttribute("treeMode", "admin");    // 템플릿 분기용

        return "admin/products/list";
    }

    /**
     * 상품 상세 페이지
     * GET /admin/products/{productId}
     */
    @GetMapping("/{productId}")
    public String detail(@PathVariable @Positive Long productId, Model model) {

        Product product = productService.getAdmin(productId);
        model.addAttribute("product", product);
        model.addAttribute("treeMode", "admin");

        return "admin/products/detail";
    }
}
