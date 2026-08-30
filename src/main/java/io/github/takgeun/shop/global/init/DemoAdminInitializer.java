package io.github.takgeun.shop.global.init;

import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.MemberRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoAdminInitializer implements ApplicationRunner {

    private final MemberService memberService;

    @Value("${app.demo.admin.email}")
    private String adminEmail;

    @Value("${app.demo.admin.password:}")
    private String adminPassword;

    @Value("${app.demo.admin.name}")
    private String adminName;

    @Value("${app.demo.admin.phone}")
    private String adminPhone;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if(memberService.existsByEmail(adminEmail)) {
            return;
        }

        if(adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD environment variable is required for demo admin initialization."
            );
        }

        Long adminId = memberService.signup(
                adminEmail,
                adminPassword,
                adminName,
                adminPhone
        );

        memberService.changeRole(adminId, MemberRole.ADMIN);
    }
}
