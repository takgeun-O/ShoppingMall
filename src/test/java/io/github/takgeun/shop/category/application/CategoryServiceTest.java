package io.github.takgeun.shop.category.application;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryRepository;
import io.github.takgeun.shop.category.infra.memory.MemoryCategoryRepository;
import io.github.takgeun.shop.global.error.code.ErrorCode;
import io.github.takgeun.shop.global.error.exception.BusinessException;
import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.ProductRepository;
import io.github.takgeun.shop.product.domain.ProductStatus;
import io.github.takgeun.shop.product.infra.memory.MemoryProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CategoryServiceTest {

    private CategoryService categoryService;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        CategoryRepository categoryRepository =
                new MemoryCategoryRepository();

        ProductRepository productRepository =
                new MemoryProductRepository();

        categoryService = new CategoryService(
                categoryRepository,
                productRepository
        );

        productService = new ProductService(
                productRepository,
                categoryService
        );
    }

    @Test
    void 루트_카테고리_생성_성공() {
        // when
        Long categoryId = categoryService.create(
                "전자",
                null
        );

        // then
        assertNotNull(categoryId);
    }

    @Test
    void 카테고리_생성_실패_이름_중복_trim_기준() {
        // given
        categoryService.create("전자", null);

        // when
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> categoryService.create(" 전자 ", null)
        );

        // then
        assertErrorCode(
                exception,
                ErrorCode.CATEGORY_NAME_DUPLICATED
        );
    }

    @Test
    void 카테고리_생성_실패_공백만() {
        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> categoryService.create(" ", null)
        );

        // then
        assertErrorCode(
                exception,
                ErrorCode.INVALID_INPUT
        );
    }

    @Test
    void 카테고리_생성_실패_부모_카테고리가_존재하지_않음() {
        // when
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> categoryService.create("노트북", 999L)
        );

        // then
        assertErrorCode(
                exception,
                ErrorCode.PARENT_CATEGORY_NOT_FOUND
        );
    }

    @Test
    void 카테고리_생성_실패_깊이_제한_초과() {
        // given
        Long electronicsId =
                categoryService.create("전자", null);

        Long computerId =
                categoryService.create("컴퓨터", electronicsId);

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> categoryService.create("노트북", computerId)
        );

        // then
        assertErrorCode(
                exception,
                ErrorCode.CATEGORY_DEPTH_EXCEEDED
        );
    }

    @Test
    void 유저_카테고리_조회_성공() {
        // given
        Long categoryId =
                categoryService.create("전자", null);

        // when
        Category category =
                categoryService.getPublic(categoryId);

        // then
        assertThat(category.getId()).isEqualTo(categoryId);
        assertThat(category.getName()).isEqualTo("전자");
    }

    @Test
    void 유저_카테고리_조회_실패_카테고리_없음() {
        // when
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> categoryService.getPublic(999L)
        );

        // then
        assertErrorCode(
                exception,
                ErrorCode.CATEGORY_NOT_FOUND
        );
    }

    @Test
    void 유저_카테고리_목록_조회_성공() {
        // given
        categoryService.create("전자", null);

        Long applianceId =
                categoryService.create("전자2", null);

        categoryService.create("컴퓨터", applianceId);

        // when
        List<Category> categories =
                categoryService.getAllPublic();

        // then
        assertThat(categories)
                .hasSize(3)
                .extracting(Category::getName)
                .containsExactly(
                        "전자",
                        "전자2",
                        "컴퓨터"
                );

        Category parent = categories.stream()
                .filter(category ->
                        category.getName().equals("전자2"))
                .findFirst()
                .orElseThrow();

        Category child = categories.stream()
                .filter(category ->
                        category.getName().equals("컴퓨터"))
                .findFirst()
                .orElseThrow();

        assertThat(child.getParentId())
                .isEqualTo(parent.getId());
    }

    @Test
    void 카테고리_수정_성공_이름() {
        // given
        Long categoryId =
                categoryService.create("전자", null);

        // when
        categoryService.update(
                categoryId,
                "가전",
                null
        );

        // then
        Category updated =
                categoryService.getAdmin(categoryId);

        assertThat(updated.getName()).isEqualTo("가전");
    }

    @Test
    void 카테고리_수정_성공_부모_이동() {
        // given
        Long electronicsId =
                categoryService.create("전자", null);

        Long computerId =
                categoryService.create("컴퓨터", null);

        // when
        categoryService.update(
                computerId,
                "컴퓨터",
                electronicsId
        );

        // then
        Category updated =
                categoryService.getAdmin(computerId);

        assertThat(updated.getParentId())
                .isEqualTo(electronicsId);
    }

    @Test
    void 카테고리_수정_성공_이름과_부모를_기존_값으로_유지() {
        // given
        Long categoryId =
                categoryService.create("전자", null);

        Category before =
                categoryService.getAdmin(categoryId);

        String beforeName = before.getName();
        Long beforeParentId = before.getParentId();

        // when
        categoryService.update(
                categoryId,
                beforeName,
                beforeParentId
        );

        // then
        Category updated =
                categoryService.getAdmin(categoryId);

        assertThat(updated.getName())
                .isEqualTo(beforeName);

        assertThat(updated.getParentId())
                .isEqualTo(beforeParentId);
    }

    @Test
    void 카테고리_수정_실패_이름_중복() {
        // given
        categoryService.create("전자", null);

        Long secondCategoryId =
                categoryService.create("전자2", null);

        // when
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> categoryService.update(
                        secondCategoryId,
                        "전자",
                        null
                )
        );

        // then
        assertErrorCode(
                exception,
                ErrorCode.CATEGORY_NAME_DUPLICATED
        );
    }

    @Test
    void 카테고리_수정_실패_부모_카테고리가_존재하지_않음() {
        // given
        Long categoryId =
                categoryService.create("전자", null);

        // when
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> categoryService.update(
                        categoryId,
                        "전자",
                        999L
                )
        );

        // then
        assertErrorCode(
                exception,
                ErrorCode.PARENT_CATEGORY_NOT_FOUND
        );
    }

    @Test
    void 카테고리_수정_실패_자기_자신을_부모로_설정() {
        // given
        Long categoryId =
                categoryService.create("전자", null);

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> categoryService.update(
                        categoryId,
                        "전자",
                        categoryId
                )
        );

        // then
        assertErrorCode(
                exception,
                ErrorCode.INVALID_CATEGORY_PARENT
        );
    }

    @Test
    void 카테고리_수정_실패_부모_순환_참조() {
        // given
        Long electronicsId =
                categoryService.create("전자", null);

        Long notebookId =
                categoryService.create(
                        "노트북",
                        electronicsId
                );

        // 전자 → 노트북으로 부모를 변경하면
        // 전자 → 노트북 → 전자의 순환 구조가 생성된다.

        // when
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> categoryService.update(
                        electronicsId,
                        "전자",
                        notebookId
                )
        );

        // then
        assertErrorCode(
                exception,
                ErrorCode.CATEGORY_CIRCULAR_REFERENCE
        );
    }

    @Test
    void 카테고리_수정_실패_깊이_제한_초과() {
        // given
        Long electronicsId =
                categoryService.create("전자", null);

        Long computerId =
                categoryService.create(
                        "컴퓨터",
                        electronicsId
                );

        Long applianceId =
                categoryService.create("가전", null);

        // 가전을 컴퓨터 아래로 이동하면
        // 전자 → 컴퓨터 → 가전의 3단 구조가 된다.

        // when
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> categoryService.update(
                        applianceId,
                        "가전",
                        computerId
                )
        );

        // then
        assertErrorCode(
                exception,
                ErrorCode.CATEGORY_DEPTH_EXCEEDED
        );
    }

    @Test
    void 카테고리_삭제_성공() {
        // given
        Long categoryId =
                categoryService.create("전자", null);

        // when
        categoryService.delete(categoryId);

        // then
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> categoryService.getAdmin(categoryId)
        );

        assertErrorCode(
                exception,
                ErrorCode.CATEGORY_NOT_FOUND
        );
    }

    @Test
    void 카테고리_삭제_실패_카테고리가_존재하지_않음() {
        // when
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> categoryService.delete(999L)
        );

        // then
        assertErrorCode(
                exception,
                ErrorCode.CATEGORY_NOT_FOUND
        );
    }

    @Test
    void 카테고리_삭제_실패_하위_카테고리가_존재함() {
        // given
        Long electronicsId =
                categoryService.create("전자", null);

        categoryService.create(
                "컴퓨터",
                electronicsId
        );

        // when
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> categoryService.delete(electronicsId)
        );

        // then
        assertErrorCode(
                exception,
                ErrorCode.CATEGORY_HAS_CHILDREN
        );
    }

    @Test
    void 카테고리_삭제_실패_연결된_상품이_존재함() {
        // given
        Long electronicsId =
                categoryService.create("전자", null);

        productService.create(
                electronicsId,
                "냉장고",
                100_000,
                10,
                "냉장고 상품 설명",
                ProductStatus.ON_SALE,
                120_000,
                "/images/no-image.png"
        );

        // when
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> categoryService.delete(electronicsId)
        );

        // then
        assertErrorCode(
                exception,
                ErrorCode.CATEGORY_HAS_PRODUCTS
        );
    }

    /**
     * 예외에 저장된 ErrorCode와 기본 메시지를 함께 검증한다.
     */
    private void assertErrorCode(
            BusinessException exception,
            ErrorCode expectedErrorCode
    ) {
        assertEquals(
                expectedErrorCode,
                exception.getErrorCode()
        );

        assertEquals(
                expectedErrorCode.getDefaultMessage(),
                exception.getMessage()
        );
    }
}