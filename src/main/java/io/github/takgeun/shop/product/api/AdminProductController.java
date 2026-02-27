package io.github.takgeun.shop.product.api;

import io.github.takgeun.shop.global.error.ForbiddenException;
import io.github.takgeun.shop.global.session.SessionConst;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.dto.request.ProductCreateRequest;
import io.github.takgeun.shop.product.dto.request.ProductStatusUpdateRequest;
import io.github.takgeun.shop.product.dto.request.ProductUpdateRequest;
import io.github.takgeun.shop.product.dto.response.ProductCreateResponse;
import io.github.takgeun.shop.product.dto.response.ProductResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final ProductService productService;

    // 상품 생성 (관리자) -> POST /api/v1/admin/products 성공 -> 201 Created + body
    @PostMapping
    public ResponseEntity<ProductCreateResponse> create(
            @Valid @RequestBody ProductCreateRequest request,
            HttpSession session) {

        requireAdmin(session);

        Long productId = productService.create(
                request.getCategoryId(),
                request.getName(),
                request.getPrice(),
                request.getStock(),
                request.getDescription()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ProductCreateResponse(productId));
    }

    // 상품 단건 조회 (관리자) -> GET /api/v1/admin/products/{productId} -> 200 OK + body
    @GetMapping("/{productId:\\d+}")
    public ResponseEntity<ProductResponse> get(
            @PathVariable @Positive Long productId,
            HttpSession session
    ) {
        requireAdmin(session);

        Product product = productService.getForDetail(true, productId);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    // 카테고리별 상품 목록 조회(관리자) -> GET api/v1/admin/products?categoryId={categoryId}&sort={sort} -> 200 OK + body
    @GetMapping
    public ResponseEntity<List<ProductResponse>> list(
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            HttpSession session
    ) {
        requireAdmin(session);

        sort = normalizeSort(sort);

        List<ProductResponse> result = productService.findForList(true, categoryId, sort).stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    // 상품 부분 수정(관리자) -> PATCH /api/v1/admin/products/{productId} -> 204 No Content
    // 상품 수정 (부분 수정)
    // Void : 응답 바디가 없다는 것을 명시한다. (JSON 응답 X, 데이터 반환 X, 오직 상태코드만 전달) 스프링은 기본적으로 200 반환하는데 다른 상태코드 반환하기 위해서
    // 수정 API에서 Void를 사용하는 이유 : PATCH / PUT의 관례
    // 리소스 수정, 성공 여부만 중요, 수정된 데이터 전체를 다시 줄 필요 없음.
    @PatchMapping("/{productId:\\d+}")
    public ResponseEntity<Void> update(
            @PathVariable @Positive Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            HttpSession session
    ) {

        requireAdmin(session);

        productService.update(
                productId,
                request.getCategoryId(),
                request.getName(),
                request.getPrice(),
                request.getStock(),
                request.getDescription()
        );

        return ResponseEntity.noContent().build();
    }

    // 상품 상태 변경(관리자) -> PATCH /api/v1/admin/products/{productId}/status -> 204 No Content
    @PatchMapping("{productId:\\d+}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable @Positive Long productId,
            @Valid @RequestBody ProductStatusUpdateRequest request,
            HttpSession session
    ) {

        requireAdmin(session);

        productService.changeStatus(productId, request.getStatus());
        return ResponseEntity.noContent().build();
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
