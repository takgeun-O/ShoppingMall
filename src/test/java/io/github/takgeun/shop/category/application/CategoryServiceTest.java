package io.github.takgeun.shop.category.application;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.domain.CategoryRepository;
import io.github.takgeun.shop.category.infra.MemoryCategoryRepository;
import io.github.takgeun.shop.global.error.ConflictException;
import io.github.takgeun.shop.global.error.NotFoundException;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.ProductRepository;
import io.github.takgeun.shop.product.infra.MemoryProductRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategoryServiceTest {

    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;

    private CategoryService categoryService;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        categoryRepository = new MemoryCategoryRepository();
        productRepository = new MemoryProductRepository();

        categoryService = new CategoryService(categoryRepository, productRepository);
        productService = new ProductService(productRepository, categoryService);
    }

    @Test
    void 루트_카테고리_생성_성공() {
        // given
        Long id = categoryService.create("전자", null);

        // when & then
        assertNotNull(id);
    }

    @Test
    void 카테고리_생성_실패_이름_중복_trim_기준() {
        categoryService.create("전자", null);
        ConflictException e = assertThrows(ConflictException.class,
                () -> categoryService.create(" 전자 ", null));
        assertEquals("이미 존재하는 카테고리 이름입니다.", e.getMessage());
    }

    @Test
    void 카테고리_생성_실패_공백만() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> categoryService.create(" ", null));
        assertEquals("카테고리명은 비어 있을 수 없습니다.", e.getMessage());
    }

    @Test
    void 부모_카테고리_존재하지_않음_예외() {
        assertThrows(NotFoundException.class,
                () -> categoryService.create("노트북", 999L));
    }

    @Test
    void 카테고리_조회_성공() {
        // given
        Long id = categoryService.create("전자", null);

        // when
        Category category = categoryService.get(id);

        // then
        Assertions.assertThat(category.getName()).isEqualTo("전자");
    }

    @Test
    void 카테고리_조회_실패_카테고리_없음() {
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> categoryService.get(999L));
        assertEquals("카테고리가 존재하지 않습니다.", e.getMessage());
    }

    @Test
    void 카테고리_목록_조회_성공() {

        // given
        Long categoryId1 = categoryService.create("전자", null);
        Long categoryId2 = categoryService.create("전자2", null);
        Long categoryId3 = categoryService.create("컴퓨터", categoryId2);

        // when
        List<Category> categoryList = categoryService.getAll();

        // then
        assertEquals(3, categoryList.size());

        assertTrue(categoryList.stream()
                .anyMatch(c -> c.getName().equals("전자")));
        assertTrue(categoryList.stream()
                .anyMatch(c -> c.getName().equals("전자2")));
        assertTrue(categoryList.stream()
                .anyMatch(c -> c.getName().equals("컴퓨터")));

        Category parent = categoryList.stream()
                .filter(c -> c.getName().equals("전자2"))
                .findFirst()
                .orElseThrow();

        Category child = categoryList.stream()
                .filter(c -> c.getName().equals("컴퓨터"))
                .findFirst()
                .orElseThrow();

        assertEquals(parent.getId(), child.getParentId());
    }

    @Test
    void 카테고리_수정_성공_이름() {

        // given
        Long id = categoryService.create("전자", null);

        // when
        categoryService.update(id, "전자2", null, null);

        // then
        Category updated = categoryService.get(id);
        assertEquals("전자2", updated.getName());
    }

    @Test
    void 카테고리_수정_성공_이동() {

        // given
        Long electronicsId = categoryService.create("전자", null);
        Long computerId = categoryService.create("컴퓨터", null);

        // when
        categoryService.update(computerId, null, electronicsId, null);

        // then
        Category updated = categoryService.get(computerId);
        assertEquals(electronicsId, updated.getParentId());
    }

    @Test
    void 카테고리_수정_성공_activate() {

        // given
        Long id = categoryService.create("전자", null);

        // when
        categoryService.update(id, null, null, false);
        Category updated = categoryService.get(id);

        // then
        assertFalse(updated.isActive());
    }

    @Test
    void 카테고리_수정_성공_deactivate() {

        // given
        Long id = categoryService.create("전자", null);

        // when
        categoryService.update(id, null, null, true);
        Category updated = categoryService.get(id);

        // then
        assertTrue(updated.isActive());
    }

    @Test
    void 카테고리_수정_성공_아무_값도_안_들어왔을_떄() {

        // given
        Long id = categoryService.create("전자", null);
        Category before = categoryService.get(id);

        String beforeName = before.getName();
        Long beforeParentId = before.getParentId();
        boolean beforeActive = before.isActive();

        // when
        categoryService.update(id, null, null, null);
        Category updated = categoryService.get(id);

        // then
        assertEquals(beforeName, updated.getName());
        assertEquals(beforeParentId, updated.getParentId());
        assertEquals(beforeActive, updated.isActive());
    }

    @Test
    void 카테고리_수정_실패_이름중복() {

        // given
        Long id = categoryService.create("전자", null);
        Long id2 = categoryService.create("전자2", null);

        // when

        // then
        ConflictException e = assertThrows(ConflictException.class,
                () -> categoryService.update(id2, "전자", null, null));
        assertEquals("이미 존재하는 카테고리 이름입니다.", e.getMessage());
    }

    @Test
    void 카테고리_수정_실패_부모_카테고리_존재하지_않음() {

        // given


        // when
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> categoryService.create("노트북", 999L));

        // then
        assertEquals("부모 카테고리가 존재하지 않습니다.", e.getMessage());
    }

    @Test
    void 카테고리_수정_실패_자기자신부모() {

        // given
        Long electronicsId = categoryService.create("전자", null);

        // when

        // then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> categoryService.update(electronicsId, null, electronicsId, null));
        assertEquals("자기 자신을 부모로 지정할 수 없습니다.", e.getMessage());
    }

    @Test
    void 카테고리_수정_실패_부모순환구조() {

        // given
        Long electronicsId = categoryService.create("전자", null);
        Long computerCategoryId = categoryService.create("컴퓨터", electronicsId);
        Long notebookCategoryId = categoryService.create("노트북", computerCategoryId);

        // when
        ConflictException e = assertThrows(ConflictException.class,
                () -> categoryService.update(electronicsId, null, notebookCategoryId, null));

        // then
        assertEquals("부모 카테고리 수정으로 인해 순환 구조가 발생합니다.", e.getMessage());
    }

    @Test
    void 카테고리_삭제_성공() {

        // given
        Long electronicsId = categoryService.create("전자", null);

        // when
        categoryService.delete(electronicsId);

        // then
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> categoryService.get(electronicsId));
        assertEquals("카테고리가 존재하지 않습니다.", e.getMessage());
    }

    @Test
    void 카테고리_삭제_실패_카테고리_없음() {

        // given


        // when
        NotFoundException e = assertThrows(NotFoundException.class,
                () -> categoryService.delete(999L));

        // then
        assertEquals("카테고리가 존재하지 않습니다.", e.getMessage());
    }

    // 직계 삭제만 구현
    @Test
    void 카테고리_삭제_실패_하위_카테고리_존재() {

        // given
        Long electronicsId = categoryService.create("전자", null);
        Long computerId = categoryService.create("컴퓨터", electronicsId);

        // when & then
        ConflictException e = assertThrows(ConflictException.class,
                () -> categoryService.delete(electronicsId));
        assertEquals("하위 카테고리가 존재하여 삭제할 수 없습니다.", e.getMessage());
    }

    // 추후 손자 존재 시 삭제 실패도 구현할 예정
    @Test
    void 카테고리_삭제_실패_손자_하위_카테고리_존재() {

    }

    @Test
    void 카테고리_삭제_실패_해당_카테고리_해당_상품_존재() {

        // given
        Long electronicsId = categoryService.create("전자", null);
        Long productId = productService.create(electronicsId, "냉장고", 100000, 10, "튼튼냉장");

        // when & then
        ConflictException e = assertThrows(ConflictException.class,
                () -> categoryService.delete(electronicsId));
        assertEquals("해당 카테고리에 상품이 존재하여 삭제할 수 없습니다.", e.getMessage());
    }
}