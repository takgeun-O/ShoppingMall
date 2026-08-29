package io.github.takgeun.shop.member.application;

import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRepository;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.member.dto.request.AdminMemberStatusUpdateRequest;
import io.github.takgeun.shop.member.dto.request.AdminMemberUpdateRequest;
import io.github.takgeun.shop.member.view.dto.admin.*;
import io.github.takgeun.shop.member.view.form.admin.AdminMemberSearchCondition;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;

    /**
     * 관리자 회원 목록 + 검색 + 페이징 + 통계
     * condition에 따라 회원 목록 페이지 전체 정보 반환
     * 필요 데이터
     * - 필터에 따른 Members ->  -> 가입 회원, 활성 회원, 휴면 회원, 탈퇴 회원은 여기서 뽑아내기
     * - List<AdminMemberItemView>
     * - AdminMemberSummaryView
     * - totalMembers
     * - 페이지 관련 데이터 : currentPage, totalPages, pageSize, pageNumbers
     */
    public AdminMemberPageView getAdminMemberPage(AdminMemberSearchCondition condition) {

        List<Member> allMembers = memberRepository.findAll();

        // 필터 조건 : 회원 이름, 이메일, 회원 상태
        List<Member> filteredMembers = allMembers.stream()
                .filter(member -> matchesName(member, condition.getNameQuery()))
                .filter(member -> matchesEmail(member, condition.getEmailQuery()))
                .filter(member -> matchesStatus(member, condition.getStatusFilter()))
                .toList();

        List<AdminMemberItemView> allItems = filteredMembers.stream()
                .map(member -> AdminMemberItemView.from(
                        member, orderRepository.countByMemberId(member.getId())
                        ))
                .toList();

        AdminMemberSummaryView summary = createSummary(allMembers);

        return AdminMemberPageView.of(
                allItems,
                condition.getPage(),
                condition.getPageSize(),
                summary
        );
    }

    /**
     * 관리자 회원 상세 조회
     */
    public AdminMemberDetailView getAdminMemberDetail(Long memberId) {

        Member member = findMember(memberId);

        List<Order> memberOrders = orderRepository.findAllByMemberId(memberId);

        List<Order> sortedOrders = memberOrders.stream()
                .sorted(Comparator.comparing(Order::getOrderedAt).reversed())
                .toList();

        List<AdminMemberOrderItemView> recentOrders = sortedOrders.stream()
                .limit(5)
                .map(AdminMemberOrderItemView::from)
                .toList();

        int totalOrders = sortedOrders.size();      // 총 주문 수
        int totalSpent = sortedOrders.stream()
                .filter(order -> !order.isCanceled())
                .mapToInt(Order::getTotalPrice)
                .sum();

        return AdminMemberDetailView.of(
                member,
                recentOrders,
                totalOrders,
                totalSpent
        );
    }

    /**
     * 관리자 회원 수정 페이지 조회
     */
    public AdminMemberEditView getAdminMemberEditView(Long memberId) {

        Member member = findMember(memberId);
        List<Order> orders = orderRepository.findAllByMemberId(memberId);

        return AdminMemberEditView.from(member, orders);
    }

    /**
     * 관리자 회원 수정 처리 (회원 수정 페이지에서 수정 처리)
     */
    @Transactional
    public void updateMember(Long memberId, AdminMemberUpdateRequest request) {

        Member member = findMember(memberId);

        // 불필요한 업데이트 방지
        if(!member.getName().equals(request.getName())) {
            member.changeName(request.getName());
        }
        if(!member.getPhone().equals(request.getPhone())) {
            member.changePhone(request.getPhone());

        }
        if(!member.getStatus().equals(request.getStatus())) {
            member.changeStatus(request.getStatus());

        }

        memberRepository.save(member);      // 메모리 리포지토리에선 필요 없으나 JPA 변환 시 필요할 것.
    }

    /**
     * 관리자 회원 상태 수정 처리 (회원 상세정보 페이지에서 수정 처리)
     */
    @Transactional
    public void changeMemberStatus(Long memberId, AdminMemberStatusUpdateRequest request) {

        Member member = findMember(memberId);

        if(!member.getStatus().equals(request.getStatus())) {
            member.changeStatus(request.getStatus());
        }

        memberRepository.save(member);
    }

    /**
     * 관리자 회원 탈퇴 (회원 상세정보 페이지에서 탈퇴 처리)
     */
    @Transactional
    public void withdrawMember(Long memberId) {

        Member member = findMember(memberId);

        // 이미 탈퇴한 회원 탈퇴 시도 시 예외 처리
        if(member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new ConflictException("이미 탈퇴 처리된 회원입니다.");
        }

        member.changeStatus(MemberStatus.WITHDRAWN);

        memberRepository.save(member);
    }


    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));
    }

    private boolean matchesName(Member member, String nameQuery) {
        if(nameQuery == null || nameQuery.isBlank()) {
            return true;
        }
        return member.getName().toLowerCase().contains(nameQuery.trim().toLowerCase());
    }

    private boolean matchesEmail(Member member, String emailQuery) {
        if(emailQuery == null || emailQuery.isBlank()) {
            return true;
        }
        return member.getEmail().toLowerCase().contains(emailQuery.trim().toLowerCase());
    }

    private boolean matchesStatus(Member member, String statusFilter) {
        if(statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter)) {    // 대소문자 무시하고 비교
            return true;
        }
        return member.getStatus().name().equalsIgnoreCase(statusFilter);
    }

    private AdminMemberSummaryView createSummary(List<Member> members) {
        int totalMemberCount = members.size();
        int todayJoinedMemberCount = (int) members.stream()
                .filter(this::isJoinedToday)
                .count();
        int activeMemberCount = (int) members.stream()
                .filter(member -> member.getStatus() == MemberStatus.ACTIVE)
                .count();
        int withdrawnMemberCount = (int) members.stream()
                .filter(member -> member.getStatus() == MemberStatus.WITHDRAWN)
                .count();
        int inactiveMemberCount = (int) members.stream()
                .filter(member -> member.getStatus() == MemberStatus.INACTIVE)
                .count();

        return AdminMemberSummaryView.of(
                totalMemberCount,
                todayJoinedMemberCount,
                activeMemberCount,
                withdrawnMemberCount,
                inactiveMemberCount
        );
    }

    private boolean isJoinedToday(Member member) {
        if(member.getCreatedAt() == null) {
            return false;
        }
        return member.getCreatedAt().toLocalDate().isEqual(LocalDate.now());
    }
}
