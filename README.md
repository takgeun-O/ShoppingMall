# ShoppingMall

**[쇼핑몰 둘러보기](https://takgeun-shop.duckdns.org/products)**

Spring Boot로 만든 개인 쇼핑몰입니다. 상품 목록과 상세 페이지는 로그인 없이 둘러볼 수 있습니다.

## 회원과 관리자 기능

- **회원가입·로그인** — 이메일과 비밀번호로 가입하고 로그인합니다. 비밀번호는 BCrypt로 해시해 저장하고, 로그인 상태는 세션으로 유지합니다.
- **회원 주문** — 상품을 장바구니에 담아 주문하고, 마이페이지에서 최근 주문을 확인하거나 주문 내역에서 취소할 수 있습니다.
- **관리자 페이지** — 관리자 계정으로 `/admin`에 접속하면 최근 주문과 총매출을 확인하고, 상품을 등록하거나 카테고리를 관리할 수 있습니다.

## 주문과 재고 처리

- 주문 저장과 재고 차감을 한 트랜잭션으로 묶었습니다. 상품 하나라도 재고가 부족하면 주문 전체를 롤백합니다.
- 같은 요청 키(`requestKey`)로 주문이 다시 들어오면 중복 생성을 막습니다.
- 주문을 취소하면 주문한 수량만큼 상품 재고를 복구합니다.

[주문 서비스](src/main/java/io/github/takgeun/shop/order/application/OrderService.java) · [주문 서비스 테스트](src/test/java/io/github/takgeun/shop/order/application/OrderServiceTest.java)

**기술 스택** · Java 21, Spring Boot, Spring Security, Thymeleaf, MyBatis, MySQL  
**배포** · AWS Lightsail
