SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM product;
DELETE FROM category;
DELETE FROM member;

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
             'pw12341234!',
             '일반회원',
             '010-1111-1111',
             'USER',
             'ACTIVE',
             '2026-03-29 10:00:00',
             '2026-03-29 18:30:00'
         ),
         (
             2,
             'admin1@test.com',
             'admin1234!',
             '관리자',
             '010-9999-9999',
             'ADMIN',
             'ACTIVE',
             '2026-03-29 09:00:00',
             '2026-03-29 19:00:00'
         );