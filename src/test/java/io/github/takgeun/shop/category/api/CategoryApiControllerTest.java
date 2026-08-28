package io.github.takgeun.shop.category.api;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.global.error.api.ApiGlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CategoryApiControllerTest {

    private CategoryService categoryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        categoryService = mock(CategoryService.class);

        CategoryApiController controller =
                new CategoryApiController(categoryService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new ApiGlobalExceptionHandler()
                )
                .build();
    }
}