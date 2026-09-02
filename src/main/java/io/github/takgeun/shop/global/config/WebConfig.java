package io.github.takgeun.shop.global.config;

import io.github.takgeun.shop.global.interceptor.AdminAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 관리자 전용
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns(
                        "/admin/**",
                        "/api/v1/admin/**")
                .excludePathPatterns(
                        "/admin/products",
                        "/admin/products/**",
                        "/login",
                        "/logout",
                        "/signup",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/assets/**",
                        "/webjars/**",
                        "/error",
                        "/forbidden",
                        "/favicon.ico"
                );
    }
}
