# 프로젝트 소개

Spring Boot 기반으로 구현한 이커머스 쇼핑몰 백엔드 프로젝트.
상품 조회, 장바구니, 주문, 회원가입 및 로그인, 관리자 기능 등 쇼핑몰의 핵심 도메인을 직접 설계하고 구현한다.

현재는 도메인 설계와 서비스 로직 검증에 집중하기 위해 메모리 기반 저장소를 사용하고 있으며,
다음 단계로 JPA 기반 DB 연동을 진행할 예정임.

---

# 프로젝트 목적

이 프로젝트는 단순 CRUD 구현이 아니라 다음을 목표로 진행

- 실제 쇼핑몰의 도메인 흐름 이해
- 계층형 백엔드 아키텍처 설계 (컨트롤러, 서비스, 도메인, 리포지토리)
- 비즈니스 로직 중심 서비스 구현
- 사용자 / 관리자 기능 분리
- UI와 백엔드 흐름 연결 경험

---

# 실행 환경

- **Java**: 21
- **Spring Boot**: 4.0.1
- **Build Tool**: Gradle
- **Template Engine**: Thymeleaf
- **Database**: 없음 (In-Memory Repository 사용)
- **IDE**: IntelliJ IDEA 권장
- **OS**: macOS / Windows / Linux

---

# 실행 방법

본 프로젝트는 별도의 데이터베이스 설정 없이 실행할 수 있도록
In-Memory Repository 기반으로 구성되어 있으며,
애플리케이션 실행 시 테스트용 더미 데이터가 자동으로 생성

## 1. 프로젝트 클론

```bash
git clone https://github.com/takgeun-O/ShoppingMall.git
cd <project-directory>
```

## 2. 애플리케이션 실행

아래와 같이 스프링 실행 시 local 프로필 지정
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

윈도우 환경에서는 다음 명령어를 사용
```bash
gradlew.bat bootRun --args="--spring.profiles.active=local"
```


## 3. 접속

애플리케이션 실행 후 아래 주소로 접속할 수 있습니다.
```bash
http://localhost:8080
```

## 4. 테스트 계정

관리자 계정
- 이메일 : testAdmin1@test.com
- 패스워드 : pw12341234!

일반 사용자 계정
- 이메일 : test1@test.com
- 패스워드 : pw12341234!

---

# 더미데이터 설명
경로 : shop/global/init/TestDataInitializer.java

`local` 프로필에서 애플리케이션 실행 시 테스트용 데이터가 자동으로 생성됩니다.

생성 데이터
- 관리자 계정
- 일반 회원 계정
- 카테고리 데이터
- 상품 데이터
- 주문 데이터

---

# 주요 접속 URL
메인페이지 : http://localhost:8080

상품 목록 : http://localhost:8080/products

로그인 : http://localhost:8080/login

회원가입 : http://localhost:8080/signup

마이페이지 : http://localhost:8080/members/me

관리자 대시보드 : http://localhost:8080/admin

---

# 기술 스택

## Backend

- Java 21
- Spring Boot
- Spring MVC
- Thymeleaf
- Lombok

## Frontend

- Thymeleaf
- Tailwind CSS

## Architecture

- Controller
- Service
- Repository (메모리 기반)
- Domain
- DTO / ViewModel

---

# 주요 기능

## 상품 기능

- 상품 목록 조회
- 상품 상세 조회
- 할인율 계산
- 품절 상태 표시
- 품절 상품 주문 차단

## 장바구니 기능

- 상품 장바구니 추가 (상품 재고 검증을 거침)
- 수량 증가 / 감소
- 상품 삭제
- 장바구니 요약 계산 (주문금액에 따른 배송비 계산)
- 품절 상품 주문 불가
- 수량 최소값 검증
- 장바구니 상태 검증

## 주문 기능

- 주문 생성
- 주문 상태 관리 (ORDERED, PAYMENT_COMPLETED, CANCELED)

## 관리자 기능

관리자 페이지에서 다음과 같은 기능을 제공

### 관리자 대시보드

- 전체 주문 요약
- 전체 상품 현황
- 전체 회원 현황
- 최근 주문 목록
- 상품 추가 페이지 진입
- 카테고리 관리 페이지 진입
- 회원 관리 페이지 진입

### 상품 관리

- 상품 목록 관리
- 상품 상태 관리
- 상품 추가
- 전체 상품 현황

### 카테고리 관리

- 카테고리 추가
- 하위 카테고리 추가
- 카테고리 수정
- 카테고리 삭제
- 카테고리별 상품 개수 현황
- 전체 카테고리 현황

### 주문 관리

- 전체 주문 목록
- 주문 정보 검색
- 주문 상태별 필터링
- 주문 상세 조회
- 주문 상태 변경
- 전체 주문 현황

### 회원 관리

- 전체 회원 현황
- 회원 검색 및 필터링
- 전체 회원 목록 조회
- 회원 수정
- 회원 상세 보기

---

# 프로젝트 구조

도메인 중심 패키지 구조를 적용하여
category, member, order, product 등 도메인별로 코드를 분리하고 각 도메인의 응집도를 높이도록 설계하였음.

또한 관리자 기능은 일반 사용자 기능과 성격이 다르므로 admin 전용 컨트롤러와 View DTO 등 표현 계층은 별도로 분리하여 관리자 기능을 명확하게 구분 (각 도메인별 view 패키지에 모아서 정리)

## category

- api (JSON 기반 REST API를 제공하는 컨트롤러 포함)
- application
- domain
- repository
- view (SSR을 위한 페이지 컨트롤러 포함)

## product

- api (JSON 기반 REST API를 제공하는 컨트롤러 포함)
- application
- domain
- repository
- view (SSR을 위한 페이지 컨트롤러 포함)

## order

- api (JSON 기반 REST API를 제공하는 컨트롤러 포함)
- application
- domain
- repository
- view (SSR을 위한 페이지 컨트롤러 포함)

## member

- api (JSON 기반 REST API를 제공하는 컨트롤러 포함)
- application
- domain
- repository
- view (SSR을 위한 페이지 컨트롤러 포함)

## admin

- application
- view (SSR을 위한 페이지 컨트롤러 포함)

## cart

- application
- domain
- repository
- view (SSR을 위한 페이지 컨트롤러 포함)

## global

- Webconfig (인터셉터 설정용)
- error (global 레벨에서의 예외 처리 설정용)
- init (테스트 더미 데이터)
- interceptor (일반 사용자 / 관리자 로그인 검증)
- session (세션에 저장하는 키 이름을 한 곳에서 관리)
- validation (검증 순서 설정용)
- view

---

# 아키텍처

- Controller -> HTTP 요청 처리
- Service -> 비즈니스 로직 처리
- Repository -> 데이터 접근
- Domain -> 핵심 비즈니스 모델

---

# 설계 포인트

## Repository 인터페이스 분리

현재 MemoryRepository 로 구현되었으나 추후 JPARepository로 구현체 교체가 용이하도록 설계

## View DTO 분리
엔티티를 직접 뷰에 전달하지 않고 엔티티를 View DTO로 변환하여 사용

이 방식으로
- View 책임 분리
- 엔티티 보호
- 각 화면별로 쓰일 데이터로 구성하여 유지보수 용이

라는 장점을 누리게 하였습니다.

## 도메인 중심 설계 (DDD)

주요 비즈니스 로직은 Service 계층에서 처리

---

# 예외 처리

커스텀 예외를 사용하여 비즈니스 오류를 명확하게 표현

- NotFoundException
- ConflictException
- ForbiddenException

---

# 추후 진행 계획

현재 프로젝트는 다음 단계를 계획 중임.

- (1순위) JPA 기반 DB 연동
- REST API 구현
- 트랜잭션 처리 개선

