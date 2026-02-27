package io.github.takgeun.shop.product.view;

import io.github.takgeun.shop.category.api.dto.response.CategoryResponse;
import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.view.CategorySidebarService;
import io.github.takgeun.shop.category.view.dto.CategoryNode;
import io.github.takgeun.shop.global.error.ForbiddenException;
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
@RequestMapping("/admin/products")
public class AdminProductViewController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CategorySidebarService categorySidebarService;

    /**
     * 상품 목록 페이지 (관리자 전용)
     * GET /admin/products?categoryId={categoryId}&sort={sort}
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            Model model,
            HttpSession session
    ) {

        requireAdmin(session);

        // sort 검증 (아무 값 들어오는 것 방지)
        sort = normalizeSort(sort);

        // admin 컨트롤러니까 true 고정
        List<Product> products = productService.findForList(true, categoryId, sort);

        model.addAttribute("products", products);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("sort", sort);

        // 제목용 카테고리명 (관리자 기준만 사용)
        String selectedCategoryName = null;
        if (categoryId != null) {
            selectedCategoryName = categoryService.findAdminNameOrNull(categoryId);
        }
        model.addAttribute("selectedCategoryName", selectedCategoryName);
        model.addAttribute("treeMode", "admin");        // 관리자 뷰 고정

        return "admin/products/list";
    }

    /**
     * 상품 상세 페이지 (관리자 전용)
     * GET /admin/products/{productId}
     */
    @GetMapping("/{productId}")
    public String detail(
            @PathVariable @Positive Long productId,
            Model model,
            HttpSession session) {

        requireAdmin(session);

        Product product = productService.getForDetail(true, productId);
        model.addAttribute("product", product);
        model.addAttribute("treeMode", "admin");

        return "admin/products/detail";
    }

    private void requireAdmin(HttpSession session) {
        if (session == null) {
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }
        Object roleObj = session.getAttribute(SessionConst.LOGIN_ROLE);
        if (!(roleObj instanceof MemberRole role) || role != MemberRole.ADMIN) {
            throw new ForbiddenException("관리자만 접근할 수 있습니다.");
        }
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) return "latest";
        return switch (sort) {
            case "latest", "best", "sale", "price-low", "price-high", "rating" -> sort;
            default -> "latest";
        };
    }
}
