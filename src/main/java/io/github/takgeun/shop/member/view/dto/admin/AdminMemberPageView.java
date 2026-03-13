package io.github.takgeun.shop.member.view.dto.admin;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 목록 페이지 전체를 감싸는 DTO (페이징까지)
 */
@Getter
public class AdminMemberPageView {

    private final List<AdminMemberItemView> members;
    private final AdminMemberSummaryView summary;

    private final int totalMembers;
    private final int currentPage;
    private final int totalPages;
    private final int pageSize;
    private final List<Integer> pageNumbers;

    private AdminMemberPageView(List<AdminMemberItemView> members,
                                AdminMemberSummaryView summary,
                                int totalMembers,
                                int currentPage,
                                int totalPages,
                                int pageSize,
                                List<Integer> pageNumbers) {
        this.members = members;
        this.summary = summary;
        this.totalMembers = totalMembers;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.pageSize = pageSize;
        this.pageNumbers = pageNumbers;
    }

    public static AdminMemberPageView of(
            List<AdminMemberItemView> allMembers,
            int requestedPage,
            int pageSize,
            AdminMemberSummaryView summary
    ) {
        int totalMembers = allMembers.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalMembers / pageSize));

        int currentPage = requestedPage;
        if (currentPage < 1) {
            currentPage = 1;
        }
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        // 현재 보여지는 페이지에서 보여질 회원정보 인덱스
        // 1페이지 : 0 ~ 9 / 2페이지 : 10 ~ 19 / ... / 마지막 페이지 : 70 ~ 72
        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalMembers);

        // 현재 페이지에서 보여질 멤버들 (전체 멤버에서 잘라내서 보여주기)
        List<AdminMemberItemView> pagedMembers = new ArrayList<>();
        if (startIndex < totalMembers) {
            pagedMembers = allMembers.subList(startIndex, endIndex);
        }

        List<Integer> pageNumbers = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNumbers.add(i);
        }

        return new AdminMemberPageView(
                pagedMembers,
                summary,
                totalMembers,
                currentPage,
                totalPages,
                pageSize,
                pageNumbers
        );
    }

    public int getStartRow() {
        if(totalMembers == 0) {
            return 0;
        }
        return (currentPage - 1) * pageSize + 1;
    }

    public int getEndRow() {
        return Math.min(currentPage * pageSize, totalMembers);
    }
}
