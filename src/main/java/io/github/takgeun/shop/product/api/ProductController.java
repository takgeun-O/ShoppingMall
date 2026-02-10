package io.github.takgeun.shop.product.api;

import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.dto.response.ProductResponse;
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

    // 카테고리별 상품 목록 조회: /products?categoryId=1
    // @RequestParam : URL 뒤에 붙는 ?key=value 형태의 값을 메서드 파라미터로 받기 위해 사용
    @GetMapping
    public ResponseEntity<List<ProductResponse>> list(
            @RequestParam(required = false)
            @Positive(message = "categoryId는 양수여야 합니다.") Long categoryId
    ) {
        List<Product> products = (categoryId == null)
                ? productService.getAllPublic()
                : productService.getAllPublicByCategoryId(categoryId);
        List<ProductResponse> result = products.stream()
                .map(ProductResponse::from)
                .toList();
        // .map(product -> ProductResponse.from(product))
        // Product를 productResponse 객체로 바꾼다.
        // Entity를 DTO 로 변환하는 책임을 DTO에 둔 설계임. (from() 메서드는 비즈니스 규칙보다는 표현/전송 관점의 변환 로직에 가까움)
        // 따라서 Entity를 DTO로 변환하는 책임은 DTO에 두는 설계가 좋음.
        return ResponseEntity.ok(result);
    }

    // 상품 단건 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> get(
            @PathVariable
            @Positive(message = "productId는 양수여야 합니다.") Long productId
    ) {
        return ResponseEntity.ok(ProductResponse.from(productService.getPublic(productId)));
    }
}
