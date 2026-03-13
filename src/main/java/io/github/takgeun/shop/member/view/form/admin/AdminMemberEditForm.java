package io.github.takgeun.shop.member.view.form.admin;

import io.github.takgeun.shop.global.validation.ValidationGroups;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.member.view.dto.admin.AdminMemberEditView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 실제 수정용
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminMemberEditForm {

    @NotBlank(message = "이름은 필수입니다.", groups = ValidationGroups.Required.class)
    private String name;

    @NotBlank(message = "전화번호는 필수입니다.", groups = ValidationGroups.Required.class)
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.", groups = ValidationGroups.Format.class)
    private String phone;

    @NotNull(message = "회원 상태는 필수입니다.")
    private MemberStatus status;

    public static AdminMemberEditForm from(AdminMemberEditView view) {
        AdminMemberEditForm form = new AdminMemberEditForm();
        form.setName(view.getName());
        form.setPhone(view.getPhone());
        form.setStatus(view.getStatus());
        return form;
    }
}
