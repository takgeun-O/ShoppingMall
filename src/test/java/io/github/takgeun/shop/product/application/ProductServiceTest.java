package io.github.takgeun.shop.product.application;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.infra.memory.MemoryCategoryRepository;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import io.github.takgeun.shop.product.infra.memory.MemoryProductRepository;
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
        Long productId = productService.create(
                categoryId,
                "맥북 파우치",
                39000,
                10,
                "튼튼한 파우치",
                ProductStatus.ON_SALE,
                null,
                "https://example.com/image.jpg"
        );

        // then
        assertNotNull(productId);

        Product product = productService.getForDetail(true, productId);
        assertEquals("맥북 파우치", product.getName());
        assertEquals(categoryId, product.getCategoryId());
        assertEquals(39000, product.getPrice());
        assertEquals(10, product.getStock());
        assertEquals("튼튼한 파우치", product.getDescription());
        assertEquals("https://example.com/image.jpg", product.getImageUrl());
        assertEquals(ProductStatus.ON_SALE, product.getStatus());
        assertNull(product.getOriginalPrice());
    }

    @Test
    void 상품_생성_성공_정가포함() {
        // given
        Long categoryId = categoryService.create("전자", null);

        // when
        Long productId = productService.create(
                categoryId,
                "맥북 파우치",
                39000,
                10,
                "튼튼한 파우치",
                ProductStatus.ON_SALE,
                49000,
                "https://example.com/image.jpg"
        );

        // then
        Product product = productService.getForDetail(true, productId);
        assertEquals(49000, product.getOriginalPrice());
    }

    @Test
    void 상품_생성_실패_정가가_판매가보다_작음() {
        // given
        Long categoryId = categoryService.create("전자", null);

        // when & then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                productService.create(
                        categoryId,
                        "맥북 파우치",
                        39000,
                        10,
                        "튼튼한 파우치",
                        ProductStatus.ON_SALE,
                        30000,
                        "https://example.com/image.jpg"
                )
        );

        assertEquals("정가는 판매가 이상이어야 합니다.", e.getMessage());
    }

    @Test
    void 상품_단건_조회_성공() {
        // given
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(
                categoryId,
                "맥북 파우치",
                39000,
                10,
                "튼튼한 파우치",
                ProductStatus.ON_SALE,
                null,
                "https://example.com/image.jpg"
        );

        // when
        Product product = productService.getForDetail(false, productId);

        // then
        assertEquals("맥북 파우치", product.getName());
        assertEquals("https://example.com/image.jpg", product.getImageUrl());
    }

    @Test
    void 상품_단건_조회_실패_상품_없음() {
        // when & then
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> productService.getForDetail(false, 999L));
        assertEquals("존재하지 않는 상품입니다.", e.getMessage());
    }

    @Test
    void 카테고리별_상품_목록_조회_성공_공개용() {
        // given
        Long categoryId = categoryService.create("전자", null);
        Long productId1 = productService.create(
                categoryId, "맥북 파우치", 39000, 10, "튼튼한 파우치",
                ProductStatus.ON_SALE, null, "https://example.com/1.jpg"
        );
        productService.create(
                categoryId, "삼성 파우치", 20000, 20, "좋은 파우치",
                ProductStatus.HIDDEN, null, "https://example.com/2.jpg"
        );
        productService.create(
                categoryId, "비공개 상품", 20000, 20, "숨김용",
                ProductStatus.DISCONTINUED, null, "https://example.com/3.jpg"
        );

        // when
        List<Product> productList = productService.findForList(false, categoryId, "latest");

        // then
        assertEquals(1, productList.size());
        assertTrue(productList.stream().allMatch(p -> p.getCategoryId().equals(categoryId)));
        assertEquals(productId1, productList.get(0).getId());
    }

    @Test
    void 카테고리별_상품_목록_조회_성공_관리자용() {
        // given
        Long categoryId = categoryService.create("전자", null);
        productService.create(
                categoryId, "맥북 파우치", 39000, 10, "튼튼한 파우치",
                ProductStatus.ON_SALE, null, "https://example.com/1.jpg"
        );
        productService.create(
                categoryId, "삼성 파우치", 20000, 20, "좋은 파우치",
                ProductStatus.HIDDEN, null, "https://example.com/2.jpg"
        );
        productService.create(
                categoryId, "비공개 상품", 20000, 20, "숨김용",
                ProductStatus.DISCONTINUED, null, "https://example.com/3.jpg"
        );

        // when
        List<Product> productList = productService.findForList(true, categoryId, "latest");

        // then
        assertEquals(3, productList.size());
        assertTrue(productList.stream().allMatch(p -> p.getCategoryId().equals(categoryId)));
    }

    @Test
    void 상품_수정_성공_이름_가격_재고_이미지() {
        // given
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(
                categoryId,
                "맥북 파우치",
                39000,
                10,
                "튼튼한 파우치",
                ProductStatus.ON_SALE,
                null,
                "https://example.com/old.jpg"
        );

        // when
        productService.update(
                productId,
                null,
                "맥북 파우치2",
                40000,
                20,
                null,
                null,
                null,
                "https://example.com/new.jpg"
        );

        // then
        Product updated = productService.getForDetail(true, productId);
        assertEquals("맥북 파우치2", updated.getName());
        assertEquals(40000, updated.getPrice());
        assertEquals(20, updated.getStock());
        assertEquals("튼튼한 파우치", updated.getDescription());
        assertEquals("https://example.com/new.jpg", updated.getImageUrl());
        assertEquals(ProductStatus.ON_SALE, updated.getStatus());
    }

    @Test
    void 상품_수정_성공_카테고리_이동() {
        // given
        Long electronicsId = categoryService.create("전자", null);
        Long fashionId = categoryService.create("패션", null);

        Long productId = productService.create(
                electronicsId,
                "맥북 파우치",
                39000,
                10,
                "튼튼한 파우치",
                ProductStatus.ON_SALE,
                null,
                "https://example.com/image.jpg"
        );

        // when
        productService.update(
                productId,
                fashionId,
                "맥북 파우치2",
                40000,
                20,
                null,
                null,
                null,
                "https://example.com/image2.jpg"
        );

        // then
        Product updated = productService.getForDetail(true, productId);
        assertEquals(fashionId, updated.getCategoryId());
        assertEquals("맥북 파우치2", updated.getName());
        assertEquals("https://example.com/image2.jpg", updated.getImageUrl());
    }

    @Test
    void 상품_수정_성공_정가_변경() {
        // given
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(
                categoryId,
                "맥북 파우치",
                39000,
                10,
                "튼튼한 파우치",
                ProductStatus.ON_SALE,
                45000,
                "https://example.com/image.jpg"
        );

        // when
        productService.update(
                productId,
                null,
                null,
                null,
                null,
                null,
                null,
                50000,
                null
        );

        // then
        Product updated = productService.getForDetail(true, productId);
        assertEquals(50000, updated.getOriginalPrice());
    }

    @Test
    void 상품_수정_성공_상태_변경() {
        // given
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(
                categoryId,
                "맥북 파우치",
                39000,
                10,
                "튼튼한 파우치",
                ProductStatus.READY,
                null,
                "https://example.com/image.jpg"
        );

        // when
        productService.update(
                productId,
                null,
                null,
                null,
                null,
                null,
                ProductStatus.HIDDEN,
                null,
                null
        );

        // then
        Product updated = productService.getForDetail(true, productId);
        assertEquals(ProductStatus.HIDDEN, updated.getStatus());
    }

    @Test
    void 상품_수정_성공_이미지_제거() {
        // given
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(
                categoryId,
                "맥북 파우치",
                39000,
                10,
                "튼튼한 파우치",
                ProductStatus.ON_SALE,
                null,
                "https://example.com/image.jpg"
        );

        // when
        productService.update(
                productId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ""
        );

        // then
        Product updated = productService.getForDetail(true, productId);
        assertNull(updated.getImageUrl());
    }

    @Test
    void 상품_수정_실패_상품_없음() {
        // given
        Long categoryId = categoryService.create("전자", null);

        // when & then
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> productService.update(
                        999L,
                        categoryId,
                        "연필",
                        1000,
                        2,
                        "desc",
                        ProductStatus.ON_SALE,
                        null,
                        "https://example.com/image.jpg"
                ));

        assertEquals("존재하지 않는 상품입니다.", e.getMessage());
    }

    @Test
    void 상품_숨김_성공() {
        // given
        Long electronicsId = categoryService.create("전자", null);
        Long productId = productService.create(
                electronicsId,
                "맥북",
                2_000_000,
                20,
                "빠른 맥북",
                ProductStatus.ON_SALE,
                null,
                "https://example.com/image.jpg"
        );

        // when
        productService.changeStatus(productId, ProductStatus.HIDDEN);

        // then
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> productService.getForDetail(false, productId));
        assertEquals("존재하지 않는 상품입니다.", e.getMessage());
    }

    @Test
    void 상품_숨김_실패_상품_없음() {
        // when & then
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> productService.changeStatus(999L, ProductStatus.HIDDEN));

        assertEquals("존재하지 않는 상품입니다.", e.getMessage());
    }

    @Test
    void 재고가_0이_되면_공개목록에서는_조회된다_SOLD_OUT() {
        // given
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(
                categoryId,
                "품절 예정 상품",
                10000,
                1,
                "설명",
                ProductStatus.ON_SALE,
                null,
                "https://example.com/image.jpg"
        );

        Product product = productService.getForDetail(true, productId);
        product.decreaseStock(1);
        productService.save(product);

        // when
        List<Product> productList = productService.findForList(false, categoryId, "latest");

        // then
        assertEquals(ProductStatus.SOLD_OUT, productService.getForDetail(true, productId).getStatus());
        assertTrue(productList.stream().anyMatch(p -> p.getId().equals(productId)));
    }

    @Test
    void 판매종료_상품은_공개상세조회_불가() {
        // given
        Long categoryId = categoryService.create("전자", null);
        Long productId = productService.create(
                categoryId,
                "단종 상품",
                10000,
                10,
                "설명",
                ProductStatus.ON_SALE,
                null,
                "https://example.com/image.jpg"
        );

        productService.changeStatus(productId, ProductStatus.DISCONTINUED);

        // when & then
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> productService.getForDetail(false, productId));

        assertEquals("존재하지 않는 상품입니다.", e.getMessage());
    }
}