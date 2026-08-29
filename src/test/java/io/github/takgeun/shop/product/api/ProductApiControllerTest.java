package io.github.takgeun.shop.product.api;

import io.github.takgeun.shop.global.error.api.ApiGlobalExceptionHandler;
import io.github.takgeun.shop.global.error.code.ErrorCode;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 요청 경로와 파라미터 바인딩 검증
 * Product -> Response DTO 변환
 * JSON 응답 구조
 * 검증 실패 및 공통 오류 응답
 * 서비스 메서드 호출 여부
 */
class ProductApiControllerTest {

    private ProductService productService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        /**
         * ProductApiController만 독립적으로 테스트할 수 있는 작은 Spring MVC 환경을 직접 구성
         * 가짜 ProductService
         * -> ProductApiController
         * -> MockMvc
         *      - Bean Validation
         *      - API 공통 예외 처리기
         */

        productService = mock(ProductService.class);

        // Spring ApplicationContext를 사용하지 않기 때문에 @Autowired가 아니라 직접 new 생성
        ProductApiController controller = new ProductApiController(productService);

        // Spring MVC에서 Jakarta Bean Validation을 사용할 수 있게 연결하는 Spring용 Validator 등록
        // 독립형 MockMvc에서 검증 애노테이션이 작동되도록 하기 위해서 직접 Validator 등록한 것
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

        // LocalValidatorFactoryBean의 초기화 메서드
        // Spring Context 없이 작동하므로 new로 직접 생성했기에 Spring이 해주던 초기화 작업을 직접 실행한 것.
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ApiGlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void 공개_상품_목록_조회_성공() throws Exception {

        // given
        Product keyboard = product(
                1L,
                10L,
                "무선 기계식 키보드",
                99_000,
                120_000,
                "저소음 무선 키보드",
                "/images/keyboard.jpg",
                4.5,
                false
        );

        Product mouse = product(
                2L,
                10L,
                "무선 마우스",
                39_000,
                null,
                "인체공학 무선 마우스",
                "/images/mouse.jpg",
                4.2,
                false
        );

        when(productService.findPublicList(null, "latest"))
                .thenReturn(List.of(keyboard, mouse));

        // when & then
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))

                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].categoryId").value(10))
                .andExpect(jsonPath("$[0].name")
                        .value("무선 기계식 키보드"))
                .andExpect(jsonPath("$[0].price").value(99_000))
                .andExpect(jsonPath("$[0].originalPrice")
                        .value(120_000))
                .andExpect(jsonPath("$[0].discountPercent")
                        .value(18))
                .andExpect(jsonPath("$[0].imageUrl")
                        .value("/images/keyboard.jpg"))
                .andExpect(jsonPath("$[0].rating").value(4.5))
                .andExpect(jsonPath("$[0].soldOut").value(false))

                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].originalPrice")
                        .doesNotExist())
                .andExpect(jsonPath("$[1].discountPercent")
                        .value(0))

                // 목록 응답에는 상세 설명과 내부 관리 정보가 없어야 한다.
                .andExpect(jsonPath("$[0].description")
                        .doesNotExist())
                .andExpect(jsonPath("$[0].stock")
                        .doesNotExist())
                .andExpect(jsonPath("$[0].status")
                        .doesNotExist());

        // Mockito를 이용한 테스트 실행 중 productService.findPublicList()가 실제로 한번만 호출됐는지 검증
        verify(productService)
                .findPublicList(null, "latest");
    }

    @Test
    void 공개_상품_목록이_없으면_빈_배열을_반환한다() throws Exception {

        // given
        when(productService.findPublicList(null, "latest"))
                .thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(productService)
                .findPublicList(null, "latest");
    }

    @Test
    void 카테고리와_정렬조건으로_공개_상품을_조회한다() throws Exception {

        // given
        Long categoryId = 10L;

        when(productService.findPublicList(
                categoryId, "price-low"
        )).thenReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/products")
                        .param("categoryId", "10")
                        .param("sort", "price-low"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(productService)
                .findPublicList(categoryId, "price-low");
    }

    @Test
    void 공개_상품_상세_조회_성공() throws Exception {

        // given
        Long productId = 1L;

        Product product = product(
                productId,
                10L,
                "무선 기계식 키보드",
                99_000,
                120_000,
                "저소음 스위치를 적용한 무선 키보드입니다.",
                "/images/keyboard.jpg",
                4.5,
                false
        );

        when(productService.getPublicDetail(productId))
                .thenReturn(product);

        // when & then
        mockMvc.perform(get(
                        "/api/v1/products/{productId}",
                        productId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.categoryId").value(10))
                .andExpect(jsonPath("$.name")
                        .value("무선 기계식 키보드"))
                .andExpect(jsonPath("$.price").value(99_000))
                .andExpect(jsonPath("$.originalPrice")
                        .value(120_000))
                .andExpect(jsonPath("$.discountPercent")
                        .value(18))
                .andExpect(jsonPath("$.description")
                        .value("저소음 스위치를 적용한 무선 키보드입니다."))
                .andExpect(jsonPath("$.imageUrl")
                        .value("/images/keyboard.jpg"))
                .andExpect(jsonPath("$.rating").value(4.5))
                .andExpect(jsonPath("$.soldOut").value(false))

                // 공개 상세 응답에서 내부 정보 제외
                .andExpect(jsonPath("$.stock").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist());

        verify(productService).getPublicDetail(productId);
    }

    @Test
    void 공개_상품_상세_조회_실패_상품이_없음() throws Exception {
        // given
        Long productId = 999L;

        when(productService.getPublicDetail(productId))
                .thenThrow(new NotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND
                ));

        // when & then
        mockMvc.perform(get(
                        "/api/v1/products/{productId}",
                        productId
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code")
                        .value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("존재하지 않는 상품입니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/products/999"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());

        verify(productService).getPublicDetail(productId);
    }

    @Test
    void 상품_ID_타입이_잘못되면_400을_반환한다() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/products/{productId}",
                        "not-a-number"
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message")
                        .value("요청 값의 형식이 올바르지 않습니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/products/not-a-number"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());

        verifyNoInteractions(productService);
    }

    @Test
    void 상품_ID가_양수가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/products/{productId}",
                        0
                ))
//                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message")
                        .value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.fieldErrors").isArray());

        verifyNoInteractions(productService);
    }

    @Test
    void 카테고리_ID가_양수가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("categoryId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message")
                        .value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.fieldErrors").isArray());

        verifyNoInteractions(productService);
    }

    @Test
    void 지원하지_않는_정렬조건이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("sort", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message")
                        .value("요청 값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.fieldErrors").isArray());

        verifyNoInteractions(productService);
    }

    /**
     * Controller의 "Product -> Response" 변환을 검증하기 위한 테스트용 Product mock 객체
     * <p>
     * 상품 생성 및 상태 변경 규칙은 ProductTest와 ProductServiceTest에서 별도로 검증
     */
    private Product product(
            Long id,
            Long categoryId,
            String name,
            int price,
            Integer originalPrice,
            String description,
            String imageUrl,
            double rating,
            boolean soldOut
    ) {
        Product product = mock(Product.class);

        when(product.getId()).thenReturn(id);
        when(product.getCategoryId()).thenReturn(categoryId);
        when(product.getName()).thenReturn(name);
        when(product.getPrice()).thenReturn(price);
        when(product.getOriginalPrice()).thenReturn(originalPrice);
        when(product.getDescription()).thenReturn(description);
        when(product.getImageUrl()).thenReturn(imageUrl);
        when(product.getRatingValue()).thenReturn(rating);
        when(product.isSoldOut()).thenReturn(soldOut);

        int discountPercent = calculateDiscountPercent(
                price,
                originalPrice
        );

        when(product.discountPercent())
                .thenReturn(discountPercent);

        return product;
    }

    private int calculateDiscountPercent(int price, Integer originalPrice) {
        if (originalPrice == null || originalPrice <= price) {
            return 0;
        }

        return (int) Math.round((originalPrice - price) * 100.0 / originalPrice);
    }
}