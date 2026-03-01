package io.github.takgeun.shop.member.view.dto;

import io.github.takgeun.shop.member.domain.Member;
import lombok.Getter;

@Getter
public class MyPageMemberView {

    private final Long id;
    private final String email;
    private final String name;
    private final String phone;
    private final String grade;       // 처음은 GOLD 문자열로 임시 처리
    private final int point;          // 15000 원으로 임시 처리
    private final int couponCount;    // 5 임시 처리

    private MyPageMemberView(Long id, String email, String name, String phone, String grade, int point, int couponCount) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.grade = grade;
        this.point = point;
        this.couponCount = couponCount;
    }

    public static MyPageMemberView from(Member m, String grade, int point, int couponCount) {

        return new MyPageMemberView(
                m.getId(),
                m.getEmail(),
                m.getName(),
                m.getPhone(),
                grade,
                point,
                couponCount
        );
    }
}
