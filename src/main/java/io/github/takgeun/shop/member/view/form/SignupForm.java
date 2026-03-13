package io.github.takgeun.shop.member.view.form;

import io.github.takgeun.shop.global.validation.ValidationGroups;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SignupForm {

    @NotBlank(message = "이메일은 필수입니다.", groups = ValidationGroups.Required.class)
    @Email(message = "이메일 형식이 올바르지 않습니다.", groups = ValidationGroups.Format.class)
    @Size(max = 320, message = "이메일은 320자 이하입니다.", groups = ValidationGroups.Format.class)
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.", groups = ValidationGroups.Required.class)
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자입니다.", groups = ValidationGroups.Format.class)
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다.", groups = ValidationGroups.Required.class)
    @Size(min = 8, max = 20, message = "비밀번호 확인은 8~20자입니다.", groups = ValidationGroups.Format.class)
    private String confirmPassword;

    @NotBlank(message = "이름은 필수입니다.", groups = ValidationGroups.Required.class)
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.", groups = ValidationGroups.Format.class)
    private String name;

    @NotBlank(message = "전화번호는 필수입니다.", groups = ValidationGroups.Required.class)
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.", groups = ValidationGroups.Format.class)
    @Size(max = 20, message = "전화번호는 20자 이하입니다.", groups = ValidationGroups.Format.class)
    private String phone;

    @AssertTrue(message = "약관에 동의해주세요.", groups = ValidationGroups.Required.class)
    private boolean agreeToTerms;
}
