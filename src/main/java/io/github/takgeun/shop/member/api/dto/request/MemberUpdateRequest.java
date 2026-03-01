package io.github.takgeun.shop.member.api.dto.request;

import io.github.takgeun.shop.global.validation.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class MemberUpdateRequest {

    // 패스워드 변경은 별도의 폼에서 관리할 것. (보안 처리 등등때문)
//    @Size(min = 8, max = 20, message = "비밀번호는 8~20자입니다.")
//    private String password;

    @NotBlank(message = "이름은 필수입니다.", groups = ValidationGroups.Required.class)
    @Size(max = 50, message = "이름은 50자 이하입니다.")
    private String name;

    @NotBlank(message = "전화번호는 필수입니다.", groups = ValidationGroups.Required.class)
    @Pattern(regexp = "^010-\\\\d{4}-\\\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.", groups = ValidationGroups.Format.class)
    @Size(max = 20, message = "전화번호는 20자 이하입니다.", groups = ValidationGroups.Format.class)
    private String phone;
}
