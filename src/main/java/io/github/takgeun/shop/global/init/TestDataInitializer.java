package io.github.takgeun.shop.global.init;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.product.application.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")   // local 환경에서만 실행
@RequiredArgsConstructor
public class TestDataInitializer implements ApplicationRunner {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final MemberService memberService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        // 회원 생성
        memberService.signup("test1@test.com", "pw12341234!", "테스트", "010-1111-2222");

        // 카테고리
        Long electronics = categoryService.create("전자", null);
        Long computer = categoryService.create("컴퓨터", electronics);
        Long phone = categoryService.create("휴대폰", electronics);

        // 상품
        productService.create(electronics, "전자제품 랜덤", 10000, 10, "전자 카테고리 테스트 상품");

        productService.create(computer, "맥북 프로", 3_000_000, 5, "애플 노트북");
        productService.create(computer, "게이밍 데스크탑", 2_500_000, 3, "RTX 탑재");

        productService.create(phone, "아이폰 15", 1_500_000, 0, "품절 상태");
        productService.create(phone, "갤럭시 S24", 1_400_000, 7, "삼성 최신폰");
    }
}
