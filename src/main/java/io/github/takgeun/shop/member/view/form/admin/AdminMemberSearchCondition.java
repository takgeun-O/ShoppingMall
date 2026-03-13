package io.github.takgeun.shop.member.view.form.admin;

import lombok.Getter;

@Getter
public class AdminMemberSearchCondition {

    private final String nameQuery;
    private final String emailQuery;
    private final String statusFilter;
    private final int page;
    private final int pageSize;

    private AdminMemberSearchCondition(String nameQuery,
                                      String emailQuery,
                                      String statusFilter,
                                      int page,
                                      int pageSize) {
        this.nameQuery = nameQuery;
        this.emailQuery = emailQuery;
        this.statusFilter = statusFilter;
        this.page = page;
        this.pageSize = pageSize;
    }

    public static AdminMemberSearchCondition of(
            String nameQuery,
            String emailQuery,
            String statusFilter,
            Integer page,
            Integer pageSize
    ) {
        int safePage = (page == null || page < 1) ? 1: page;
        int safePageSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        return new AdminMemberSearchCondition(
                nameQuery,
                emailQuery,
                statusFilter,
                safePage,
                safePageSize
        );
    }
}
