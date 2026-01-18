package io.github.takgeun.shop.category.application;

import io.github.takgeun.shop.category.domain.Category;
import io.github.takgeun.shop.category.infra.MemoryCategoryRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategoryServiceTest {

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(new MemoryCategoryRepository());
    }

    @Test
    void 루트_카테고리_생성_성공() {
        // given
        Long id = categoryService.create("전자", null);

        // when & then
        assertNotNull(id);
    }

    @Test
    void 이름_중복이면_예외() {
        categoryService.create("전자", null);
        assertThrows(IllegalArgumentException.class, () -> categoryService.create("전자", null));
    }

    @Test
    void 부모_카테고리_존재하지_않음_예외() {
        assertThrows(IllegalArgumentException.class,
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
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
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
    void 카테고리_수정_실패_이름중복() {

        // given
        Long id = categoryService.create("전자", null);
        Long id2 = categoryService.create("전자2", null);

        // when

        // then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> categoryService.update(id2, "전자", null, null));
        assertEquals("이미 존재하는 카테고리 이름입니다.", e.getMessage());
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
    void 카테고리_수정_성공_active() {

        // given
        Long id = categoryService.create("전자", null);

        // when
        Category category = categoryService.get(id);

        // then
        category.deactivate();
        assertFalse(category.isActive());
    }

    @Test
    void 카테고리_삭제_성공() {

        // given
        Long electronicsId = categoryService.create("전자", null);

        // when
        categoryService.delete(electronicsId);

        // then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> categoryService.get(electronicsId));
        assertEquals("카테고리가 존재하지 않습니다.", e.getMessage());
    }

    @Test
    void 카테고리_삭제_실패_카테고리_없음() {

        // given


        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> categoryService.delete(999L));

        // then
        assertEquals("카테고리가 존재하지 않습니다.", e.getMessage());
    }
}