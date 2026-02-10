package io.github.takgeun.shop.product.application;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.infra.MemoryCategoryRepository;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import io.github.takgeun.shop.product.dto.request.ProductUpdateRequest;
import io.github.takgeun.shop.product.infra.MemoryProductRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceTest {
    private ProductService productService;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        MemoryCategoryRepository categoryRepository = new MemoryCategoryRepository();
        MemoryProductRepository productRepository = new MemoryProductRepository();

        categoryService = new CategoryService(categoryRepository, productRepository);
        productService = new ProductService(productRepository, categoryService);
    }

    @Test
    void 상품_생성_성공() {
        // given
        Long categoryId = categoryService.create("전자", null);

        // when
        Long productId = productService.create(categoryId, "맥북 파우치", 39000, 10, "튼튼한 파우치");

        // then
        assertNotNull(productId);
        Product product = productService.get(productId);
        assertEquals("맥북 파우치", product.getName());
        assertEquals(categoryId, product.getCategoryId());
    }

    @Test
    void 존재하지_않는_카테고리면_상품_생성_실패() {
        // when & then
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> productService.create(999L, "맥북 파우치", 39000, 10, "튼튼한 파우치"));

        assertEquals("카테고리가 존재하지 않습니다.", e.getMessage());
    }

    @Test
    void 상품_단건_조회_성공() {
        // given
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(categoryId, "맥북 파우치", 39000, 10, "튼튼한 파우치");

        // when
        Product product1 = productService.get(productId);

        // then
        Assertions.assertThat(product1.getName()).isEqualTo("맥북 파우치");
    }

    @Test
    void 상품_단건_조회_실패_상품_없음() {
        // when & then
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> productService.get(999L));
        assertEquals("상품이 존재하지 않습니다.", e.getMessage());
    }

    @Test
    void 카테고리별_상품_목록_조회_성공_공개용() {

        // given
        Long categoryId = categoryService.create("전자", null);
        Long productId1 = productService.create(categoryId, "맥북 파우치", 39000, 10, "튼튼한 파우치");
        Long productId2 = productService.create(categoryId, "삼성 파우치", 20000, 20, "좋은 파우치");
        Long productId3 = productService.create(categoryId, "비활성화템", 20000, 20, "비활성화된거");

        Product product1 = productService.get(productId1);
        Product product2 = productService.get(productId2);
        Product product3 = productService.get(productId3);
        product1.onSale();          // ON_SALE
        product2.changeStock(0);    // SOLD_OUT
        product3.discontinue();     // DISCONTINUE

        // when
        List<Product> productList = productService.getAllPublicByCategoryId(categoryId);

        // then
        assertEquals(1, productList.size());
        assertTrue(productList.stream()
                .allMatch(p -> p.getCategoryId().equals(categoryId)));
    }

    @Test
    void 카테고리별_상품_목록_조회_성공_관리자용() {

        // given
        Long categoryId = categoryService.create("전자", null);
        Long productId1 = productService.create(categoryId, "맥북 파우치", 39000, 10, "튼튼한 파우치");
        Long productId2 = productService.create(categoryId, "삼성 파우치", 20000, 20, "좋은 파우치");
        Long productId3 = productService.create(categoryId, "비활성화템", 20000, 20, "비활성화된거");

        Product product1 = productService.get(productId1);
        Product product2 = productService.get(productId2);
        Product product3 = productService.get(productId3);
        product1.onSale();          // ON_SALE
        product2.changeStock(0);    // SOLD_OUT
        product3.discontinue();     // DISCONTINUE

        // when
        List<Product> productList = productService.getByCategoryAdmin(categoryId);

        // then
        assertEquals(3, productList.size());
        assertTrue(productList.stream()
                .allMatch(p -> p.getCategoryId().equals(categoryId)));
    }

    @Test
    void 카테고리별_상품_목록_조회_실패_공개용_카테고리_없음() {
        // when & then
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> productService.getAllPublicByCategoryId(999L));
        assertEquals("카테고리가 존재하지 않습니다.", e.getMessage());
    }

    @Test
    void 상품_수정_성공_이름_가격_재고() {
        // given
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(categoryId, "맥북 파우치", 39000, 10, "튼튼한 파우치");

        ProductUpdateRequest request = ProductUpdateRequest.of(
                null,
                "맥북 파우치2",
                40000,
                20,
                null
        );
        // when
        productService.update(productId, request.getCategoryId(), request.getName(),
                request.getPrice(), request.getStock(), request.getDescription());

        // then
        Product updated = productService.get(productId);
        assertEquals("맥북 파우치2", updated.getName());
        assertEquals(40000, updated.getPrice());
        assertEquals(20, updated.getStock());
        assertEquals("튼튼한 파우치", updated.getDescription());
    }

    @Test
    void 상품_수정_성공_카테고리_이동() {
        // given
        Long electronicsId = categoryService.create("전자", null);
        Long fashionId = categoryService.create("패션", null);

        Long productId = productService.create(electronicsId, "맥북 파우치", 39000, 10, "튼튼한 파우치");

        ProductUpdateRequest request = ProductUpdateRequest.of(
                fashionId,
                "맥북 파우치2",
                40000,
                20,
                null
        );

        // when
        productService.update(productId, request.getCategoryId(), request.getName(),
                request.getPrice(), request.getStock(), request.getDescription());

        // then
        Product updated = productService.get(productId);
        assertEquals(fashionId, updated.getCategoryId());
    }

    @Test
    void 상품_수정_실패_상품_없음() {
        // given
        Long categoryId = categoryService.create("전자", null);

        ProductUpdateRequest request = ProductUpdateRequest.of(
                categoryId,
                "연필",
                1000,
                2,
                "desc"
        );

        // when
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> productService.update(999L, request.getCategoryId(), request.getName(),
                        request.getPrice(), request.getStock(), request.getDescription()));

        // then
        assertEquals("상품이 존재하지 않습니다.", e.getMessage());
    }

    @Test
    void 상품_수정_실패_변경하려는_카테고리_없음() {
        // given
        Long electronicsId = categoryService.create("전자", null);
        Long productId = productService.create(electronicsId, "맥북", 2_000_000, 20, "빠른 맥북");

        ProductUpdateRequest request = ProductUpdateRequest.of(
                999L,
                null,
                null,
                null,
                null
        );

        // when
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> productService.update(productId, request.getCategoryId(), request.getName(),
                        request.getPrice(), request.getStock(), request.getDescription()));

        // then
        assertEquals("카테고리가 존재하지 않습니다.", e.getMessage());
    }

    @Test
    void 상품_숨김_성공() {
        // given
        Long electronicsId = categoryService.create("전자", null);
        Long productId = productService.create(electronicsId, "맥북", 2_000_000, 20, "빠른 맥북");

        // when
        productService.changeStatus(productId, ProductStatus.HIDDEN);

        // then
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> productService.getPublic(productId));
        assertEquals("상품이 존재하지 않습니다.", e.getMessage());
    }

    @Test
    void 상품_숨김_실패_상품_없음() {
        // when
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> productService.changeStatus(999L, ProductStatus.HIDDEN));

        // then
        assertEquals("상품이 존재하지 않습니다.", e.getMessage());
    }
}