package io.github.takgeun.shop.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI shoppingMallOpenApi() {
                return new OpenAPI()
                        .info(new Info()
                                .title("ShoppingMall REST API")
                                .description(
                                        "Spring Boot 기반 쇼핑몰 REST API"
                                )
                                .version("v1"));
        }
}

