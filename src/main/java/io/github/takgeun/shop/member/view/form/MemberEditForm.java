package io.github.takgeun.shop.member.view.form;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MemberEditForm {

    // 패스워드 변경은 별도의 폼에서 관리할 것. (보안 처리 등등때문)
//    @Size(min = 8, max = 20, message = "비밀번호는 8~20자입니다.")
//    private String password;

    @Size(max = 50, message = "이름은 50자 이하입니다.")
    private String name;

    @Pattern(regexp = "^[0-9+\\- ]+$", message = "전화번호 형식이 올바르지 않습니다.")
    @Size(max = 20, message = "전화번호는 20자 이하입니다.")
    private String phone;
}
