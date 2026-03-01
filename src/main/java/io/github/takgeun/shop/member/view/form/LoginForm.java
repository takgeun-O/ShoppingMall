package io.github.takgeun.shop.member.view.form;

import io.github.takgeun.shop.global.validation.ValidationGroups;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginForm {

    @NotBlank(message = "이메일은 필수입니다.", groups = ValidationGroups.Required.class)
    @Email(message = "이메일 형식이 올바르지 않습니다.", groups = ValidationGroups.Format.class)
    @Size(max = 320, message = "이메일은 320자 이하입니다.", groups = ValidationGroups.Format.class)
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.", groups = ValidationGroups.Required.class)
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자입니다.", groups = ValidationGroups.Format.class)
    private String password;
}
