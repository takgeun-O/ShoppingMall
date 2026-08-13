# 프로젝트 소개

Spring Boot 기반으로 구현한 이커머스 쇼핑몰 백엔드 프로젝트.

상품 조회, 장바구니, 주문, 회원가입 및 로그인, 관리자 기능 등 쇼핑몰의 핵심 도메인을 직접 설계하고 구현했습니다.


현재는 MyBatis + MySQL 기반으로 데이터 저장소를 구현했으며,
추후 JPA 기반 구현으로 확장하고 REST API 중심 구조로 발전시키는 것을 목표로 하고 있습니다.

---
# 프로젝트 특징

- MyBatis 기반 Repository 구현 및 향후 JPA 구현체 추가 가능 구조
- 서비스 계층 트랜잭션 분리 (readOnly / write)
- 주문 생성과 재고 차감을 단일 트랜잭션으로 처리
- requestKey를 이용한 중복 주문 요청 방지 및 주문 생성 멱등성 처리
- 사용자 / 관리자 기능 및 표현 계층 분리
- 조회 / 변경 작업에 따른 트랜잭션 경계 분리
- Domain 객체와 View DTO 분리
- 도메인 중심 패키지 구조 설계

# 프로젝트 목적

이 프로젝트는 단순 CRUD 구현이 아니라 다음을 목표로 진행

- 실제 쇼핑몰의 도메인 흐름 이해
- 계층형 백엔드 아키텍처 설계 (컨트롤러, 서비스, 도메인, 리포지토리)
- 비즈니스 로직 중심 서비스 구현
- 사용자 / 관리자 기능 분리
- UI와 백엔드 흐름 연결 경험
- 저장소 기술 전환을 고려한 구조 설계

---

# 실행 환경

- **Java**: 21
- **Spring Boot**: 4.0.1
- **Build Tool**: Gradle
- **Template Engine**: Thymeleaf
- **Database**: MySQL
- **Persistence**: MyBatis
- **IDE**: IntelliJ IDEA 권장
- **OS**: macOS / Windows / Linux

---

# 실행 방법

본 프로젝트는 MyBatis + MySQL 기반으로 동작합니다.

개발 환경에서는 Spring Profile 설정에 따라 MyBatis Repository가 활성화됩니다.

`demo` 프로필을 함께 활성화하면 `schema.sql`과 `data.sql`을 이용하여 테이블과 시연용 데이터가 초기화됩니다.


## 실행 방법 1. 로컬에서 실행

### 1. 프로젝트 클론

```bash
git clone https://github.com/takgeun-O/ShoppingMall.git
```

프로젝트 디렉토리로 이동합니다.

```bash
cd ShoppingMall
```

### 2. 데이터베이스 준비
MySQL에서 프로젝트용 데이터베이스를 생성합니다.

```sql
CREATE DATABASE shoppingmall;
```

`demo` 프로필로 애플리케이션을 실행하면 `schema.sql`과 `data.sql`을 통해 필요한 테이블과 시연용 데이터가 초기화됩니다.


※ application.yml에서 DB 연결 정보 (url, username, password)를 환경에 맞게 수정해야 합니다.

예시)
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shoppingmall
    username: your_username
    password: your_password
```

### 3. 애플리케이션 실행

MyBatis Repository와 Demo 데이터를 함께 사용하는 경우 다음과 같이 실행합니다.

macOS / Linux:

```bash
./gradlew bootRun --args='--spring.profiles.active=mybatis,demo'
```

Windows: 

```bash
gradlew.bat bootRun --args='--spring.profiles.active=mybatis,demo'
```


## 실행 방법 2. Ngrok 데모 접속

별도의 설치와 실행 없이 아래 링크에서 바로 확인하실 수 있습니다.

> Ngrok 데모는 개발 서버가 실행 중인 경우에만 접속 가능합니다.

- [데모 사이트 접속](https://exporter-bucket-flick.ngrok-free.dev/)

Ngrok 안내 화면이 나타나면 `Visit Site`를 선택해 주세요.

---

## 4. 데모 계정

일반 사용자 계정
- 이메일 : `user1@test.com`
- 패스워드 : `pw12341234!`

> 관리자 계정은 데이터 변경 및 삭제가 가능하므로 공개하지 않습니다.

> 관리자 기능 시연이 필요한 경우 별도로 문의해 주세요.

---

## 5. 데모 확인 가이드

현재 프로젝트는 MVP 단계까지 개발한 상태이며, 아래 핵심 흐름을 중심으로 확인할 수 있습니다.

### 사용자 핵심 Flow
1. 데모 사이트 접속
2. 테스트 계정으로 로그인
3. 상품 목록 및 상품 상세 조회
4. 상품을 장바구니에 추가
5. 장바구니 수량 변경 및 주문 금액 확인
6. 주문서 작성 및 주문 생성
7. 마이페이지에서 주문 내역 확인
8. 마이페이지 수정 및 탈퇴 (비밀번호 변경 미구현)

### 주요 구현 포인트

- 상품 상태와 재고를 검증한 장바구니 처리
- 주문 생성과 재고 차감을 하나의 트랜잭션으로 처리
- `requestKey`를 활용한 중복 주문 방지
- 사용자와 관리자 기능 분리
- 도메인 중심 패키지 구조
- MyBatis 기반 Repository 구현

### 관리자 기능

관리자 화면에서는 상품, 주문, 회원 및 카테고리를 관리할 수 있습니다.

관리자 기능은 데이터 변경 및 삭제가 가능하므로 계정을 공개하지 않습니다.

필요 시 요청주시면 별도로 제공드립니다.

### 참고 사항

현재 프로젝트는 MVP 개발 단계이므로 일부 화면과 기능이 미완성일 수 있습니다.

외부 결제, 배송 시스템, 커뮤니티 등 실제 연동 기능은 포함하지 않습니다.

Ngrok 데모는 로컬 서버가 실행 중인 시간에만 접속할 수 있습니다.

---

# 더미데이터 설명
경로 : src/main/resources/data.sql

`demo` 프로필에서 애플리케이션 실행 시 테스트용 데이터가 자동으로 생성됩니다.

생성 데이터
- 관리자 계정
- 일반 회원 계정
- 카테고리 데이터
- 상품 데이터
- 주문 데이터

---

# 주요 접속 URL

## 로컬 접속
- 메인 페이지: http://localhost:8080
- 상품 목록: http://localhost:8080/products
- 로그인: http://localhost:8080/login
- 회원가입 : http://localhost:8080/signup
- 마이페이지 : http://localhost:8080/members/me
- 관리자 대시보드 : http://localhost:8080/admin

## Ngrok 접속
- 메인페이지 : https://exporter-bucket-flick.ngrok-free.dev/
- 상품 목록 : https://exporter-bucket-flick.ngrok-free.dev/products
- 로그인 : https://exporter-bucket-flick.ngrok-free.dev/login
- 회원가입 : https://exporter-bucket-flick.ngrok-free.dev/signup
- 마이페이지 : https://exporter-bucket-flick.ngrok-free.dev/members/me
- 관리자 대시보드 : https://exporter-bucket-flick.ngrok-free.dev/admin

---

# 기술 스택

## Backend

- Java 21
- Spring Boot
- Spring MVC
- MyBatis
- Lombok

## Frontend

- Thymeleaf (SSR 기반 View)
- HTML / CSS

## Architecture

- Controller
- Service
- Domain
- Repository Interface
- MyBatis Repository Implementation
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
- 주문 상태 관리
  - ORDERED
  - PAYMENT_COMPLETED
  - PREPARING
  - SHIPPING
  - DELIVERED
  - CANCELED

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
- view (전역 View 데이터 구성용)

---

# 아키텍처

- Controller -> HTTP 요청/응답 및 입력 검증
- Application(Service) -> 유스케이스 흐름 및 트랜잭션 조정
- Domain -> 핵심 비즈니스 규칙과 상태 변경
- Repository -> 데이터 접근

---

# 설계 포인트

## Repository 인터페이스 분리

Repository 인터페이스와 MyBatis 구현체를 분리하여 설계했습니다.
현재는 MyBatis 기반으로 데이터 접근을 처리하고 있으며,
추후 JPA 기반 구현체로 확장하거나 교체할 수 있도록 구조를 분리했습니다.

## View DTO 분리
도메인 객체를 View에 직접 노출하지 않고 화면 전용 View DTO로 변환하여 전달

이 방식으로
- View 책임 분리
- 엔티티 보호
- 각 화면별로 쓰일 데이터로 구성하여 유지보수 용이

라는 장점을 누리게 하였습니다.

## 도메인 중심 패키지 구조

category, member, order, product 등 도메인 기준으로 패키지를 분리하고,
각 도메인의 응집도를 높이도록 설계했습니다.

---

# 예외 처리

커스텀 예외를 사용하여 비즈니스 오류를 명확하게 표현

- NotFoundException
- ConflictException
- ForbiddenException

---

# 추후 진행 계획

현재 프로젝트는 다음 단계를 계획 중

- (1순위) MyBatis Repository와 동일한 인터페이스를 구현하는 JPA Repository 추가
- REST API 중심 아키텍처로 확장

