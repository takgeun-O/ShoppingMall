package io.github.takgeun.shop.product.view;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.global.error.ForbiddenException;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.dto.request.ProductCreateRequest;
import io.github.takgeun.shop.product.view.dto.ProductCardView;
import io.github.takgeun.shop.product.view.dto.ProductDetailView;
import io.github.takgeun.shop.product.view.form.ProductCreateForm;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/products")
public class AdminProductViewController {

    private final ProductService productService;
    private final CategoryService categoryService;

    /**
     * 상품 목록 페이지 (관리자 전용)
     * GET /admin/products?categoryId={categoryId}&sort={sort}
     *
     * 관리자 컨트롤러 관심사
     * - 전체 상품 조회
     * - 숨김/준비중/판매중지 상품까지 조회
     * - 상품 등록
     * - 상품 수정
     * - 상품 상태 변경
     * - 관리자 권한 체크
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            Model model,
            HttpSession session
    ) {

        log.info("관리자 상품목록 진입 : categoryId={}, sort={}", categoryId, sort);
        requireAdmin(session);

        // sort 검증 (아무 값 들어오는 것 방지)
        String normalizedSort = normalizeSort(sort);

        // admin 컨트롤러니까 true 고정
        List<Product> products = productService.findForList(true, categoryId, normalizedSort);

        List<ProductCardView> cards = products.stream()
                .map(ProductCardView::from)
                .toList();

        // 제목용 카테고리명 (관리자 기준만 사용)
        String selectedCategoryName = null;
        if (categoryId != null) {
            selectedCategoryName = categoryService.findAdminNameOrNull(categoryId);
        }

        model.addAttribute("products", cards);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedCategoryName", selectedCategoryName);
        model.addAttribute("sort", normalizedSort);
        model.addAttribute("treeMode", "admin");        // 관리자 뷰 고정
        model.addAttribute("isAdmin", true);

        return "admin/products/list";
    }

    /**
     * 상품 상세 페이지 (관리자 전용)
     * GET /admin/products/{productId}
     */
    @GetMapping("/{productId:\\d+}")
    public String detail(
            @PathVariable @Positive Long productId,
            Model model,
            HttpSession session) {

        requireAdmin(session);

        Product product = productService.getForDetail(true, productId);
        ProductDetailView view = ProductDetailView.from(product);

        model.addAttribute("product", view);
        model.addAttribute("treeMode", "admin");
        model.addAttribute("isAdmin", true);

        return "admin/products/detail";
    }

    /**
     * 상품 등록 폼
     * GET /admin/products/create
     */
    @GetMapping("/create")
    public String createForm(Model model, HttpSession session) {
        requireAdmin(session);

        if(!model.containsAttribute("form")) {
            model.addAttribute("form", new ProductCreateForm());
        }

        model.addAttribute("categories", categoryService.getAllAdminCategories());
        model.addAttribute("treeMode", "admin");
        model.addAttribute("isAdmin", true);

        return "admin/products/create";
    }

    /**
     * 상품 등록 처리
     * POST /admin/products/create
     */
    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("form") ProductCreateForm form,
            BindingResult bindingResult,
            Model model,
            HttpSession session
    ) {
        requireAdmin(session);

        if(bindingResult.hasErrors()) {
            // 에러 났을 때 제자리 포워딩하는 동시에 정보 그대로 옮기기
            model.addAttribute("categories", categoryService.getAllAdminCategories());
            model.addAttribute("treeMode", "admin");
            model.addAttribute("isAdmin", true);
            return "admin/products/create";
        }

        try {
            Long productId = productService.create(
                    form.getCategoryId(),
                    form.getName(),
                    form.getPrice(),
                    form.getStock(),
                    form.getDescription(),
                    form.getStatus(),
                    form.getOriginalPrice(),
                    form.getImageUrl()
            );
            return "redirect:/admin/products/" + productId;
        } catch (IllegalArgumentException e) {
            bindingResult.reject("createFail", e.getMessage());
            model.addAttribute("categories", categoryService.getAllAdminCategories());
            model.addAttribute("treeMode", "admin");
            model.addAttribute("isAdmin", true);
            return "admin/products/create";
        }
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
