package io.github.takgeun.shop.product.view;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.view.dto.ProductCardView;
import io.github.takgeun.shop.product.view.dto.ProductDetailView;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Validated
@Controller
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductViewController {

    private final ProductService productService;
    private final CategoryService categoryService;

    /**
     * 상품 목록 페이지
     * GET /products?categoryId={categoryId}&sort={sort}
     *
     * sort:
     * - null(default) : 기본 목록
     * - best : 베스트 상품
     * - sale : 할인 상품
     *
     * 일반 사용자 컨트롤러의 관심사
     * - 공개 상품 목록
     * - 공개 상품 상세
     * - 비관리자도 접근 가능
     * - ON_SALE, 공개 가능 상품만 노출
     */
    @GetMapping
    public String list(
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            Model model
    ) {

        log.info("ENTER list: categoryId={}, sort={}", categoryId, sort);

        // sort 검증 (아무 값 들어오는 것 방지)
        String normalizedSort = normalizeSort(sort);

        List<Product> products = productService.findPublicList(categoryId, normalizedSort);

        log.info("products={}", products.stream().map(Product::getName).toList());
        List<ProductCardView> cards = products.stream()
                        .map(ProductCardView::from)
                                .toList();

        model.addAttribute("products", cards);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("sort", normalizedSort);

        // 제목용 카테고리명
        String selectedCategoryName = null;
        selectedCategoryName = categoryService.findPublicNameOrNull(categoryId);

        model.addAttribute("selectedCategoryName", selectedCategoryName);
//        model.addAttribute("treeMode", admin ? "admin" : "public");

        return "public/products/list";
    }

    /**
     * 상품 상세 페이지
     * GET /products/{productId}
     *
     * {productId} 부분에 숫자만 매칭되도록 정규식 제한할 것. (best/sale 같은 문자열 충돌 방지)
     */
    @GetMapping("/{productId:\\d+}")
    public String detail(@PathVariable @Positive Long productId,
                         Model model,
                         HttpServletRequest request,
                         RedirectAttributes ra) {

        try {
            Product product = productService.getPublicDetail(productId);
            ProductDetailView view = ProductDetailView.from(product);

            String query = request.getQueryString();    // 사용자가 상품 목록 페이지를 어떻게 보고 있었는지 저장

            // 사용자가 상품 목록에서 선택했던 카테고리, 정렬 조건을 유지하기 위해 만듦.
            // 이건 상세페이지에서 상품 목록으로 돌아갈 때 사용
            String returnUrl = "/products";
            if(query != null && !query.isBlank()) {
                returnUrl += "?" + query;
            }

            // 사용자가 상품 목록에서 선택했던 카테고리, 정렬 조건을 유지하기 위해 만듦.
            // 이건 로그인 등 작업 후 현재 상품 상세 페이지로 돌아올 때 사용
            // 여기서는 장바구니에 추가하는 요청폼을 통해서 CartViewController로 값이 전달됨.
            // 장바구니에 담는 순간 현재 보고 있는 상품상세페이지로 이동되게끔 한 것.
            String detailReturnUrl = request.getRequestURI();
            if(query != null && !query.isBlank()) {
                detailReturnUrl += "?" + query;
            }

            model.addAttribute("product", view);
//            model.addAttribute("treeMode", admin ? "admin" : "public");
            model.addAttribute("returnUrl", returnUrl);
            model.addAttribute("detailReturnUrl", detailReturnUrl);

            return "public/products/detail";
        } catch (NotFoundException e) {
            ra.addFlashAttribute("error", "해당 상품은 현재 판매 중이 아니거나 찾을 수 없습니다.");
            return "redirect:/products";
        }
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
