package io.github.takgeun.shop.global.config;

import io.github.takgeun.shop.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"test", "mybatis"})
public class OpenApiIntegrationTest extends IntegrationTestSupport {

    @Test
    void OpenAPI_문서가_생성된다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title")
                        .value("Shopping Mall API"))
                .andExpect(jsonPath(
                        "$.paths['/api/v1/categories']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/categories/{categoryId}']"
                ).exists());
    }
}
