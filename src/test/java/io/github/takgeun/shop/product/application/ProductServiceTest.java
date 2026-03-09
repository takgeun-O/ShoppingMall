package io.github.takgeun.shop.product.application;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.category.infra.MemoryCategoryRepository;
import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.product.domain.Product;
import io.github.takgeun.shop.product.domain.ProductStatus;
import io.github.takgeun.shop.product.dto.request.ProductUpdateRequest;
import io.github.takgeun.shop.product.infra.MemoryProductRepository;
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
        Long productId1 = productService.create(categoryId, "맥북 파우치", 39000, 10, "튼튼한 파우치", "https://example.com/1.jpg");
        Long productId2 = productService.create(categoryId, "삼성 파우치", 20000, 20, "좋은 파우치", "https://example.com/2.jpg");
        Long productId3 = productService.create(categoryId, "비공개 상품", 20000, 20, "숨김용", "https://example.com/3.jpg");

        productService.changeStatus(productId1, ProductStatus.ON_SALE);
        productService.changeStatus(productId2, ProductStatus.HIDDEN);
        productService.changeStatus(productId3, ProductStatus.DISCONTINUED);

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
        Long productId1 = productService.create(categoryId, "맥북 파우치", 39000, 10, "튼튼한 파우치", "https://example.com/1.jpg");
        Long productId2 = productService.create(categoryId, "삼성 파우치", 20000, 20, "좋은 파우치", "https://example.com/2.jpg");
        Long productId3 = productService.create(categoryId, "비공개 상품", 20000, 20, "숨김용", "https://example.com/3.jpg");

        productService.changeStatus(productId1, ProductStatus.ON_SALE);
        productService.changeStatus(productId2, ProductStatus.HIDDEN);
        productService.changeStatus(productId3, ProductStatus.DISCONTINUED);

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
                "https://example.com/old.jpg"
        );

        ProductUpdateRequest request = ProductUpdateRequest.of(
                null,
                "맥북 파우치2",
                40000,
                20,
                null,
                "https://example.com/new.jpg"
        );

        // when
        productService.update(
                productId,
                request.getCategoryId(),
                request.getName(),
                request.getPrice(),
                request.getStock(),
                request.getDescription(),
                request.getImageUrl()
        );

        // then
        Product updated = productService.getForDetail(true, productId);
        assertEquals("맥북 파우치2", updated.getName());
        assertEquals(40000, updated.getPrice());
        assertEquals(20, updated.getStock());
        assertEquals("튼튼한 파우치", updated.getDescription());
        assertEquals("https://example.com/new.jpg", updated.getImageUrl());
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
                "https://example.com/image.jpg"
        );

        ProductUpdateRequest request = ProductUpdateRequest.of(
                fashionId,
                "맥북 파우치2",
                40000,
                20,
                null,
                "https://example.com/image2.jpg"
        );

        // when
        productService.update(
                productId,
                request.getCategoryId(),
                request.getName(),
                request.getPrice(),
                request.getStock(),
                request.getDescription(),
                request.getImageUrl()
        );

        // then
        Product updated = productService.getForDetail(true, productId);
        assertEquals(fashionId, updated.getCategoryId());
        assertEquals("https://example.com/image2.jpg", updated.getImageUrl());
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
                "https://example.com/image.jpg"
        );

        ProductUpdateRequest request = ProductUpdateRequest.of(
                null,
                null,
                null,
                null,
                null,
                ""
        );

        // when
        productService.update(
                productId,
                request.getCategoryId(),
                request.getName(),
                request.getPrice(),
                request.getStock(),
                request.getDescription(),
                request.getImageUrl()
        );

        // then
        Product updated = productService.getForDetail(true, productId);
        assertNull(updated.getImageUrl());
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
                "desc",
                "https://example.com/image.jpg"
        );

        // when & then
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> productService.update(
                        999L,
                        request.getCategoryId(),
                        request.getName(),
                        request.getPrice(),
                        request.getStock(),
                        request.getDescription(),
                        request.getImageUrl()
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
                "https://example.com/image.jpg"
        );

        productService.changeStatus(productId, ProductStatus.DISCONTINUED);

        // when & then
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> productService.getForDetail(false, productId));

        assertEquals("존재하지 않는 상품입니다.", e.getMessage());
    }
}