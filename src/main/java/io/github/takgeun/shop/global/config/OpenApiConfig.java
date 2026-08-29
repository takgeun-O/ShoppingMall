package io.github.takgeun.shop.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shoppingMallOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shopping Mall API")
                        .description("""
                                쇼핑몰 프로젝트의 REST API 명세입니다.

                                공개 상품 및 카테고리 조회부터 회원, 주문,
                                관리자 기능까지 단계적으로 제공합니다.
                                """)
                        .version("v1")
                        .contact(new Contact()
                                .name("Takgeun Oh")
                                .url("https://github.com/takgeun-O"))
                        .license(new License()
                                .name("Portfolio Project")));
    }
}
