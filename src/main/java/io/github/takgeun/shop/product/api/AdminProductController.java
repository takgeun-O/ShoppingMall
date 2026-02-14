package io.github.takgeun.shop.product.api;

import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.dto.request.ProductCreateRequest;
import io.github.takgeun.shop.product.dto.request.ProductStatusUpdateRequest;
import io.github.takgeun.shop.product.dto.request.ProductUpdateRequest;
import io.github.takgeun.shop.product.dto.response.ProductCreateResponse;
import io.github.takgeun.shop.product.dto.response.ProductResponse;
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
@RequestMapping("api/v1/admin/products")
public class AdminProductController {

    private final ProductService productService;

    // 상품 생성 (관리자) -> POST /admin/products 성공 -> 201 Created + body
    @PostMapping
    public ResponseEntity<ProductCreateResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        Long categoryId = request.getCategoryId();
        String name = request.getName();
        int price = request.getPrice();
        int stock = request.getStock();
        String description = request.getDescription();

        Long productId = productService.create(categoryId, name, price, stock, description);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ProductCreateResponse(productId));
    }

    // 상품 단건 조회 (관리자) -> GET /admin/products/{productId} -> 200 OK + body
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> get(
            @PathVariable
            @NotNull(message = "productId는 필수입니다.")
            @Positive(message = "productId는 양수여야 합니다.") Long productId
    ) {
        return ResponseEntity.ok(ProductResponse.from(productService.getAdmin(productId)));
    }

    // 카테고리별 상품 목록 조회(관리자) -> GET /admin/products -> 200 OK + body
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getByCategory(
            @RequestParam
            @NotNull(message = "categoryId는 필수입니다.")
            @Positive(message = "categoryId는 양수여야 합니다.") Long categoryId
    ) {
        List<ProductResponse> result = productService.getByCategoryAdmin(categoryId).stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    // 상품 부분 수정(관리자) -> PATCH /admin/products/{productId} -> 204 No Content
    // 상품 수정 (부분 수정)
    // Void : 응답 바디가 없다는 것을 명시한다. (JSON 응답 X, 데이터 반환 X, 오직 상태코드만 전달) 스프링은 기본적으로 200 반환하는데 다른 상태코드 반환하기 위해서
    // 수정 API에서 Void를 사용하는 이유 : PATCH / PUT의 관례
    // 리소스 수정, 성공 여부만 중요, 수정된 데이터 전체를 다시 줄 필요 없음.
    @PatchMapping("/{productId}")
    public ResponseEntity<Void> update(
            @PathVariable
            @NotNull(message = "productId는 필수입니다.")
            @Positive(message = "productId는 양수여야 합니다.") Long productId,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        Long categoryId = request.getCategoryId();
        String name = request.getName();
        Integer price = request.getPrice();
        Integer stock = request.getStock();
        String description = request.getDescription();
        productService.update(productId, categoryId, name, price, stock, description);

        return ResponseEntity.noContent().build();
    }

    // 상품 상태 변경(관리자) -> PATCH /admin/products/{productId}/status -> 204 No Content
    @PatchMapping("{productId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable
            @NotNull(message = "productId는 필수입니다.")
            @Positive(message = "productId는 양수여야 합니다.") Long productId,
            @Valid @RequestBody ProductStatusUpdateRequest request
    ) {
        productService.changeStatus(productId, request.getStatus());
        return ResponseEntity.noContent().build();
    }
}
