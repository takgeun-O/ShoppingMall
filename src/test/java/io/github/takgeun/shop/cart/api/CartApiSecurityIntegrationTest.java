package io.github.takgeun.shop.cart.api;

import io.github.takgeun.shop.IntegrationTestSupport;
import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
public class CartApiSecurityIntegrationTest extends IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Test
    void 비회원이_빈_장바구니를_조회하면_200을_반환한다() throws Exception {

        // given
        MockHttpSession session = new MockHttpSession();

        // when & then
        mockMvc.perform(
                        get("/api/v1/cart")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(content()
                        // "application/json;charset=UTF-8" 처럼 추가 파라미터가 붙으면 정확한 일치 검증은 실패할 수 있음.
                        // 그래서 contentTypeCompatibleWith() 사용
                        // 보통 일반적인 REST API 테스트에서는 이런 방식을 추천함 (테스트 목적이 JSON 응답 여부 확인이기 때문)
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.summary.subtotal")
                        .value(0))
                .andExpect(jsonPath("$.summary.discountTotal")
                        .value(0))
                .andExpect(jsonPath("$.summary.shippingFee")
                        .value(0))
                .andExpect(jsonPath("$.summary.totalPrice")
                        .value(0));
    }

    @Test
    void 상품을_추가하고_같은_세션으로_조회할_수_있다() throws Exception {

        // given
        Long categoryId =
                categoryService.create(
                        "장바구니 카테고리",
                        null
                );

        int originalStock = 10;

        Long productId = createProduct(
                categoryId,
                "무선 키보드",
                10_000,
                12_000,
                originalStock
        );

        MockHttpSession session =
                new MockHttpSession();

        // when : 장바구니에 2개 추가
        mockMvc.perform(
                        post("/api/v1/cart/items")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(addItemJson(
                                        productId,
                                        2
                                ))
                )
                .andExpect(status().isNoContent())
                // 참고로 204 No Content 응답은 본문이 없어서 Content-Type 자체가 설정되지 않는 것이 자연스러움
                // 따라서 아래와 같이 사용하고 보통 contentTypeCompatibleWith()를 사용하지 않음.
                .andExpect(content().string(""));

        // then : 동일한 세션으로 조회
        mockMvc.perform(
                        get("/api/v1/cart")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()")
                        .value(1))
                .andExpect(jsonPath("$.items[0].productId")
                        .value(productId))
                .andExpect(jsonPath("$.items[0].quantity")
                        .value(2))
                .andExpect(jsonPath("$.summary.subtotal")
                        .value(20_000))
                .andExpect(jsonPath("$.summary.discountTotal")
                        .value(4_000))
                .andExpect(jsonPath("$.summary.shippingFee")
                        .value(3_000))
                .andExpect(jsonPath("$.summary.totalPrice")
                        .value(23_000));

        /**
         * 장바구니 추가는 재고 예약이 아니므로
         * 실제 상품 재고가 감소하면 안된다.
         */
        Product product = productService.getPublicDetail(productId);

        assertThat(product.getStock())
                .isEqualTo(originalStock);
    }

    @Test
    void 같은_상품을_다시_추가하면_수량이_누적된다()
            throws Exception {

        // given
        Long categoryId =
                categoryService.create(
                        "수량누적 카테고리",
                        null
                );

        Long productId = createProduct(
                categoryId,
                "수량누적 상품",
                5_000,
                null,
                10
        );

        MockHttpSession session =
                new MockHttpSession();

        // 첫 번째 추가: 2개
        mockMvc.perform(
                        post("/api/v1/cart/items")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(addItemJson(
                                        productId,
                                        2
                                ))
                )
                .andExpect(status().isNoContent());

        // when: 같은 상품 3개 추가
        mockMvc.perform(
                        post("/api/v1/cart/items")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(addItemJson(
                                        productId,
                                        3
                                ))
                )
                .andExpect(status().isNoContent());

        // then: 최종 수량 5개
        mockMvc.perform(
                        get("/api/v1/cart")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()")
                        .value(1))
                .andExpect(jsonPath("$.items[0].productId")
                        .value(productId))
                .andExpect(jsonPath("$.items[0].quantity")
                        .value(5))
                .andExpect(jsonPath("$.summary.subtotal")
                        .value(25_000))
                .andExpect(jsonPath("$.summary.shippingFee")
                        .value(3_000))
                .andExpect(jsonPath("$.summary.totalPrice")
                        .value(28_000));

        // 장바구니 추가만으로 상품 재고는 감소하지 않음
        assertThat(
                productService
                        .getPublicDetail(productId)
                        .getStock()
        ).isEqualTo(10);
    }

    @Test
    void 서로_다른_세션의_장바구니는_분리된다()
            throws Exception {

        // given
        Long categoryId =
                categoryService.create(
                        "세션분리 카테고리",
                        null
                );

        Long productId = createProduct(
                categoryId,
                "세션분리 상품",
                10_000,
                null,
                10
        );

        MockHttpSession firstSession =
                new MockHttpSession();

        MockHttpSession secondSession =
                new MockHttpSession();

        // 첫 번째 세션에만 상품 추가
        mockMvc.perform(
                        post("/api/v1/cart/items")
                                .with(csrf())
                                .session(firstSession)
                                .contentType(APPLICATION_JSON)
                                .content(addItemJson(
                                        productId,
                                        2
                                ))
                )
                .andExpect(status().isNoContent());

        // 첫 번째 세션에는 상품이 존재
        mockMvc.perform(
                        get("/api/v1/cart")
                                .session(firstSession)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()")
                        .value(1))
                .andExpect(jsonPath("$.items[0].productId")
                        .value(productId))
                .andExpect(jsonPath("$.items[0].quantity")
                        .value(2));

        // 두 번째 세션은 빈 장바구니
        mockMvc.perform(
                        get("/api/v1/cart")
                                .session(secondSession)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void 장바구니_상품의_수량을_변경하고_0으로_변경하면_삭제한다()
            throws Exception {

        // given
        Long categoryId =
                categoryService.create(
                        "수량변경 카테고리",
                        null
                );

        Long productId = createProduct(
                categoryId,
                "수량변경 상품",
                10_000,
                null,
                10
        );

        MockHttpSession session =
                new MockHttpSession();

        mockMvc.perform(
                        post("/api/v1/cart/items")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(addItemJson(
                                        productId,
                                        2
                                ))
                )
                .andExpect(status().isNoContent());

        // when: 수량을 5개로 변경
        mockMvc.perform(
                        patch(
                                "/api/v1/cart/items/{productId}",
                                productId
                        )
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(quantityJson(5))
                )
                .andExpect(status().isNoContent());

        // then
        mockMvc.perform(
                        get("/api/v1/cart")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()")
                        .value(1))
                .andExpect(jsonPath("$.items[0].quantity")
                        .value(5));

        // when: 수량을 0으로 변경
        mockMvc.perform(
                        patch(
                                "/api/v1/cart/items/{productId}",
                                productId
                        )
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(quantityJson(0))
                )
                .andExpect(status().isNoContent());

        // then: 장바구니에서 삭제됨
        mockMvc.perform(
                        get("/api/v1/cart")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void 장바구니에서_상품을_삭제한다()
            throws Exception {

        // given
        Long categoryId =
                categoryService.create(
                        "단일삭제 카테고리",
                        null
                );

        Long productId = createProduct(
                categoryId,
                "단일삭제 상품",
                10_000,
                null,
                10
        );

        MockHttpSession session =
                new MockHttpSession();

        mockMvc.perform(
                        post("/api/v1/cart/items")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(addItemJson(
                                        productId,
                                        2
                                ))
                )
                .andExpect(status().isNoContent());

        // when
        mockMvc.perform(
                        delete(
                                "/api/v1/cart/items/{productId}",
                                productId
                        )
                                .with(csrf())
                                .session(session)
                )
                .andExpect(status().isNoContent());

        // then
        mockMvc.perform(
                        get("/api/v1/cart")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void 장바구니를_전체_비운다()
            throws Exception {

        // given
        Long categoryId =
                categoryService.create(
                        "전체삭제 카테고리",
                        null
                );

        Long firstProductId = createProduct(
                categoryId,
                "첫 번째 상품",
                10_000,
                null,
                10
        );

        Long secondProductId = createProduct(
                categoryId,
                "두 번째 상품",
                20_000,
                null,
                10
        );

        MockHttpSession session =
                new MockHttpSession();

        addItem(
                session,
                firstProductId,
                1
        );

        addItem(
                session,
                secondProductId,
                1
        );

        // when
        mockMvc.perform(
                        delete("/api/v1/cart")
                                .with(csrf())
                                .session(session)
                )
                .andExpect(status().isNoContent());

        // then
        mockMvc.perform(
                        get("/api/v1/cart")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.summary.totalPrice")
                        .value(0));
    }

    @Test
    void 재고보다_많은_수량을_담으면_409를_반환하고_장바구니를_변경하지_않는다()
            throws Exception {

        // given
        Long categoryId =
                categoryService.create(
                        "재고초과 카테고리",
                        null
                );

        Long productId = createProduct(
                categoryId,
                "재고초과 상품",
                10_000,
                null,
                3
        );

        MockHttpSession session =
                new MockHttpSession();

        // when & then
        mockMvc.perform(
                        post("/api/v1/cart/items")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(addItemJson(
                                        productId,
                                        4
                                ))
                )
                .andExpect(status().isConflict())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status")
                        .value(409))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/cart/items"));

        // 실패한 상품이 장바구니에 저장되지 않았는지 확인
        mockMvc.perform(
                        get("/api/v1/cart")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        // 상품 DB의 실제 재고도 변경되지 않음
        assertThat(
                productService
                        .getPublicDetail(productId)
                        .getStock()
        ).isEqualTo(3);
    }

    @Test
    void 존재하지_않는_상품을_담으면_404를_반환한다()
            throws Exception {

        // given
        MockHttpSession session =
                new MockHttpSession();

        long missingProductId = 999_999_999L;

        // when & then
        mockMvc.perform(
                        post("/api/v1/cart/items")
                                .with(csrf())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(addItemJson(
                                        missingProductId,
                                        1
                                ))
                )
                .andExpect(status().isNotFound())
                .andExpect(content()
                        .contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status")
                        .value(404))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/cart/items"));

        mockMvc.perform(
                        get("/api/v1/cart")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void CSRF_토큰_없이_상품을_추가하면_403을_반환한다()
            throws Exception {

        // given
        Long categoryId =
                categoryService.create(
                        "CSRF 카테고리",
                        null
                );

        Long productId = createProduct(
                categoryId,
                "CSRF 검증 상품",
                10_000,
                null,
                10
        );

        MockHttpSession session =
                new MockHttpSession();

        // when & then
        mockMvc.perform(
                        post("/api/v1/cart/items")
                                // .with(csrf())를 의도적으로 제외
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(addItemJson(
                                        productId,
                                        1
                                ))
                )
                .andExpect(status().isForbidden());

        // Controller에 진입하지 않아 장바구니가 비어 있어야 함
        mockMvc.perform(
                        get("/api/v1/cart")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }


    private Long createProduct(
            Long categoryId,
            String name,
            int price,
            Integer originalPrice,
            int stock
    ) {
        Product product = Product.create(
                categoryId,
                name,
                price,
                stock,
                "통합 테스트 상품",
                ProductStatus.ON_SALE,
                originalPrice,
                "/images/cart-test.jpg"
        );

        productService.save(product);

        return product.getId();
    }

    private String addItemJson(Long productId, int quantity) {
        return """
                {
                    "productId": %d,
                    "quantity": %d
                }
                """.formatted(
                productId,
                quantity
        );
    }

    private String quantityJson(int quantity) {
        return """
                {
                    "quantity": %d
                }
                """.formatted(quantity);
    }

    private void addItem(MockHttpSession session, Long productId, int quantity) throws Exception {

        mockMvc.perform(
                post("/api/v1/cart/items")
                        .with(csrf())
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(addItemJson(
                                productId,
                                quantity
                        ))
        )
                .andExpect(status().isNoContent());
    }
}
