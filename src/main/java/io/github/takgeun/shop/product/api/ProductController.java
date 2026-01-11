package io.github.takgeun.shop.product.api;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.dto.request.CategoryCreateRequest;
import io.github.takgeun.shop.category.dto.request.CategoryUpdateRequest;
import io.github.takgeun.shop.category.dto.response.CategoryCreateResponse;
import io.github.takgeun.shop.category.dto.response.CategoryResponse;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.dto.request.ProductCreateRequest;
import io.github.takgeun.shop.product.dto.request.ProductUpdateRequest;
import io.github.takgeun.shop.product.dto.response.ProductCreateResponse;
import io.github.takgeun.shop.product.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/* HTTP 요청을 받아 Service에 위임하고 HTTP 응답으로 변환해서 돌려주는 역할
* 비즈니스 로직 X
* 검증/흐름 제어 O
* 도메인 조작 X*/

@RestController                     // HTTP 요청을 처리하는데 반환값을 View가 아니라 JSON(Response Body) 로 보내고자 하는 의도
@RequiredArgsConstructor            // 필수 의존성만 받는 생성자를 자동으로 만들어주는 어노테이션
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    // 컨트롤러는 서비스에만 의존한다.
    // Repository/Entity 접근 X

    // 상품 생성
    @PostMapping
    public ResponseEntity<ProductCreateResponse> create(@RequestBody ProductCreateRequest request) {
        Long id = productService.create(request.getCategoryId(), request.getName(),
                request.getPrice(), request.getStock(), request.getDescription());

        // ResponseEntity : HTTP 응답 전체(상태코드 + 헤더 + 바디)를 표현하는 객체
        // ResponseEntity 없이 쓰면
        // return new CategoryCreateResponse(id); --> 상태코드: 200 OK (기본값)으로 되어 헤더 제어 불가
        return ResponseEntity.status(HttpStatus.CREATED)        // 상태코드 직접 지정
                .body(new ProductCreateResponse(id));           // 응답바디(JSON)
        // 응답DTO --> JSON 변환한다. (Jackson이 직렬화함)
        // @RestController + @ResponseBody(@RestController에 이미 포함) 의 역할 덕분임.
    }

    // 카테고리별 상품 목록 조회: /products?categoryId=1
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getByCategory(@RequestParam Long categoryId) {
        // @RequestParam : URL 뒤에 붙는 ?key=value 형태의 값을 메서드 파라미터로 받기 위해 사용
        List<ProductResponse> result = productService.getByCategory(categoryId).stream()
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
    public ResponseEntity<ProductResponse> get(@PathVariable Long productId) {
        return ResponseEntity.ok(ProductResponse.from(productService.get(productId)));
    }

    // 상품 수정 (부분 수정)
    // Void : 응답 바디가 없다는 것을 명시한다. (JSON 응답 X, 데이터 반환 X, 오직 상태코드만 전달) 스프링은 기본적으로 200 반환하는데 다른 상태코드 반환하기 위해서
    // 수정 API에서 Void를 사용하는 이유 : PATCH / PUT의 관례
    // 리소스 수정, 성공 여부만 중요, 수정된 데이터 전체를 다시 줄 필요 없음.
    @PatchMapping("/{productId}")
    public ResponseEntity<Void> update(@PathVariable Long productId, @RequestBody ProductUpdateRequest request) {
        productService.update(productId, request.getCategoryId(), request.getName(), request.getPrice(),
                request.getStock(), request.getDescription(), request.getActive());
        return ResponseEntity.noContent().build();
        // HTTP 상태 코드는 204 No Content (요청은 성공했고 응답 본문은 없다.)
        // .build() : ResponseEntity 객체 생성 완료
    }

    // 상품 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable Long productId) {
        productService.delete(productId);
        return ResponseEntity.noContent().build();
    }
}
