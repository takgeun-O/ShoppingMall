package io.github.takgeun.shop.member.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberUpdateRequest {

    @Size(min = 8, max = 20, message = "비밀번호는 8~20자입니다.")
    private String password;

    @Size(max = 50, message = "이름은 50자 이하입니다.")
    private String name;

    @Pattern(regexp = "^[0-9+\\- ]+$", message = "전화번호 형식이 올바르지 않습니다.")
    @Size(max = 20, message = "전화번호는 20자 이하입니다.")
    private String phone;

    // 테스트용
    public static MemberUpdateRequest of(
            String password,
            String name,
            String phone
    ) {
        MemberUpdateRequest request = new MemberUpdateRequest();
        request.password = password;
        request.name = name;
        request.phone = phone;
        return request;
    }
}
