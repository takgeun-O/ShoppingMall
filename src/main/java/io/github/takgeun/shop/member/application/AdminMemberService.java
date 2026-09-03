package io.github.takgeun.shop.member.application;

import io.github.takgeun.shop.global.error.exception.ConflictException;
import io.github.takgeun.shop.global.error.exception.NotFoundException;
import io.github.takgeun.shop.global.security.session.MemberSessionExpirationEvent;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRepository;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.member.application.command.AdminMemberStatusChangeCommand;
import io.github.takgeun.shop.member.application.command.AdminMemberUpdateCommand;
import io.github.takgeun.shop.member.view.dto.admin.*;
import io.github.takgeun.shop.member.view.form.admin.AdminMemberSearchCondition;
import io.github.takgeun.shop.order.domain.Order;
import io.github.takgeun.shop.order.domain.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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
    public void updateMember(Long memberId, AdminMemberUpdateCommand request) {

        Member member = findMember(memberId);

        /**
         * 이름이나 상태는 ShopUserPrincipal과 인증 가능 여부에 영향을 주므로
         * 세션을 만료하고
         * 전화번호만 변경됐다면 세션을 만료할 필요 없음
         */
        boolean memberChanged = false;      // DB에 저장할 변경사항이 있는가?
        boolean sessionAffectingChange = false; // 기존 Principal이나 로그인 상태에 영향을 주는가?

        // 불필요한 업데이트 방지
        if(!member.getName().equals(request.getName())) {
            member.changeName(request.getName());

            memberChanged = true;
            sessionAffectingChange = true;
        }

        if(!member.getPhone().equals(request.getPhone())) {
            member.changePhone(request.getPhone());

            memberChanged = true;
        }
        if(!member.getStatus().equals(request.getStatus())) {
            member.changeStatus(request.getStatus());

            memberChanged = true;
            sessionAffectingChange = true;
        }

        if(!memberChanged) {
            return;
        }

        memberRepository.save(member);      // 메모리 리포지토리에선 필요 없으나 JPA 변환 시 필요할 것.

        if(sessionAffectingChange) {
            publishSessionExpiration(memberId);
        }
    }

    /**
     * 관리자 회원 상태 수정 처리 (회원 상세정보 페이지에서 수정 처리)
     *
     * 상태가 실제로 달라질 때만 저장하고 세션을 만료시킨다.
     */
    @Transactional
    public void changeMemberStatus(
            Long memberId,
            AdminMemberStatusChangeCommand request
    ) {

        Member member = findMember(memberId);

        MemberStatus newStatus = request.getStatus();

        if(member.getStatus() == newStatus) {
            return;
        }

        member.changeStatus(newStatus);
        memberRepository.save(member);

        // 모든 상태 변경에서 기존 세션 만료
        publishSessionExpiration(memberId);
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

        publishSessionExpiration(memberId);
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

    /**
     * AdminMemberService.withdrawMember() 호출
     *         ↓
     * @Transactional 프록시가 트랜잭션 시작
     *         ↓
     * 회원 상태를 WITHDRAWN으로 변경
     *         ↓
     * memberRepository.save(member)
     *         ↓
     * publishSessionExpiration(memberId)
     *         ↓
     * ApplicationEventPublisher.publishEvent(event)
     *         ↓
     * Spring이 해당 이벤트를 받는 리스너 검색
     *         ↓
     * @TransactionalEventListener 발견
     *         ↓
     * 즉시 실행하지 않고 트랜잭션 완료 시점까지 대기
     *         ↓
     * withdrawMember() 정상 종료
     *         ↓
     * 트랜잭션 커밋
     *         ↓
     * AFTER_COMMIT 리스너 실행
     *         ↓
     * MemberSessionExpirationListener.handle()
     *         ↓
     * MemberSessionService.expireAllByMemberId()
     *         ↓
     * 해당 회원의 모든 로그인 세션 만료 표시
     */
    private void publishSessionExpiration(Long memberId) {
        eventPublisher.publishEvent(
                new MemberSessionExpirationEvent(memberId)
        );
    }
}
