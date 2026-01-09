package io.github.takgeun.shop.category.application;

import io.github.takgeun.shop.category.infra.MemoryCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryServiceTest {

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(new MemoryCategoryRepository());
    }

    @Test
    void 루트_카테고리_생성_성공() {
        Long id = categoryService.create("전자", null);
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

}