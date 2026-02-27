package io.github.takgeun.shop.product.api;

import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.dto.response.ProductResponse;
import io.github.takgeun.shop.product.view.dto.ProductCardView;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/* HTTP 요청을 받아 Service에 위임하고 HTTP 응답으로 변환해서 돌려주는 역할
* 비즈니스 로직 X
* 검증/흐름 제어 O
* 도메인 조작 X*/

@Validated              // @RequestParam 이나 @PathVariable 검증할 때 필요
@RestController                     // HTTP 요청을 처리하는데 반환값을 View가 아니라 JSON(Response Body) 로 보내고자 하는 의도
@RequiredArgsConstructor            // 필수 의존성만 받는 생성자를 자동으로 만들어주는 어노테이션
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    // 컨트롤러는 서비스에만 의존한다.
    // Repository/Entity 접근 X

    // 카테고리별 상품 목록 조회: /products?categoryId={categoryId}&sort={sort}
    @GetMapping
    public ResponseEntity<List<ProductResponse>> list(
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            HttpSession session
    ) {

        boolean admin = isAdmin(session);

        // sort 검증 (아무 값 들어오는 것 방지)
        sort = normalizeSort(sort);

        List<Product> products = productService.findForList(admin, categoryId, sort);
        List<ProductResponse> result = products.stream()
                .map(ProductResponse::from)
                .toList();

        return ResponseEntity.ok(result);
    }

    // 상품 단건 조회
    @GetMapping("/{productId:\\d+}")
    public ResponseEntity<ProductResponse> get(
            @PathVariable @Positive Long productId,
            HttpSession session
    ) {
        boolean admin = isAdmin(session);

        Product product = productService.getForDetail(admin, productId);
        return ResponseEntity.ok(ProductResponse.from(product));
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
