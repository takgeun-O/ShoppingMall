package io.github.takgeun.shop.product.view;

import io.github.takgeun.shop.category.api.dto.response.CategoryResponse;
import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.view.CategorySidebarService;
import io.github.takgeun.shop.category.view.dto.CategoryNode;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
            Model model,
            HttpSession session
    ) {

        boolean admin = isAdmin(session);

        // 카테고리 목록은 권한에 따라
        List<CategoryResponse> categories = admin
                ? categoryService.getAllAdminCategories()
                : categoryService.getAllPublicCategories();

        // 트리 렌더링 데이터
        // public 사용자에게 보여질 카테고리 리스트 반환 (단, 루트 노드가 비공개면 자식 노드가 공개여도 전부 비공개 처리)
        // null -> [전자, 의류]
        // 1 -> [노트북, 휴대폰, 상의]
        Map<Long, List<CategoryResponse>> childrenByParent = categorySidebarService.groupByParent(categories);
        Set<Long> openIds = categorySidebarService.buildOpenIds(categoryId, categories);    // 선택된 카테고리와 선택된 카테고리의 조상까지 오픈 대상

        // 상품 조회 (예: 카테고리 필터)
        Set<Long> categoryIdsForProducts = (categoryId == null)
                ? null
                : categorySidebarService.buildSubtreeIds(categoryId, childrenByParent);

        // 상품조회도 권한에 따라
        List<Product> products = (categoryId == null)
                ? (admin ? productService.getAllAdmin() : productService.getAllPublic())
                : (admin ? productService.getAllAdminByCategoryIds(categoryIdsForProducts)
                         : productService.getAllPublicByCategoryIds(categoryIdsForProducts));

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
        model.addAttribute("treeMode", admin ? "admin" : "public");   // 템플릿 분기용

        return "public/products/list";
    }

    /**
     * 상품 상세 페이지
     * GET /products/{productId}
     */
    @GetMapping("/{productId}")
    public String detail(@PathVariable @Positive Long productId, Model model, HttpSession session) {

        boolean admin = isAdmin(session);

        Product product = admin
                ? productService.getAdmin(productId)
                : productService.getPublic(productId);

        model.addAttribute("product", product);
        model.addAttribute("treeMode", admin ? "admin" : "public");

        return "public/products/detail";
    }

    private boolean isAdmin(HttpSession session) {
        MemberRole role = (MemberRole) session.getAttribute(SessionConst.LOGIN_ROLE);
        return role == MemberRole.ADMIN;
    }
}
