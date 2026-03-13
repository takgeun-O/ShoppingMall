package io.github.takgeun.shop.member.view.dto.admin;

import lombok.Getter;

@Getter
public class AdminMemberSummaryView {

    private final int totalMemberCount;
    private final int todayJoinedMemberCount;
    private final int activeMemberCount;
    private final int inactiveMemberCount;
    private final int withdrawnMemberCount;

    private AdminMemberSummaryView(int totalMemberCount,
                                  int todayJoinedMemberCount,
                                  int activeMemberCount,
                                  int inactiveMemberCount,
                                  int withdrawnMemberCount) {
        this.totalMemberCount = totalMemberCount;
        this.todayJoinedMemberCount = todayJoinedMemberCount;
        this.activeMemberCount = activeMemberCount;
        this.inactiveMemberCount = inactiveMemberCount;
        this.withdrawnMemberCount = withdrawnMemberCount;
    }

    public static AdminMemberSummaryView of(
            int totalMemberCount,
            int todayJoinedMemberCount,
            int activeMemberCount,
            int inactiveMemberCount,
            int withdrawnMemberCount
    ) {
        return new AdminMemberSummaryView(
                totalMemberCount,
                todayJoinedMemberCount,
                activeMemberCount,
                inactiveMemberCount,
                withdrawnMemberCount
        );
    }
}
