package io.github.takgeun.shop.order.view.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 화면용 페이징 로직 담당 (DB 페이징 아님)
 * 주문 목록을 페이지 단위로 잘라서 화면에 보여주기 위한 뷰 DTO
 */
@Getter
public class OrderHistoryPageView {

    private final List<OrderHistoryItemView> orders;    // 현재 페이지에 보여줄 주문 목록
    private final int totalOrders;                      // 전체 주문 개수
    private final int currentPage;                      // 현재 페이지
    private final int totalPages;                       // 전체 페이지 수
    private final List<Integer> pageNumbers;            // 화면에 표시할 페이지 번호 ([1, 2, 3, 4, 5] 또는 [3, 4, 5, 6, 7] 이런 식)

    private OrderHistoryPageView(
            List<OrderHistoryItemView> orders,
            int totalOrders,
            int currentPage,
            int totalPages,
            List<Integer> pageNumbers) {

        this.orders = orders;
        this.totalOrders = totalOrders;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.pageNumbers = pageNumbers;
    }

    /**
     *
     * @param allOrders : 전체 주문 목록
     * @param requestedPage : 요청 페이지
     * @param pageSize : 페이지당 주문 개수 (그러니까 한 페이지에 몇 개의 데이터를 보여줄 것인지 결정)
     * @return
     */
    public static OrderHistoryPageView of(List<OrderHistoryItemView> allOrders, int requestedPage, int pageSize) {

        List<OrderHistoryItemView> safeOrders =
                (allOrders == null) ? List.of() : List.copyOf(allOrders);   // copyOf()는 불변리스트로 복사함.

        int totalOrders = safeOrders.size();    // 전체 주문 수 (42)
        int totalPages = Math.max((int) Math.ceil((double) totalOrders / pageSize), 1);
        // 전체 페이지 계산
        // (42 / 10 = 4.2 -> ceil -> 5) -> Math.max(5, 1) : 주문이 0개여도 페이지는 최소 1페이지임을 보장

        // requestedPage < 1 -> 1 , requestedPage > totalPages -> totalPages (현재 페이지 보정)
        int currentPage = Math.min(Math.max(requestedPage, 1), totalPages);

        // pageSize = 10, currentPage = 2
        // 즉 현재 페이지가 2페이지일 때 orders[10 ~ 19] 보여주기
        int startIndex = (currentPage - 1) * pageSize;  // (2-1) * 10 = 10
        int endIndex = Math.min(startIndex + pageSize, totalOrders);    // 20

        // 현재 페이지의 주문만 따로 추출
        List<OrderHistoryItemView> currentOrders =
                (totalOrders == 0) ? List.of() : safeOrders.subList(startIndex, endIndex);

        // 페이지 번호 생성
        // 현재 페이지 = 7, 전체 페이지 = 20 -> [5, 6, 7, 8, 9] 생성
        List<Integer> pageNumbers = buildPageNumbers(currentPage, totalPages);

        // 현재 페이지의 주문, 전체 주문, 현재 페이지 번호, 전체 페이지 개수, 뷰에 보여질 페이지 번호 리스트
        return new OrderHistoryPageView(
                currentOrders,
                totalOrders,
                currentPage,
                totalPages,
                pageNumbers
        );
    }

    // 페이지 번호 UI를 만들기 위한 로직
    private static List<Integer> buildPageNumbers(int currentPage, int totalPages) {
        if (totalPages <= 0) return List.of();

        List<Integer> pages = new ArrayList<>();
        int maxVisiblePages = 5;    // 최대 5개만 보여주도록 제한

        int startPage;
        int endPage;

        if (totalPages <= maxVisiblePages) {
            // 전체 페이지가 5 이하 -> [1,2,3,4]
            startPage = 1;
            endPage = totalPages;
        } else if (currentPage <= 3) {
            // 현재 페이지가 앞쪽 -> currentPage = 2 -> [1,2,3,4,5]
            startPage = 1;
            endPage = 5;
        } else if (currentPage >= totalPages - 2) {
            // 현재 페이지가 뒤쪽 -> currentPage = 18, totalPages = 20 -> [16,17,18,19,20]
            startPage = totalPages - 4;
            endPage = totalPages;
        } else {
            // 현재 페이지가 중간 -> currentPage = 7 -> [5,6,7,8,9]
            startPage = currentPage - 2;
            endPage = currentPage + 2;
        }

        for (int i = startPage; i <= endPage; i++) {
            pages.add(i);
        }

        return pages;
    }
}
