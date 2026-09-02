package io.github.takgeun.shop.product.api;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.product.api.dto.response.ProductDetailResponse;
import io.github.takgeun.shop.product.api.dto.response.ProductListItemResponse;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@ActiveProfiles({"test", "mybatis"})
class ProductApiIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProductService productService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 공개_상품_목록_조회_성공() throws Exception {

        // given
        Long categoryId = createCategory("전자");

        Long keyboardId = createProduct(
                categoryId,
                "무선 키보드",
                99_000,
                10,
                ProductStatus.ON_SALE,
                120_000
        );

        Long mouseId = createProduct(
                categoryId,
                "무선 마우스",
                39_000,
                20,
                ProductStatus.ON_SALE,
                null
        );

        // when
        List<ProductListItemResponse> response = requestProductList("/api/v1/products");

        // then
        assertThat(response)
                .extracting(ProductListItemResponse::id)
                .contains(keyboardId, mouseId);

        findListItem(response, keyboardId);
    }

    @Test
    void 공개_상품이_없으면_빈_목록을_반환한다() throws Exception {

        // given
        Long categoryId = createCategory("빈 카테고리");

        // when
        List<ProductListItemResponse> response = requestProductList("/api/v1/products?categoryId=" + categoryId);

        // then
        assertThat(response).isEmpty();
    }

    @Test
    void 카테고리를_선택하면_해당_카테고리와_하위_카테고리_상품을_조회한다() throws Exception {

        // given
        Long electronicsId = createCategory("전자");
        Long computerId = createChildCategory("컴퓨터", electronicsId);
        Long furnitureId = createCategory("가구");

        Long electronicsProductId = createProduct(
                electronicsId,
                "전자 상품",
                100_000,
                10,
                ProductStatus.ON_SALE,
                null
        );

        Long computerProductId = createProduct(
                computerId,
                "컴퓨터 상품",
                200_000,
                10,
                ProductStatus.ON_SALE,
                null
        );

        Long furnitureProductId = createProduct(
                furnitureId,
                "가구 상품",
                300_000,
                10,
                ProductStatus.ON_SALE,
                null
        );

        // when
        List<ProductListItemResponse> response = requestProductList("/api/v1/products?categoryId=" + electronicsId);

        // then
        assertThat(response)
                .extracting(ProductListItemResponse::id)
                .containsExactlyInAnyOrder(electronicsProductId, computerProductId)
                .doesNotContain(furnitureProductId);
    }

    @Test
    void 가격_낮은_순으로_공개_상품을_조회한다() throws Exception {

        // given
        Long categoryId = createCategory("가격 정렬");

        Long expensiveId = createProduct(
                categoryId,
                "고가 상품",
                300_000,
                10,
                ProductStatus.ON_SALE,
                null
        );

        Long cheapId = createProduct(
                categoryId,
                "저가 상품",
                10_000,
                10,
                ProductStatus.ON_SALE,
                null
        );

        Long middleId = createProduct(
                categoryId,
                "중간 가격 상품",
                100_000,
                10,
                ProductStatus.ON_SALE,
                null
        );

        // when
        List<ProductListItemResponse> response = requestProductList(
                "/api/v1/products?categoryId=" + categoryId + "&sort=price-low"
        );

        // then
        assertThat(response)
                .extracting(ProductListItemResponse::id)
                .containsExactly(cheapId, middleId, expensiveId);

        assertThat(response)
                .extracting(ProductListItemResponse::price)
                .containsExactly(
                        10_000,
                        100_000,
                        300_000
                );
    }

    @Test
    void 할인_정렬을_선택하면_할인_상품만_조회한다() throws Exception {

        // given
        Long categoryId = createCategory("할인 상품");

        Long discountedProductId = createProduct(
                categoryId,
                "할인 상품",
                80_000,
                10,
                ProductStatus.ON_SALE,
                100_000
        );

        Long normalProductId = createProduct(
                categoryId,
                "일반 상품",
                50_000,
                10,
                ProductStatus.ON_SALE,
                null
        );

        // when
        List<ProductListItemResponse> response = requestProductList(
                "/api/v1/products?categoryId=" + categoryId + "&sort=sale"
        );

        // then
        assertThat(response)
                .extracting(ProductListItemResponse::id)
                .containsExactly(discountedProductId)
                .doesNotContain(normalProductId);

        assertThat(response.getFirst().discountPercent())
                .isPositive();
    }

    @Test
    void 공개되지_않은_상품은_목록에서_제외한다() throws Exception {

        // given
        Long categoryId = createCategory("공개 여부");

        Long publicProductId = createProduct(
                categoryId,
                "공개 상품",
                100_000,
                10,
                ProductStatus.ON_SALE,
                null
        );

        Long hiddenProductId = createProduct(
                categoryId,
                "숨김 상품",
                200_000,
                10,
                ProductStatus.HIDDEN,
                null
        );

        Long discontinuedProductId = createProduct(
                categoryId,
                "판매 종료 상품",
                300_000,
                10,
                ProductStatus.DISCONTINUED,
                null
        );

        // when
        List<ProductListItemResponse> response = requestProductList(
                "/api/v1/products?categoryId=" + categoryId
        );

        // then
        assertThat(response)
                .extracting(ProductListItemResponse::id)
                .containsExactly(publicProductId)
                .doesNotContain(
                        hiddenProductId,
                        discontinuedProductId
                );
    }

    @Test
    void 공개_상품_상세_조회_성공() throws Exception {

        // given
        Long categoryId = createCategory("상품 상세");

        Long productId = productService.create(
                categoryId,
                uniqueName("기계식 키보드"),
                99_000,
                10,
                "저소음 스위치를 적용한 무선 기계식 키보드입니다.",
                ProductStatus.ON_SALE,
                120_000,
                "/images/keyboard.jpg"
        );

        // when
        MvcResult result = mockMvc.perform(get("/api/v1/products/{productId}", productId))
                .andExpect(status().isOk())
                .andReturn();           // 검증이 끝난 요청의 전체 실행 결과 반환

        ProductDetailResponse response = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                ProductDetailResponse.class
        );

        // then
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.name()).contains("기계식 키보드");
        assertThat(response.price()).isEqualTo(99_000);
        assertThat(response.originalPrice()).isEqualTo(120_000);
        assertThat(response.discountPercent()).isPositive();
        assertThat(response.description())
                .isEqualTo(
                        "저소음 스위치를 적용한 무선 기계식 키보드입니다."
                );
        assertThat(response.imageUrl())
                .isEqualTo("/images/keyboard.jpg");
        assertThat(response.soldOut()).isFalse();
    }

    @Test
    void 공개되지_않은_상품_상세_조회_시_404를_반환한다()
            throws Exception {

        // given
        Long categoryId = createCategory("숨김 상세");

        Long hiddenProductId = createProduct(
                categoryId,
                "숨김 상세 상품",
                100_000,
                10,
                ProductStatus.HIDDEN,
                null
        );

        // when & then
        mockMvc.perform(
                        get(
                                "/api/v1/products/{productId}",
                                hiddenProductId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code")
                        .value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("존재하지 않는 상품입니다."))
                .andExpect(jsonPath("$.path")
                        .value(
                                "/api/v1/products/"
                                        + hiddenProductId
                        ));
    }

    @Test
    void 존재하지_않는_상품_상세_조회_시_404를_반환한다()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/products/{productId}",
                                Long.MAX_VALUE
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code")
                        .value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("존재하지 않는 상품입니다."))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void 상품_ID가_양수가_아니면_400을_반환한다()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/products/{productId}", 0)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message")
                        .value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void 지원하지_않는_정렬조건이면_400을_반환한다()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/products")
                                .param("sort", "unknown")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message")
                        .value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }


    private Long createCategory(String name) {
        return categoryService.create(uniqueName(name), null);
    }

    private Long createProduct(
            Long categoryId,
            String name,
            int price,
            int stock,
            ProductStatus status,
            Integer originalPrice
    ) {
        return productService.create(
                categoryId,
                uniqueName(name),
                price,
                stock,
                "상품 API 통합 테스트",
                status,
                originalPrice,
                "/images/no-image.jpg"
        );
    }

    private List<ProductListItemResponse> requestProductList(String path) throws Exception {

        MvcResult result = mockMvc.perform(get(path))   // 해당 경로로 가상의 GET 요청 보냄
                .andExpect(status().isOk())             // 응답 상태가 200인지 검증
                .andReturn();       // 검증이 끝난 요청의 전체 실행 결과 반환

        return objectMapper.readValue(  // objectMapper : JSON과 Java 객체 사이를 반환하는 Jackson의 핵심 객체 (즉 readValue면 JSON -> Jave 객체로 변환)
                result.getResponse().getContentAsByteArray(),   // 응답 본문을 byte[]로 가져온다.
                new TypeReference<>() {
                }    // 단일 객체가 아닌 목록 타입의 경우 런타임에 제네릭 타입 정보가 지워지는 타입 소거 특성이 있음.
                // 단순히 List.class만 전달하면 Jackson은 목록이라는 사실만 알 수 있고, 목록 안의 원소가 ProductListItemResponse라는 것은 알 수 없음.
                // 그래서 TypeReference로 전체 타입 정보를 전달한다. (다이아몬드 연산자는 메서드 반환 타입을 통해 타입 추론)
        );
    }

    private ProductListItemResponse findListItem(
            List<ProductListItemResponse> products,
            Long productId
    ) {
        return products.stream()
                .filter(product -> product.id().equals(productId))
                .findFirst()
                .orElseThrow();
    }

    private Long createChildCategory(String name, Long parentId) {
        return categoryService.create(uniqueName(name), parentId);
    }

    private String uniqueName(String prefix) {
        return prefix + "-" + System.nanoTime();
    }
}
