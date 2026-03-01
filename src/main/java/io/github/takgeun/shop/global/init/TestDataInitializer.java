package io.github.takgeun.shop.global.init;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.Member;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.order.application.OrderService;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class TestDataInitializer implements ApplicationRunner {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final MemberService memberService;
    private final OrderService orderService;

    @Override
    public void run(ApplicationArguments args) {

        // =========================
        // 회원
        // =========================
        Long userId1 = memberService.signup("test1@test.com", "pw12341234!", "테스트", "010-1111-2222");
        Long userId2 = memberService.signup("test2@test.com", "pw12341234!", "테스트2", "010-1111-4444");

        Long adminId1 = memberService.signup("testAdmin1@test.com", "pw12341234!", "테스트관리자", "010-1111-3333");
        memberService.changeRole(adminId1, MemberRole.ADMIN);

        // =========================
        // 카테고리
        // =========================
        Long electronics = categoryService.create("전자", null);
        Long furniture   = categoryService.create("가구", null);
        Long clothing    = categoryService.create("의류", null);

        Long computer = categoryService.create("컴퓨터", electronics);
        Long phone    = categoryService.create("휴대폰", electronics);
        Long accessory = categoryService.create("주변기기", electronics);

        Long laptop  = categoryService.create("노트북", computer);
        Long desktop = categoryService.create("데스크탑", computer);
        Long monitor = categoryService.create("모니터", computer);

        Long keyboard = categoryService.create("키보드", accessory);
        Long mouse    = categoryService.create("마우스", accessory);
        Long audio    = categoryService.create("오디오", accessory);

        Long seating = categoryService.create("의자", furniture);
        Long bed     = categoryService.create("침대", furniture);
        Long storage = categoryService.create("수납가구", furniture);

        Long officeChair = categoryService.create("사무용 의자", seating);
        Long diningChair = categoryService.create("식탁 의자", seating);

        Long tops      = categoryService.create("상의", clothing);
        Long bottoms   = categoryService.create("하의", clothing);
        Long outerwear = categoryService.create("아우터", clothing);

        Long tshirt = categoryService.create("티셔츠", tops);
        Long shirt  = categoryService.create("셔츠", tops);

        // =========================
        // 상품
        // =========================

        // 전자(루트) 테스트 1개
        Long electronicsTestId = productService.create(electronics, "전자제품 랜덤", 10_000, 10, "전자 카테고리 테스트 상품");

        // 컴퓨터 > 노트북
        Long macbookId = productService.create(laptop, "맥북 프로 14", 3_000_000, 5, "애플 노트북");

        // 할인 테스트: 원가(정가) 세팅
        productService.changeOriginalPrice(macbookId, 3_500_000);

        Long ultrabookId = productService.create(laptop, "윈도우 울트라북", 1_800_000, 8, "가벼운 업무용");
        productService.changeOriginalPrice(ultrabookId, 2_100_000);

        Long readyLaptopId = productService.create(laptop, "출시 예정 노트북", 2_200_000, 20, "판매 준비");
        productService.changeStatus(readyLaptopId, ProductStatus.READY);

        // 컴퓨터 > 데스크탑
        Long gamingDesktopId = productService.create(desktop, "게이밍 데스크탑", 2_500_000, 3, "RTX 탑재");
        Long miniPcId = productService.create(desktop, "미니 PC", 900_000, 0, "재고 0 테스트"); // stock 0 => 품절

        // 컴퓨터 > 모니터
        Long qhdMonitorId = productService.create(monitor, "27인치 QHD 모니터", 350_000, 12, "가성비 모니터");
        productService.changeOriginalPrice(qhdMonitorId, 420_000);

        Long hiddenMonitorId = productService.create(monitor, "프로토타입 모니터", 1_200_000, 2, "숨김 테스트");
        productService.changeStatus(hiddenMonitorId, ProductStatus.HIDDEN);

        // 휴대폰
        Long iphoneId = productService.create(phone, "아이폰 15", 1_500_000, 0, "품절 상태 (재고 0)");
        Long galaxyId = productService.create(phone, "갤럭시 S24", 1_400_000, 7, "삼성 최신폰");
        productService.changeOriginalPrice(galaxyId, 1_550_000);

        Long readyPhoneId = productService.create(phone, "판매준비중", 1_500_000, 10, "판매 준비");
        productService.changeStatus(readyPhoneId, ProductStatus.READY);

        // 주변기기
        Long keyboardId = productService.create(keyboard, "기계식 키보드", 129_000, 25, "청축/갈축 랜덤");
        Long mouseId = productService.create(mouse, "무선 마우스", 59_000, 30, "사무용");
        Long headsetId = productService.create(audio, "게이밍 헤드셋", 89_000, 0, "품절 테스트");

        // 가구
        Long ergoChairId = productService.create(officeChair, "인체공학 사무의자", 250_000, 6, "허리 지지대 포함");
        productService.changeOriginalPrice(ergoChairId, 299_000);

        Long meshChairId = productService.create(officeChair, "메쉬 의자", 180_000, 15, "통풍 좋은 메쉬");

        Long woodenChairId = productService.create(diningChair, "원목 식탁 의자", 120_000, 10, "원목 느낌");
        Long hiddenDiningId = productService.create(diningChair, "전시 상품", 90_000, 1, "숨김 테스트");
        productService.changeStatus(hiddenDiningId, ProductStatus.HIDDEN);

        Long bedId = productService.create(bed, "퀸사이즈 침대 프레임", 420_000, 4, "프레임 단품");
        Long storageId = productService.create(storage, "수납장", 160_000, 9, "거실 수납장");

        // 의류
        Long tshirtId = productService.create(tshirt, "베이직 티셔츠", 19_900, 50, "기본 티");
        productService.changeOriginalPrice(tshirtId, 29_900);

        Long shirtId = productService.create(shirt, "옥스포드 셔츠", 49_900, 20, "단정한 셔츠");

        Long jeansId = productService.create(bottoms, "데님 팬츠", 59_900, 18, "기본 데님");
        Long readyOuterId = productService.create(outerwear, "신상 코트", 189_000, 10, "판매 준비");
        productService.changeStatus(readyOuterId, ProductStatus.READY);

        // =========================
        // 주문 더미 (중복 create 금지: 위에서 만든 상품 ID 재사용)
        // =========================

        // 1) user1이 맥북 1개 주문
        orderService.create(
                userId1,
                macbookId,
                1,
                "테스트",
                "010-1111-2222",
                "06236",
                "서울특별시 강남구 테헤란로 123",
                "문 앞에 놔주세요"
        );

        // 2) user2가 무선 마우스 2개 주문 (기존 mouseId 재사용)
        orderService.create(
                userId2,
                mouseId,
                2,
                "테스트2",
                "010-1111-4444",
                "04157",
                "서울특별시 마포구 월드컵북로 1",
                null
        );

        // 3) user1이 QHD 모니터 1개 주문 (기존 qhdMonitorId 재사용)
        orderService.create(
                userId1,
                qhdMonitorId,
                1,
                "테스트",
                "010-1111-2222",
                "06236",
                "서울특별시 강남구 테헤란로 123",
                "경비실 맡겨주세요"
        );
    }
}
