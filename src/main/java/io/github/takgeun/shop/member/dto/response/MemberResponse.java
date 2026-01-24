package io.github.takgeun.shop.member.dto.response;

import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 내 정보 조회용 응답
@Getter
@AllArgsConstructor
public class MemberResponse {

    private Long id;
    private String email;
    private String name;
//    private String password;
    private String phone;
    private MemberRole role;
    private MemberStatus status;

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getPhone(),
                member.getRole(),
                member.getStatus()
        );
    }
}
