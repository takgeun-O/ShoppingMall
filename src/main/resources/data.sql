SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM product;
DELETE FROM category;
DELETE FROM member;
DELETE FROM order_items;
DELETE FROM orders;

SET FOREIGN_KEY_CHECKS = 1;

-- Category

-- ROOT
INSERT INTO category (
    id,
    name,
    name_key,
    slug,
    parent_id,
    status
) VALUES
      (1, '전자', '전자', 'electronics', NULL, 'ACTIVE'),
      (2, '가구', '가구', 'furniture', NULL, 'ACTIVE'),
      (3, '의류', '의류', 'clothing', NULL, 'ACTIVE');

-- ROOT 추가
INSERT INTO category (
    id,
    name,
    name_key,
    slug,
    parent_id,
    status
) VALUES
      (13, '뷰티', '뷰티', 'beauty', NULL, 'ACTIVE'),
      (14, '스포츠', '스포츠', 'sports', NULL, 'ACTIVE'),
      (15, '도서', '도서', 'books', NULL, 'ACTIVE'),
      (16, '식품', '식품', 'food', NULL, 'ACTIVE'),
      (17, '생활용품', '생활용품', 'home-living', NULL, 'ACTIVE');


-- 전자 하위
INSERT INTO category (
    id,
    name,
    name_key,
    slug,
    parent_id,
    status
) VALUES
      (4, '컴퓨터', '컴퓨터', 'computer', 1, 'ACTIVE'),
      (5, '휴대폰', '휴대폰', 'phone', 1, 'ACTIVE'),
      (6, '주변기기', '주변기기', 'accessory', 1, 'ACTIVE');

-- 가구 하위
INSERT INTO category (
    id,
    name,
    name_key,
    slug,
    parent_id,
    status
) VALUES
      (7, '의자', '의자', 'seating', 2, 'ACTIVE'),
      (8, '침대', '침대', 'bed', 2, 'ACTIVE'),
      (9, '수납가구', '수납가구', 'storage', 2, 'ACTIVE');

-- 의류 하위
INSERT INTO category (
    id,
    name,
    name_key,
    slug,
    parent_id,
    status
) VALUES
      (10, '상의', '상의', 'tops', 3, 'ACTIVE'),
      (11, '하의', '하의', 'bottoms', 3, 'ACTIVE'),
      (12, '아우터', '아우터', 'outerwear', 3, 'ACTIVE');

-- 뷰티 하위
INSERT INTO category (
    id,
    name,
    name_key,
    slug,
    parent_id,
    status
) VALUES
      (18, '스킨케어', '스킨케어', 'skincare', 13, 'ACTIVE'),
      (19, '메이크업', '메이크업', 'makeup', 13, 'ACTIVE'),
      (20, '헤어케어', '헤어케어', 'haircare', 13, 'ACTIVE');

-- 스포츠 하위
INSERT INTO category (
    id,
    name,
    name_key,
    slug,
    parent_id,
    status
) VALUES
      (21, '헬스용품', '헬스용품', 'fitness', 14, 'ACTIVE'),
      (22, '캠핑', '캠핑', 'camping', 14, 'ACTIVE'),
      (23, '러닝', '러닝', 'running', 14, 'ACTIVE');

-- 도서 하위
INSERT INTO category (
    id,
    name,
    name_key,
    slug,
    parent_id,
    status
) VALUES
      (24, '소설', '소설', 'novel', 15, 'ACTIVE'),
      (25, '개발서', '개발서', 'programming-books', 15, 'ACTIVE'),
      (26, '자기계발', '자기계발', 'self-improvement', 15, 'ACTIVE');

-- 식품 하위
INSERT INTO category (
    id,
    name,
    name_key,
    slug,
    parent_id,
    status
) VALUES
      (27, '간편식', '간편식', 'meal-kit', 16, 'ACTIVE'),
      (28, '음료', '음료', 'beverage', 16, 'ACTIVE'),
      (29, '건강식품', '건강식품', 'health-food', 16, 'ACTIVE');

-- 생활용품 하위
INSERT INTO category (
    id,
    name,
    name_key,
    slug,
    parent_id,
    status
) VALUES
      (30, '주방용품', '주방용품', 'kitchen', 17, 'ACTIVE'),
      (31, '욕실용품', '욕실용품', 'bathroom', 17, 'ACTIVE'),
      (32, '청소용품', '청소용품', 'cleaning', 17, 'ACTIVE');

-- Product
INSERT INTO product (
    id,
    category_id,
    name,
    price,
    stock,
    description,
    image_url,
    original_price,
    status
) VALUES
      -- 전자 루트
      (
          1,
          1,
          '전자제품 랜덤',
          10000,
          10,
          '전자 카테고리 테스트 상품',
          'https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),

      -- 컴퓨터
      (
          2,
          4,
          '맥북 프로 14',
          3000000,
          5,
          '애플 노트북',
          'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=800&q=80',
          3500000,
          'ON_SALE'
      ),
      (
          3,
          4,
          '윈도우 울트라북',
          1800000,
          8,
          '가벼운 업무용',
          'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=800&q=80',
          2100000,
          'ON_SALE'
      ),
      (
          4,
          4,
          '출시 예정 노트북',
          2200000,
          20,
          '판매 준비',
          'https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?auto=format&fit=crop&w=800&q=80',
          NULL,
          'READY'
      ),
      (
          5,
          4,
          '게이밍 데스크탑',
          2500000,
          3,
          'RTX 탑재',
          'https://images.unsplash.com/photo-1587202372775-e229f172b9d7?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),
      (
          6,
          4,
          '미니 PC',
          900000,
          0,
          '품절 상태 테스트',
          'https://images.unsplash.com/photo-1593642702821-c8da6771f0c6?auto=format&fit=crop&w=800&q=80',
          NULL,
          'SOLD_OUT'
      ),
      (
          7,
          4,
          '27인치 QHD 모니터',
          350000,
          12,
          '가성비 모니터',
          'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=80',
          420000,
          'ON_SALE'
      ),
      (
          8,
          4,
          '프로토타입 모니터',
          1200000,
          2,
          '숨김 테스트',
          'https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=800&q=80',
          NULL,
          'HIDDEN'
      ),

      -- 휴대폰
      (
          9,
          5,
          '아이폰 15',
          1500000,
          0,
          '품절 상태 (재고 0)',
          'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=800&q=80',
          NULL,
          'SOLD_OUT'
      ),
      (
          10,
          5,
          '갤럭시 S24',
          1400000,
          7,
          '삼성 최신폰',
          'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?auto=format&fit=crop&w=800&q=80',
          1550000,
          'ON_SALE'
      ),
      (
          11,
          5,
          '판매준비중',
          1500000,
          10,
          '판매 준비',
          'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=800&q=80',
          NULL,
          'READY'
      ),

      -- 주변기기
      (
          12,
          6,
          '기계식 키보드',
          129000,
          25,
          '청축/갈축 랜덤',
          'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),
      (
          13,
          6,
          '무선 마우스',
          59000,
          30,
          '사무용',
          'https://images.unsplash.com/photo-1527814050087-3793815479db?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),
      (
          14,
          6,
          '게이밍 헤드셋',
          89000,
          0,
          '품절 테스트',
          'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=800&q=80',
          NULL,
          'SOLD_OUT'
      ),

      -- 의자
      (
          15,
          7,
          '인체공학 사무의자',
          250000,
          6,
          '허리 지지대 포함',
          'https://images.unsplash.com/photo-1580480055273-228ff5388ef8?auto=format&fit=crop&w=800&q=80',
          299000,
          'ON_SALE'
      ),
      (
          16,
          7,
          '메쉬 의자',
          180000,
          15,
          '통풍 좋은 메쉬',
          'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),
      (
          17,
          7,
          '원목 식탁 의자',
          120000,
          10,
          '원목 느낌',
          'https://images.unsplash.com/photo-1519947486511-46149fa0a254?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),
      (
          18,
          7,
          '전시 상품',
          90000,
          1,
          '숨김 테스트',
          'https://images.unsplash.com/photo-1501045661006-fcebe0257c3f?auto=format&fit=crop&w=800&q=80',
          NULL,
          'HIDDEN'
      ),

      -- 침대 / 수납가구
      (
          19,
          8,
          '퀸사이즈 침대 프레임',
          420000,
          4,
          '프레임 단품',
          'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),
      (
          20,
          9,
          '수납장',
          160000,
          9,
          '거실 수납장',
          'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),

      -- 의류
      (
          21,
          10,
          '베이직 티셔츠',
          19900,
          50,
          '기본 티',
          'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&q=80',
          29900,
          'ON_SALE'
      ),
      (
          22,
          10,
          '옥스포드 셔츠',
          49900,
          20,
          '단정한 셔츠',
          'https://images.unsplash.com/photo-1603252109303-2751441dd157?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),
      (
          23,
          11,
          '데님 팬츠',
          59900,
          18,
          '기본 데님',
          'https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),
      (
          24,
          12,
          '신상 코트',
          189000,
          10,
          '판매 준비',
          'https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=800&q=80',
          NULL,
          'READY'
      );

-- =========================================================
-- 추가 상품
-- =========================================================
INSERT INTO product (
    id,
    category_id,
    name,
    price,
    stock,
    description,
    image_url,
    original_price,
    status
) VALUES
      -- 뷰티 > 스킨케어
      (
          25,
          18,
          '수분 진정 토너',
          22000,
          40,
          '민감 피부용 저자극 토너',
          'https://images.unsplash.com/photo-1620916566398-39f1143ab7be?auto=format&fit=crop&w=800&q=80',
          28000,
          'ON_SALE'
      ),
      (
          26,
          18,
          '데일리 보습 크림',
          32000,
          28,
          '사계절 사용 가능한 보습 크림',
          'https://images.unsplash.com/photo-1556228578-8c89e6adf883?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),

      -- 뷰티 > 메이크업
      (
          27,
          19,
          '쿠션 파운데이션',
          29000,
          18,
          '자연스러운 커버 메이크업',
          'https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?auto=format&fit=crop&w=800&q=80',
          35000,
          'ON_SALE'
      ),
      (
          28,
          19,
          '립 틴트 세트',
          19000,
          0,
          '품절 테스트용 립 틴트',
          'https://images.unsplash.com/photo-1586495777744-4413f21062fa?auto=format&fit=crop&w=800&q=80',
          NULL,
          'SOLD_OUT'
      ),

      -- 뷰티 > 헤어케어
      (
          29,
          20,
          '단백질 샴푸',
          17000,
          35,
          '손상모 케어용 샴푸',
          'https://images.unsplash.com/photo-1527799820374-dcf8d9d4a388?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),

      -- 스포츠 > 헬스용품
      (
          30,
          21,
          '덤벨 5kg 세트',
          45000,
          12,
          '홈트 입문용 덤벨',
          'https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=800&q=80',
          52000,
          'ON_SALE'
      ),
      (
          31,
          21,
          '요가 매트',
          25000,
          22,
          '미끄럼 방지 매트',
          'https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),

      -- 스포츠 > 캠핑
      (
          32,
          22,
          '2인용 캠핑 텐트',
          129000,
          7,
          '간편 설치형 텐트',
          'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=800&q=80',
          149000,
          'ON_SALE'
      ),
      (
          33,
          22,
          '캠핑 랜턴',
          39000,
          16,
          '충전식 LED 랜턴',
          'https://images.unsplash.com/photo-1504280390368-397dc5f11232?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),

      -- 스포츠 > 러닝
      (
          34,
          23,
          '러닝화',
          99000,
          20,
          '쿠셔닝 좋은 입문용 러닝화',
          'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=80',
          119000,
          'ON_SALE'
      ),
      (
          35,
          23,
          '러닝 캡',
          22000,
          14,
          '통기성 좋은 기능성 모자',
          'https://images.unsplash.com/photo-1521369909029-2afed882baee?auto=format&fit=crop&w=800&q=80',
          NULL,
          'READY'
      ),

      -- 도서 > 소설
      (
          36,
          24,
          '추리 장편 소설',
          16800,
          50,
          '몰입감 있는 미스터리 소설',
          'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),

      -- 도서 > 개발서
      (
          37,
          25,
          '자바 백엔드 실전 입문',
          32000,
          25,
          'Spring 기반 백엔드 개발 입문서',
          'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=800&q=80',
          38000,
          'ON_SALE'
      ),
      (
          38,
          25,
          'SQL 핵심 가이드',
          28000,
          17,
          '실무 중심 SQL 학습서',
          'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),

      -- 도서 > 자기계발
      (
          39,
          26,
          '업무 습관 개선법',
          18500,
          31,
          '생산성을 높이는 습관 정리',
          'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),

      -- 식품 > 간편식
      (
          40,
          27,
          '매콤 닭가슴살 볶음밥',
          5900,
          80,
          '전자레인지 간편조리',
          'https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),
      (
          41,
          27,
          '소고기 미역국 밀키트',
          8900,
          26,
          '1~2인분 간편식',
          'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=800&q=80',
          9900,
          'ON_SALE'
      ),

      -- 식품 > 음료
      (
          42,
          28,
          '콜드브루 원액',
          15900,
          33,
          '대용량 커피 원액',
          'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),
      (
          43,
          28,
          '제로 탄산음료 세트',
          12900,
          0,
          '품절 테스트용 음료 세트',
          'https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=800&q=80',
          NULL,
          'SOLD_OUT'
      ),

      -- 식품 > 건강식품
      (
          44,
          29,
          '종합 비타민',
          24000,
          21,
          '하루 1정 건강관리',
          'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?auto=format&fit=crop&w=800&q=80',
          30000,
          'ON_SALE'
      ),

      -- 생활용품 > 주방용품
      (
          45,
          30,
          '스테인리스 프라이팬',
          39000,
          13,
          '가정용 28cm 프라이팬',
          'https://images.unsplash.com/photo-1584990347449-ae77c5f0e635?auto=format&fit=crop&w=800&q=80',
          49000,
          'ON_SALE'
      ),
      (
          46,
          30,
          '전자저울',
          17000,
          19,
          '베이킹용 디지털 저울',
          'https://images.unsplash.com/photo-1514996937319-344454492b37?auto=format&fit=crop&w=800&q=80',
          NULL,
          'ON_SALE'
      ),

      -- 생활용품 > 욕실용품
      (
          47,
          31,
          '호텔식 수건 5장 세트',
          25000,
          24,
          '도톰한 면 수건',
          'https://images.unsplash.com/photo-1584622781564-1d987f7333c1?auto=format&fit=crop&w=800&q=80',
          32000,
          'ON_SALE'
      ),

      -- 생활용품 > 청소용품
      (
          48,
          32,
          '무선 핸디 청소기',
          79000,
          11,
          '차량/원룸 청소용',
          'https://images.unsplash.com/photo-1558317374-067fb5f30001?auto=format&fit=crop&w=800&q=80',
          99000,
          'ON_SALE'
      ),
      (
          49,
          32,
          '리필형 청소포',
          8900,
          60,
          '대용량 청소포 리필',
          'https://images.unsplash.com/photo-1563453392212-326f5e854473?auto=format&fit=crop&w=800&q=80',
          NULL,
          'HIDDEN'
      );

-- Member
INSERT INTO member (
                    id,
                    email,
                    password,
                    name,
                    phone,
                    role,
                    status,
                    created_at,
                    last_login_at
) VALUES (
             1,
             'user1@test.com',
             '$2y$10$782aPyMGYv6/z7jBkkVZqu5L3MPgbopNA253uw2CYJk.EUjfyvbpe',
             '일반회원',
             '010-1111-1111',
             'USER',
             'ACTIVE',
             '2026-03-29 10:00:00',
             '2026-03-29 18:30:00'
         );


-- Orders
INSERT INTO orders (
    id,
    order_number,
    member_id,
    status,
    request_key,
    recipient_name,
    recipient_phone,
    shipping_zip_code,
    shipping_address,
    shipping_address_detail,
    request_message,
    subtotal,
    shipping_fee,
    total_price,
    ordered_at,
    updated_at
) VALUES
      (
          1,
          'ORDER-20260329-0001',
          1,
          'ORDERED',
          'req-key-1',
          '일반회원',
          '010-1111-1111',
          '06236',
          '서울 강남구 테헤란로',
          '101호',
          '문 앞에 놔주세요',
          3059000,
          0,
          3059000,
          '2026-03-29 12:00:00',
          '2026-03-29 12:00:00'
      ),
      (
          2,
          'ORDER-20260329-0002',
          1,
          'PAYMENT_COMPLETED',
          'req-key-2',
          '일반회원',
          '010-1111-1111',
          '06236',
          '서울 강남구 테헤란로',
          '101호',
          NULL,
          1490000,
          0,
          1490000,
          '2026-03-29 13:00:00',
          '2026-03-29 13:00:00'
      ),
      (
          3,
          'ORDER-20260329-0003',
          1,
          'ORDERED',
          'req-key-3',
          '관리자',
          '010-9999-9999',
          '04524',
          '서울 중구 세종대로',
          '202호',
          '경비실에 맡겨주세요',
          188900,
          3000,
          191900,
          '2026-03-29 14:00:00',
          '2026-03-29 14:00:00'
      );


-- Order Items
INSERT INTO order_items (
    id,
    order_id,
    product_id,
    product_name_snapshot,
    unit_price_snapshot,
    original_price_snapshot,
    quantity,
    image_url_snapshot
) VALUES
      -- 주문 1 (맥북 + 마우스)
      (
          1,
          1,
          2,
          '맥북 프로 14',
          3000000,
          3500000,
          1,
          'https://images.unsplash.com/photo-1517336714731-489689fd1ca8'
      ),
      (
          2,
          1,
          13,
          '무선 마우스',
          59000,
          NULL,
          1,
          'https://images.unsplash.com/photo-1527814050087-3793815479db'
      ),

      -- 주문 2 (갤럭시)
      (
          3,
          2,
          10,
          '갤럭시 S24',
          1400000,
          1550000,
          1,
          'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf'
      ),

      -- 주문 3 (의류 + 헤드셋)
      (
          4,
          3,
          21,
          '베이직 티셔츠',
          19900,
          29900,
          2,
          'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab'
      ),
      (
          5,
          3,
          14,
          '게이밍 헤드셋',
          89000,
          NULL,
          1,
          'https://images.unsplash.com/photo-1546435770-a3e426bf472b'
      );
