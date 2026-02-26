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
@Profile("local")   // local 환경에서만 실행
@RequiredArgsConstructor
public class TestDataInitializer implements ApplicationRunner {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final MemberService memberService;
    private final OrderService orderService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        // 회원 생성
        Long userId1 = memberService.signup("test1@test.com", "pw12341234!", "테스트", "010-1111-2222");
        Long userId2 = memberService.signup("test2@test.com", "pw12341234!", "테스트2", "010-1111-4444");

        Long adminId1 = memberService.signup("testAdmin1@test.com", "pw12341234!", "테스트관리자", "010-1111-3333");
        memberService.changeRole(adminId1, MemberRole.ADMIN);

        // =========================
        // 카테고리 (2~3뎁스까지)
        // =========================
        Long electronics = categoryService.create("전자", null);
        Long furniture   = categoryService.create("가구", null);
        Long clothing    = categoryService.create("의류", null);

        // 전자 > 컴퓨터/휴대폰/주변기기
        Long computer = categoryService.create("컴퓨터", electronics);
        Long phone    = categoryService.create("휴대폰", electronics);
        Long accessory = categoryService.create("주변기기", electronics);

        // 전자 > 컴퓨터 > 노트북/데스크탑/모니터
        Long laptop  = categoryService.create("노트북", computer);
        Long desktop = categoryService.create("데스크탑", computer);
        Long monitor = categoryService.create("모니터", computer);

        // 전자 > 주변기기 > 키보드/마우스/오디오
        Long keyboard = categoryService.create("키보드", accessory);
        Long mouse    = categoryService.create("마우스", accessory);
        Long audio    = categoryService.create("오디오", accessory);

        // 가구 > 의자/침대/수납가구
        Long seating = categoryService.create("의자", furniture);
        Long bed     = categoryService.create("침대", furniture);
        Long storage = categoryService.create("수납가구", furniture);

        // 가구 > 의자 > 사무용/식탁용
        Long officeChair = categoryService.create("사무용 의자", seating);
        Long diningChair = categoryService.create("식탁 의자", seating);

        // 의류 > 상의/하의/아우터
        Long tops     = categoryService.create("상의", clothing);
        Long bottoms  = categoryService.create("하의", clothing);
        Long outerwear = categoryService.create("아우터", clothing);

        // 의류 > 상의 > 티셔츠/셔츠
        Long tshirt = categoryService.create("티셔츠", tops);
        Long shirt  = categoryService.create("셔츠", tops);

        // =========================
        // 상품 (카테고리/상태/재고 다양화)
        // =========================

        // 전자(루트)도 테스트용 1개
        productService.create(electronics, "전자제품 랜덤", 10_000, 10, "전자 카테고리 테스트 상품");

        // 컴퓨터 > 노트북
        Long macbookId = productService.create(laptop, "맥북 프로 14", 3_000_000, 5, "애플 노트북");
        productService.create(laptop, "윈도우 울트라북", 1_800_000, 8, "가벼운 업무용");
        Long readyLaptopId = productService.create(laptop, "출시 예정 노트북", 2_200_000, 20, "판매 준비");
        productService.changeStatus(readyLaptopId, ProductStatus.READY);

        // 컴퓨터 > 데스크탑
        productService.create(desktop, "게이밍 데스크탑", 2_500_000, 3, "RTX 탑재");
        Long soldOutDesktopId = productService.create(desktop, "미니 PC", 900_000, 0, "재고 0 테스트");

        // 컴퓨터 > 모니터
        productService.create(monitor, "27인치 QHD 모니터", 350_000, 12, "가성비 모니터");
        Long hiddenMonitorId = productService.create(monitor, "프로토타입 모니터", 1_200_000, 2, "숨김 테스트");
        productService.changeStatus(hiddenMonitorId, ProductStatus.HIDDEN);

        // 휴대폰
        productService.create(phone, "아이폰 15", 1_500_000, 0, "품절 상태 (재고 0)");
        productService.create(phone, "갤럭시 S24", 1_400_000, 7, "삼성 최신폰");
        Long readyPhoneId = productService.create(phone, "판매준비중", 1_500_000, 10, "판매 준비");
        productService.changeStatus(readyPhoneId, ProductStatus.READY);

        // 주변기기 > 키보드/마우스/오디오
        productService.create(keyboard, "기계식 키보드", 129_000, 25, "청축/갈축 랜덤");
        productService.create(mouse, "무선 마우스", 59_000, 30, "사무용");
        Long soldOutHeadsetId = productService.create(audio, "게이밍 헤드셋", 89_000, 0, "품절 테스트");

        // 가구 > 사무용 의자
        productService.create(officeChair, "인체공학 사무의자", 250_000, 6, "허리 지지대 포함");
        productService.create(officeChair, "메쉬 의자", 180_000, 15, "통풍 좋은 메쉬");

        // 가구 > 식탁 의자
        productService.create(diningChair, "원목 식탁 의자", 120_000, 10, "원목 느낌");
        Long hiddenDiningId = productService.create(diningChair, "전시 상품", 90_000, 1, "숨김 테스트");
        productService.changeStatus(hiddenDiningId, ProductStatus.HIDDEN);

        // 가구 > 침대/수납가구
        productService.create(bed, "퀸사이즈 침대 프레임", 420_000, 4, "프레임 단품");
        productService.create(storage, "수납장", 160_000, 9, "거실 수납장");

        // 의류 > 티셔츠/셔츠
        productService.create(tshirt, "베이직 티셔츠", 19_900, 50, "기본 티");
        productService.create(shirt, "옥스포드 셔츠", 49_900, 20, "단정한 셔츠");

        // 의류 > 하의/아우터
        productService.create(bottoms, "데님 팬츠", 59_900, 18, "기본 데님");
        Long readyOuterId = productService.create(outerwear, "신상 코트", 189_000, 10, "판매 준비");
        productService.changeStatus(readyOuterId, ProductStatus.READY);

        // =========================
        // 주문 더미 (관리자 화면 테스트용)
        // =========================

        // 1) user1이 맥북 1개 주문 (정상: PAYMENT_COMPLETED)
        Long orderId1 = orderService.create(
                userId1,
                macbookId,
                1,
                "테스트",
                "010-1111-2222",
                "06236",
                "서울특별시 강남구 테헤란로 123",
                "문 앞에 놔주세요"
        );

        // 2) user2가 무선마우스 2개 주문 (정상: PAYMENT_COMPLETED)
        // ※ 무선 마우스 id 변수가 없으니, 아래처럼 변수로 받아두는 게 좋음.
        //    productService.create(mouse, ...) 반환값을 mouseId로 받아두자.
        Long mouseId = productService.create(mouse, "무선 마우스(주문용)", 59_000, 30, "사무용"); // 이미 만들었다면 이 줄은 제거하고 기존 id 사용
        Long orderId2 = orderService.create(
                userId2,
                mouseId,
                2,
                "테스트2",
                "010-1111-4444",
                "04157",
                "서울특별시 마포구 월드컵북로 1",
                null
        );

        // 3) user1이 QHD 모니터 1개 주문 (정상: PAYMENT_COMPLETED)
        // 마찬가지로 monitor 상품 id가 필요하니 create 반환값을 변수로 받아두는 게 깔끔함.
        Long qhdMonitorId = productService.create(monitor, "27인치 QHD 모니터(주문용)", 350_000, 12, "가성비 모니터"); // 이미 만들었다면 제거
        Long orderId3 = orderService.create(
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
