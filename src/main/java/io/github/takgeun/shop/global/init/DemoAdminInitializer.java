package io.github.takgeun.shop.global.init;

import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.MemberRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoAdminInitializer implements ApplicationRunner {

    private final MemberService memberService;

    @Value("${app.demo.admin.email}")
    private String email;

    @Value("${app.demo.admin.password}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        Long adminId = memberService.signup(
                email,
                password,
                "관리자",
                "010-9999-9999"
        );

        memberService.changeRole(adminId, MemberRole.ADMIN);
    }
}
