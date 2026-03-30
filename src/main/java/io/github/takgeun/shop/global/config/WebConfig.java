package io.github.takgeun.shop.global.config;

import io.github.takgeun.shop.global.interceptor.AdminAuthInterceptor;
import io.github.takgeun.shop.global.interceptor.UserAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final UserAuthInterceptor userAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 관리자 전용
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
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

        // 유저 전용 : 로그인 필요
        registry.addInterceptor(userAuthInterceptor)
                .addPathPatterns(
                        "/orders/**",
                        "/members/me",
                        "/members/me/**"
                )
                .excludePathPatterns(
                        "/login",
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
