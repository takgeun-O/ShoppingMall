package io.github.takgeun.shop.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Shopping Mall API",
                version = "v1",
                description = "쇼핑몰 백엔드 API 문서"
        )
)
public class SwaggerConfig {
}

