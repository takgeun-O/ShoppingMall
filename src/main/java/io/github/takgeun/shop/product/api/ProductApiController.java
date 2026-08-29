package io.github.takgeun.shop.product.api;

import io.github.takgeun.shop.global.error.api.ApiErrorResponse;
import io.github.takgeun.shop.product.api.dto.response.ProductDetailResponse;
import io.github.takgeun.shop.product.api.dto.response.ProductListItemResponse;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Public Product",
        description = "일반 사용자에게 공개되는 상품 조회 API"
)
// @Validated: AOP 프록시가 존재해야 작동함. 최근 스프링MVC는 컨트롤러 메서드 파라미터의 제약조건을 자체적으로 검증하기 때문에 제거해도 된다.
// @Validated AOP 검증 방식의 경우 주로 ConstraintViolationException 예외가 발생하며
// 이번에 바꾼 Spring MVC 기본 검증은 HandlerMethodValidationException 예외가 주로 발생한다.
// 즉 ApiGlobalExceptionHandler에 HandlerMethodValidationException을 처리할 처리기가 있어야 공통 JSON으로 400 반환할 것
//@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductApiController {

    private final ProductService productService;

    /**
     * 공개 상품 목록 조회
     *
     * GET /api/v1/products
     * GET /api/v1/products?categoryId=1
     * GET /api/v1/products?categoryId=1&sort=price-low
     */
    @Operation(
            summary = "공개 상품 목록 조회",
            description = """
                    판매 중이며 일반 사용자에게 공개 가능한 상품 목록을 조회합니다.
                    
                    CategoryId를 전달하면 해당 카테고리와 하위 카테고리에 속한 상품을 함께 조회합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 목록 조회 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "카테고리 ID 또는 정렬 조건이 올바르지 않음",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "조회할 카테고리가 존재하지 않거나 공개 상태가 아님",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiErrorResponse.class
                            )
                    )
            )
    })
    @GetMapping
    public List<ProductListItemResponse> findAll(
            @Parameter(
                    description = "조회할 카테고리 ID. 생략하면 전체 공개 상품을 조회합니다.",
                    example = "1"
            )
            @RequestParam(required = false)
            @Positive(message = "카테고리 ID는 양수여야 합니다.")
            Long categoryId,

            @Parameter(
                    description = """
                            상품 정렬 조건:
                            latest, best, sale, price-low, price-high, rating
                            """,
                    example = "latest"
            )
            @RequestParam(defaultValue = "latest")
            @Pattern(
                    regexp = "latest|best|sale|price-low|price-high|rating",
                    message = "지원하지 않는 상품 정렬 조건입니다."
            )
            String sort
    ) {
        return productService.findPublicList(categoryId, sort).stream()
                .map(ProductListItemResponse::from)
                .toList();
    }


    /**
     * 공개 상품 단건 조회
     */
    @Operation(
            summary = "공개 상품 상세 조회",
            description = """
                    상품 ID로 공개 상품의 상세 정보를 조회합니다.
                    
                    존재하지 않거나 일반 사용자에게 공개할 수 없는 상품은 동일하게 404 응답을 반환합니다.            
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 상세 조회 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "상품 ID의 형식 또는 값이 올바르지 않음",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{productId}")
    public ProductDetailResponse findOne(
        @Parameter(
                description = "조회할 상품 ID",
                example = "1",
                required = true
        )
        @PathVariable @Positive(message = "상품 ID는 양수여야 합니다") Long productId
    ) {
        Product product = productService.getPublicDetail(productId);

        return ProductDetailResponse.from(product);
    }
}
