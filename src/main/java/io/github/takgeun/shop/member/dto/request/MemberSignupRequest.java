package io.github.takgeun.shop.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

//@Data         // @Data의 역할이 과도함. (setter를 여기서 쓰지 않는데 DTO는 보통 불변으로 쓰여서 setter를 쓰지 않음.) 즉 @Data는 가변 객체를 전제로 쓰인다.
@Getter
@NoArgsConstructor
// 요청 DTO는 프레임워크 바인딩 용도 -> setter 없이 사용
// 이 부분은 class 대신 record 사용해서 추후 교체 예정
public class MemberSignupRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 320, message = "이메일은 320자 이하입니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자입니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^$|^[0-9+\\- ]+$", message = "전화번호 형식이 올바르지 않습니다.")
    @Size(max = 20, message = "전화번호는 20자 이하입니다.")
    private String phone;
}
