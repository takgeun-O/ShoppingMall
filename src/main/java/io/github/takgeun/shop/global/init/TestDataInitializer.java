package io.github.takgeun.shop.global.init;

import io.github.takgeun.shop.category.application.CategoryService;
import io.github.takgeun.shop.member.application.MemberService;
import io.github.takgeun.shop.member.domain.MemberRole;
import io.github.takgeun.shop.member.domain.MemberStatus;
import io.github.takgeun.shop.order.application.OrderService;
import io.github.takgeun.shop.order.application.dto.CreateOrderCommand;
import io.github.takgeun.shop.order.dto.request.CheckoutItem;
import io.github.takgeun.shop.product.api.dto.request.ProductCreateRequest;
import io.github.takgeun.shop.product.application.ProductService;
import io.github.takgeun.shop.product.domain.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

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
        // 카테고리 (2단까지만 허용)
        // =========================
        Long electronics = categoryService.create("전자", null);
        Long furniture = categoryService.create("가구", null);
        Long clothing = categoryService.create("의류", null);

        Long computer = categoryService.create("컴퓨터", electronics);
        Long phone = categoryService.create("휴대폰", electronics);
        Long accessory = categoryService.create("주변기기", electronics);

        Long seating = categoryService.create("의자", furniture);
        Long bed = categoryService.create("침대", furniture);
        Long storage = categoryService.create("수납가구", furniture);

        Long tops = categoryService.create("상의", clothing);
        Long bottoms = categoryService.create("하의", clothing);
        Long outerwear = categoryService.create("아우터", clothing);

        // =========================
        // 상품
        // =========================

        // 전자(루트) 테스트 1개
        Long electronicsTestId = createProduct(
                electronics,
                "전자제품 랜덤",
                10_000,
                10,
                "전자 카테고리 테스트 상품",
                img("electronics-random"),
                null,
                ProductStatus.ON_SALE
        );

        // 전자 > 컴퓨터
        Long macbookId = createProduct(
                computer,
                "맥북 프로 14",
                3_000_000,
                5,
                "애플 노트북",
                img("macbook-pro"),
                3_500_000,
                ProductStatus.ON_SALE
        );

        Long ultrabookId = createProduct(
                computer,
                "윈도우 울트라북",
                1_800_000,
                8,
                "가벼운 업무용",
                img("ultrabook"),
                2_100_000,
                ProductStatus.ON_SALE
        );

        Long readyLaptopId = createProduct(
                computer,
                "출시 예정 노트북",
                2_200_000,
                20,
                "판매 준비",
                img("laptop-upcoming"),
                null,
                ProductStatus.READY
        );

        Long gamingDesktopId = createProduct(
                computer,
                "게이밍 데스크탑",
                2_500_000,
                3,
                "RTX 탑재",
                img("gaming-desktop"),
                null,
                ProductStatus.ON_SALE
        );

        Long miniPcId = createProduct(
                computer,
                "미니 PC",
                900_000,
                0,
                "품절 상태 테스트",
                img("mini-pc"),
                null,
                ProductStatus.SOLD_OUT
        );

        Long qhdMonitorId = createProduct(
                computer,
                "27인치 QHD 모니터",
                350_000,
                12,
                "가성비 모니터",
                img("monitor-qhd"),
                420_000,
                ProductStatus.ON_SALE
        );

        Long hiddenMonitorId = createProduct(
                computer,
                "프로토타입 모니터",
                1_200_000,
                2,
                "숨김 테스트",
                img("prototype-monitor"),
                null,
                ProductStatus.HIDDEN
        );

        // 전자 > 휴대폰
        Long iphoneId = createProduct(
                phone,
                "아이폰 15",
                1_500_000,
                0,
                "품절 상태 (재고 0)",
                img("iphone"),
                null,
                ProductStatus.SOLD_OUT
        );

        Long galaxyId = createProduct(
                phone,
                "갤럭시 S24",
                1_400_000,
                7,
                "삼성 최신폰",
                img("galaxy"),
                1_550_000,
                ProductStatus.ON_SALE
        );

        Long readyPhoneId = createProduct(
                phone,
                "판매준비중",
                1_500_000,
                10,
                "판매 준비",
                img("phone-upcoming"),
                null,
                ProductStatus.READY
        );

        // 전자 > 주변기기
        Long keyboardId = createProduct(
                accessory,
                "기계식 키보드",
                129_000,
                25,
                "청축/갈축 랜덤",
                img("keyboard"),
                null,
                ProductStatus.ON_SALE
        );

        Long mouseId = createProduct(
                accessory,
                "무선 마우스",
                59_000,
                30,
                "사무용",
                img("mouse"),
                null,
                ProductStatus.ON_SALE
        );

        Long headsetId = createProduct(
                accessory,
                "게이밍 헤드셋",
                89_000,
                0,
                "품절 테스트",
                img("headset"),
                null,
                ProductStatus.SOLD_OUT
        );

        // 가구 > 의자
        Long ergoChairId = createProduct(
                seating,
                "인체공학 사무의자",
                250_000,
                6,
                "허리 지지대 포함",
                img("ergonomic-chair"),
                299_000,
                ProductStatus.ON_SALE
        );

        Long meshChairId = createProduct(
                seating,
                "메쉬 의자",
                180_000,
                15,
                "통풍 좋은 메쉬",
                img("mesh-chair"),
                null,
                ProductStatus.ON_SALE
        );

        Long woodenChairId = createProduct(
                seating,
                "원목 식탁 의자",
                120_000,
                10,
                "원목 느낌",
                img("wood-chair"),
                null,
                ProductStatus.ON_SALE
        );

        Long hiddenDiningId = createProduct(
                seating,
                "전시 상품",
                90_000,
                1,
                "숨김 테스트",
                img("display-chair"),
                null,
                ProductStatus.HIDDEN
        );

        // 가구 > 침대
        Long bedId = createProduct(
                bed,
                "퀸사이즈 침대 프레임",
                420_000,
                4,
                "프레임 단품",
                img("bed"),
                null,
                ProductStatus.ON_SALE
        );

        // 가구 > 수납가구
        Long storageId = createProduct(
                storage,
                "수납장",
                160_000,
                9,
                "거실 수납장",
                img("storage-cabinet"),
                null,
                ProductStatus.ON_SALE
        );

        // 의류 > 상의
        Long tshirtId = createProduct(
                tops,
                "베이직 티셔츠",
                19_900,
                50,
                "기본 티",
                img("tshirt"),
                29_900,
                ProductStatus.ON_SALE
        );

        Long shirtId = createProduct(
                tops,
                "옥스포드 셔츠",
                49_900,
                20,
                "단정한 셔츠",
                img("shirt"),
                null,
                ProductStatus.ON_SALE
        );

        // 의류 > 하의
        Long jeansId = createProduct(
                bottoms,
                "데님 팬츠",
                59_900,
                18,
                "기본 데님",
                img("jeans"),
                null,
                ProductStatus.ON_SALE
        );

        // 의류 > 아우터
        Long readyOuterId = createProduct(
                outerwear,
                "신상 코트",
                189_000,
                10,
                "판매 준비",
                img("coat"),
                null,
                ProductStatus.READY
        );

        // 사용만 하고 경고 안 나게
        Long unused1 = electronicsTestId;
        Long unused2 = readyLaptopId;
        Long unused3 = miniPcId;
        Long unused4 = hiddenMonitorId;
        Long unused5 = iphoneId;
        Long unused6 = readyPhoneId;
        Long unused7 = headsetId;
        Long unused8 = hiddenDiningId;
        Long unused9 = bedId;
        Long unused10 = readyOuterId;

        // =========================
        // 주문 더미
        // =========================

        // 1) user1: 맥북 1개 주문
        orderService.checkout(
                userId1,
                List.of(CheckoutItem.of(macbookId, 1)),
                createOrderCommand(
                        userId1,
                        "테스트",
                        "010-1111-2222",
                        "06236",
                        "서울특별시 강남구 테헤란로 123",
                        "101동 202호",
                        "문 앞에 놔주세요"
                )
        );

        // 2) user2: 무선 마우스 2개 주문
        orderService.checkout(
                userId2,
                List.of(CheckoutItem.of(mouseId, 2)),
                createOrderCommand(
                        userId2,
                        "테스트2",
                        "010-1111-4444",
                        "04157",
                        "서울특별시 마포구 월드컵북로 1",
                        "3층",
                        null
                )
        );

        // 3) user1: QHD 모니터 1개 + 키보드 1개
        orderService.checkout(
                userId1,
                List.of(
                        CheckoutItem.of(qhdMonitorId, 1),
                        CheckoutItem.of(keyboardId, 1)
                ),
                createOrderCommand(
                        userId1,
                        "테스트",
                        "010-1111-2222",
                        "06236",
                        "서울특별시 강남구 테헤란로 123",
                        "101동 202호",
                        "경비실에 맡겨주세요"
                )
        );

        // 4) user1: 갤럭시 S24 1개
        orderService.checkout(
                userId1,
                List.of(CheckoutItem.of(galaxyId, 1)),
                createOrderCommand(
                        userId1,
                        "테스트",
                        "010-1111-2222",
                        "06236",
                        "서울특별시 강남구 테헤란로 123",
                        "101동 202호",
                        "빠른 배송 부탁드립니다."
                )
        );

        // 5) user1: 인체공학 사무의자 1개
        orderService.checkout(
                userId1,
                List.of(CheckoutItem.of(ergoChairId, 1)),
                createOrderCommand(
                        userId1,
                        "테스트",
                        "010-1111-2222",
                        "06236",
                        "서울특별시 강남구 테헤란로 123",
                        "101동 202호",
                        null
                )
        );

        // 6) user1: 베이직 티셔츠 2개 + 데님 팬츠 1개
        orderService.checkout(
                userId1,
                List.of(
                        CheckoutItem.of(tshirtId, 2),
                        CheckoutItem.of(jeansId, 1)
                ),
                createOrderCommand(
                        userId1,
                        "테스트",
                        "010-1111-2222",
                        "06236",
                        "서울특별시 강남구 테헤란로 123",
                        "101동 202호",
                        "부재 시 문 앞에 놔주세요"
                )
        );

        // 7) user1: 원목 식탁 의자 2개
        orderService.checkout(
                userId1,
                List.of(CheckoutItem.of(woodenChairId, 2)),
                createOrderCommand(
                        userId1,
                        "테스트",
                        "010-1111-2222",
                        "06236",
                        "서울특별시 강남구 테헤란로 123",
                        "101동 202호",
                        null
                )
        );

        // 8) user1: 윈도우 울트라북 1개
        orderService.checkout(
                userId1,
                List.of(CheckoutItem.of(ultrabookId, 1)),
                createOrderCommand(
                        userId1,
                        "테스트",
                        "010-1111-2222",
                        "06236",
                        "서울특별시 강남구 테헤란로 123",
                        "101동 202호",
                        "평일 오후 배송 희망"
                )
        );

        // 9) user1: 수납장 1개 + 메쉬 의자 1개
        orderService.checkout(
                userId1,
                List.of(
                        CheckoutItem.of(storageId, 1),
                        CheckoutItem.of(meshChairId, 1)
                ),
                createOrderCommand(
                        userId1,
                        "테스트",
                        "010-1111-2222",
                        "06236",
                        "서울특별시 강남구 테헤란로 123",
                        "101동 202호",
                        null
                )
        );

        // 10) user1: 옥스포드 셔츠 2개
        orderService.checkout(
                userId1,
                List.of(CheckoutItem.of(shirtId, 2)),
                createOrderCommand(
                        userId1,
                        "테스트",
                        "010-1111-2222",
                        "06236",
                        "서울특별시 강남구 테헤란로 123",
                        "101동 202호",
                        "옷걸이 포장 부탁드립니다."
                )
        );

        // 11) user1: 게이밍 데스크탑 1개
        orderService.checkout(
                userId1,
                List.of(CheckoutItem.of(gamingDesktopId, 1)),
                createOrderCommand(
                        userId1,
                        "테스트",
                        "010-1111-2222",
                        "06236",
                        "서울특별시 강남구 테헤란로 123",
                        "101동 202호",
                        "파손 주의"
                )
        );

        // 12) user1: 수납장 1개
        orderService.checkout(
                userId1,
                List.of(CheckoutItem.of(storageId, 1)),
                createOrderCommand(
                        userId1,
                        "테스트",
                        "010-1111-2222",
                        "06236",
                        "서울특별시 강남구 테헤란로 123",
                        "101동 202호",
                        null
                )
        );

        // 13) user1: 베이직 티셔츠 3개
        orderService.checkout(
                userId1,
                List.of(CheckoutItem.of(tshirtId, 3)),
                createOrderCommand(
                        userId1,
                        "테스트",
                        "010-1111-2222",
                        "06236",
                        "서울특별시 강남구 테헤란로 123",
                        "101동 202호",
                        "주말 전 배송 부탁드립니다."
                )
        );

        // =========================
        // 회원관리 페이징 테스트용 추가 회원/주문 더미
        // =========================
        initMemberPagingTestData(
                macbookId,
                mouseId,
                qhdMonitorId,
                keyboardId,
                galaxyId,
                tshirtId,
                jeansId,
                storageId,
                meshChairId
        );
    }

    private void initMemberPagingTestData(
            Long macbookId,
            Long mouseId,
            Long qhdMonitorId,
            Long keyboardId,
            Long galaxyId,
            Long tshirtId,
            Long jeansId,
            Long storageId,
            Long meshChairId
    ) {
        for (int i = 3; i <= 26; i++) {
            String email = "paging" + i + "@test.com";
            String name = buildMemberName(i);
            String phone = String.format("010-2000-%04d", i);

            Long memberId = memberService.signup(email, "pw12341234!", name, phone);

            // 주문은 ACTIVE 상태일 때만 먼저 생성
            if (i % 2 == 0) {
                orderService.checkout(
                        memberId,
                        List.of(CheckoutItem.of(mouseId, 1)),
                        createOrderCommand(
                                memberId,
                                name,
                                phone,
                                "04524",
                                "서울특별시 중구 세종대로 " + i,
                                i + "층",
                                null
                        )
                );
            }

            if (i % 3 == 0) {
                orderService.checkout(
                        memberId,
                        List.of(
                                CheckoutItem.of(tshirtId, 2),
                                CheckoutItem.of(jeansId, 1)
                        ),
                        createOrderCommand(
                                memberId,
                                name,
                                phone,
                                "06236",
                                "서울특별시 강남구 테헤란로 " + (100 + i),
                                (i % 5 + 1) + "01호",
                                "부재 시 문 앞에 놓아주세요"
                        )
                );
            }

            if (i % 5 == 0) {
                orderService.checkout(
                        memberId,
                        List.of(
                                CheckoutItem.of(qhdMonitorId, 1),
                                CheckoutItem.of(keyboardId, 1)
                        ),
                        createOrderCommand(
                                memberId,
                                name,
                                phone,
                                "04157",
                                "서울특별시 마포구 월드컵북로 " + i,
                                "A동 " + (200 + i) + "호",
                                "평일 오후 배송 희망"
                        )
                );
            }

            if (i == 7 || i == 13 || i == 19) {
                orderService.checkout(
                        memberId,
                        List.of(CheckoutItem.of(galaxyId, 1)),
                        createOrderCommand(
                                memberId,
                                name,
                                phone,
                                "48058",
                                "부산광역시 해운대구 센텀동로 " + i,
                                "센텀빌딩 " + i + "층",
                                "빠른 배송 부탁드립니다."
                        )
                );
            }

            if (i == 9 || i == 15 || i == 21) {
                orderService.checkout(
                        memberId,
                        List.of(
                                CheckoutItem.of(storageId, 1),
                                CheckoutItem.of(meshChairId, 1)
                        ),
                        createOrderCommand(
                                memberId,
                                name,
                                phone,
                                "35229",
                                "대전광역시 서구 둔산로 " + i,
                                "오피스텔 " + i + "01호",
                                null
                        )
                );
            }

            if (i == 11 || i == 17) {
                orderService.checkout(
                        memberId,
                        List.of(CheckoutItem.of(macbookId, 1)),
                        createOrderCommand(
                                memberId,
                                name,
                                phone,
                                "13529",
                                "경기도 성남시 분당구 판교역로 " + i,
                                "테크노밸리 " + i + "층",
                                "파손 주의"
                        )
                );
            }

            // 마지막에 상태 변경
            if (i % 10 == 0) {
                memberService.changeStatus(memberId, MemberStatus.WITHDRAWN);
            } else if (i % 4 == 0) {
                memberService.changeStatus(memberId, MemberStatus.INACTIVE);
            }
        }
    }

    private String buildMemberName(int index) {
        return switch (index % 8) {
            case 0 -> "김테스트" + index;
            case 1 -> "이페이징" + index;
            case 2 -> "박회원" + index;
            case 3 -> "최사용자" + index;
            case 4 -> "정관리확인" + index;
            case 5 -> "한목록" + index;
            case 6 -> "윤검색" + index;
            default -> "장샘플" + index;
        };
    }

    private Long createProduct(
            Long categoryId,
            String name,
            int price,
            int stock,
            String description,
            String imageUrl,
            Integer originalPrice,
            ProductStatus status
    ) {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setCategoryId(categoryId);
        request.setName(name);
        request.setPrice(price);
        request.setStock(stock);
        request.setDescription(description);
        request.setImageUrl(imageUrl);
        request.setOriginalPrice(originalPrice);
        request.setStatus(status);

        return productService.create(
                request.getCategoryId(),
                request.getName(),
                request.getPrice(),
                request.getStock(),
                request.getDescription(),
                request.getStatus(),
                request.getOriginalPrice(),
                request.getImageUrl()
        );
    }

    private CreateOrderCommand createOrderCommand(
            Long memberId,
            String recipientName,
            String phoneNumber,
            String zipCode,
            String address,
            String addressDetail,
            String requestMessage
    ) {
        return new CreateOrderCommand(
                recipientName,
                phoneNumber,
                zipCode,
                address,
                addressDetail,
                requestMessage,
                generateRequestKey(memberId)
        );
    }

    private String generateRequestKey(Long memberId) {
        return "seed:" + memberId + ":" + UUID.randomUUID();
    }

    private String img(String key) {
        return switch (key) {
            case "electronics-random" -> "https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&w=800&q=80";
            case "macbook-pro" -> "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=800&q=80";
            case "ultrabook" -> "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=800&q=80";
            case "laptop-upcoming" -> "https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?auto=format&fit=crop&w=800&q=80";

            case "gaming-desktop" -> "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?auto=format&fit=crop&w=800&q=80";
            case "mini-pc" -> "https://images.unsplash.com/photo-1593642702821-c8da6771f0c6?auto=format&fit=crop&w=800&q=80";

            case "monitor-qhd" -> "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=80";
            case "prototype-monitor" -> "https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=800&q=80";

            case "iphone" -> "https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=800&q=80";
            case "galaxy" -> "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?auto=format&fit=crop&w=800&q=80";
            case "phone-upcoming" -> "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=800&q=80";

            case "keyboard" -> "https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=800&q=80";
            case "mouse" -> "https://images.unsplash.com/photo-1527814050087-3793815479db?auto=format&fit=crop&w=800&q=80";
            case "headset" -> "https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=800&q=80";

            case "ergonomic-chair" -> "https://images.unsplash.com/photo-1580480055273-228ff5388ef8?auto=format&fit=crop&w=800&q=80";
            case "mesh-chair" -> "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=800&q=80";
            case "wood-chair" -> "https://images.unsplash.com/photo-1519947486511-46149fa0a254?auto=format&fit=crop&w=800&q=80";
            case "display-chair" -> "https://images.unsplash.com/photo-1501045661006-fcebe0257c3f?auto=format&fit=crop&w=800&q=80";

            case "bed" -> "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=800&q=80";
            case "storage-cabinet" -> "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=800&q=80";

            case "tshirt" -> "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80";
            case "shirt" -> "https://images.unsplash.com/photo-1603252109303-2751441dd157?auto=format&fit=crop&w=800&q=80";
            case "jeans" -> "https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=800&q=80";
            case "coat" -> "https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=800&q=80";

            default -> "/images/no-image.png";
        };
    }
}