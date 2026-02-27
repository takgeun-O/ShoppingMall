package io.github.takgeun.shop.product.view;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.view.CategorySidebarService;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.view.dto.ProductCardView;
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
     * GET /products?categoryId={categoryId}&sort={sort}
     *
     * sort:
     * - null(default) : 기본 목록
     * - best : 베스트 상품
     * - sale : 할인 상품
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            Model model,
            HttpSession session
    ) {

        boolean admin = isAdmin(session);

        // sort 검증 (아무 값 들어오는 것 방지)
        sort = normalizeSort(sort);

        List<Product> products = productService.findForList(admin, categoryId, sort);

        List<ProductCardView> cards = products.stream()
                        .map(ProductCardView::from)
                                .toList();

        model.addAttribute("products", cards);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("sort", sort);

        // 제목용 카테고리명
        String selectedCategoryName = null;
        if(categoryId != null) {
            selectedCategoryName = admin
                    ? categoryService.findAdminNameOrNull(categoryId)
                    : categoryService.findPublicNameOrNull(categoryId);
        }
        model.addAttribute("selectedCategoryName", selectedCategoryName);
        model.addAttribute("treeMode", admin ? "admin" : "public");

        return "public/products/list";
    }

    /**
     * 상품 상세 페이지
     * GET /products/{productId}
     *
     * {productId} 부분에 숫자만 매칭되도록 정규식 제한할 것. (best/sale 같은 문자열 충돌 방지)
     */
    @GetMapping("/{productId:\\d+}")
    public String detail(
            @PathVariable @Positive Long productId,
            Model model, HttpSession session) {

        boolean admin = isAdmin(session);

        Product product = productService.getForDetail(admin, productId);

        model.addAttribute("product", product);
        model.addAttribute("treeMode", admin ? "admin" : "public");

        return "public/products/detail";
    }

    private boolean isAdmin(HttpSession session) {
        if(session == null) return false;
        MemberRole role = (MemberRole) session.getAttribute(SessionConst.LOGIN_ROLE);
        return role == MemberRole.ADMIN;
    }

    private String normalizeSort(String sort) {
        if(sort == null || sort.isBlank()) return "latest";
        return switch (sort) {
            case "latest", "best", "sale", "price-low", "price-high", "rating" -> sort;
            default -> "latest";
        };
    }
}
