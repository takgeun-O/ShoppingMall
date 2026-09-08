# ShoppingMall

Spring Boot로 구현한 이커머스 백엔드 개인 프로젝트입니다.

Thymeleaf 기반 웹 화면과 JSON REST API를 함께 제공하며, 회원 인증부터 상품 조회, 장바구니, 주문 생성·조회·취소, 관리자 운영 기능까지 쇼핑몰의 핵심 흐름을 구현했습니다.

> 현재 `main` 브랜치는 MyBatis + MySQL을 사용합니다. REST API는 카테고리·상품 조회와 일반 회원·주문 영역까지 구현되어 있으며, 장바구니와 관리자 기능은 현재 서버 사이드 렌더링 방식으로 제공합니다.

## 빠른 확인

- 데모: [쇼핑몰 바로가기](https://exporter-bucket-flick.ngrok-free.dev/)
- API 문서: [Swagger UI](https://exporter-bucket-flick.ngrok-free.dev/swagger-ui.html)
- 로컬 Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

> Ngrok 주소는 개발 서버가 실행 중일 때만 접속할 수 있습니다. Ngrok 안내 화면이 나타나면 **Visit Site**를 선택해 주세요.

### 데모 계정

| 구분 | 이메일 | 비밀번호 |
| --- | --- | --- |
| 일반 회원 | `user1@test.com` | `pw12341234!` |

관리자 계정은 데이터 변경·삭제 권한이 있어 공개하지 않습니다. 관리자 기능 시연이 필요하면 별도로 문의해 주세요.

## 핵심 구현 내용

- Spring Security 기반 세션 인증 및 사용자·관리자 URL 인가
- 화면 요청과 API 요청에 서로 다른 인증 실패 응답 적용
  - 화면: 로그인 페이지로 이동
  - API: 표준화된 JSON 오류 응답
- 주문 생성과 상품 재고 차감을 하나의 트랜잭션으로 처리
- 주문 취소 시 주문 상태 변경과 상품 재고 복구
- `requestKey`를 이용한 중복 주문 요청 감지
- 주문 상세 조회·취소 시 주문 소유권 검증
- 주문 시점의 상품명·가격·이미지를 주문 항목에 스냅샷으로 저장
- 카테고리 계층, 상품 판매 상태, 회원 상태, 주문 상태 전이 규칙을 도메인 객체로 관리
- Repository 인터페이스와 MyBatis 구현체 분리
- Thymeleaf View DTO와 REST API 요청·응답 DTO 분리
- Bean Validation과 전역 예외 처리를 이용한 일관된 API 오류 계약
- Controller, Service, 도메인, Security·DB 통합 테스트 구성

## 제공 기능

| 영역 | 웹 화면 | REST API | 주요 기능 |
| --- | :---: | :---: | --- |
| 카테고리 | O | O | 공개 카테고리 목록·상세 조회, 관리자 CRUD |
| 상품 | O | O | 목록·상세 조회, 상태·재고 검증, 관리자 관리 |
| 회원 | O | O | 회원가입·로그인, 내 정보 조회·수정, 비밀번호 변경, 탈퇴 |
| 장바구니 | O | - | 세션 장바구니, 수량 변경·삭제, 주문 금액 계산 |
| 주문 | O | O | 목록·상세 조회, 생성, 중복 요청 차단, 취소·재고 복구 |
| 관리자 | O | - | 대시보드, 상품·카테고리·주문·회원 관리 |

## REST API

### 공개 API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/categories` | 공개 카테고리 목록 조회 |
| `GET` | `/api/v1/categories/{categoryId}` | 공개 카테고리 상세 조회 |
| `GET` | `/api/v1/products` | 공개 상품 목록 조회 |
| `GET` | `/api/v1/products/{productId}` | 공개 상품 상세 조회 |

### 인증 필요 API

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/members/me` | 내 회원 정보 조회 |
| `PATCH` | `/api/v1/members/me` | 이름·전화번호 수정 |
| `PATCH` | `/api/v1/members/me/password` | 현재 비밀번호 확인 후 비밀번호 변경 |
| `DELETE` | `/api/v1/members/me` | 현재 비밀번호 확인 후 회원 탈퇴 |
| `GET` | `/api/v1/orders` | 내 주문 목록 조회 |
| `GET` | `/api/v1/orders/{orderId}` | 내 주문 상세 조회 |
| `POST` | `/api/v1/orders` | 주문 생성 |
| `PATCH` | `/api/v1/orders/{orderId}/cancel` | 내 주문 취소 |

인증은 Spring Security의 HTTP 세션을 사용합니다. 상태 변경 요청에는 CSRF 검증이 적용됩니다. 자세한 요청·응답 형식은 Swagger UI에서 확인할 수 있습니다.

## 기술 스택

| 분류 | 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.0.1, Spring MVC, Spring Security |
| Persistence | MyBatis 4.0.1 |
| Database | MySQL |
| View | Thymeleaf, HTML, CSS, JavaScript |
| API | REST, Bean Validation, springdoc-openapi 3.0.3 |
| Test | JUnit 5, AssertJ, Mockito, MockMvc, Spring Security Test |
| Build | Gradle Wrapper |

## 아키텍처

도메인을 기준으로 패키지를 나누고, 각 도메인 내부에서 표현·애플리케이션·도메인·인프라 계층을 분리했습니다.

```text
io.github.takgeun.shop
├── category
│   ├── api
│   ├── application
│   ├── domain
│   ├── infra
│   └── view
├── product
├── member
├── order
├── cart
├── admin
└── global
    ├── api
    ├── config
    ├── error
    ├── init
    ├── security
    ├── validation
    └── view
```

의존 흐름은 다음을 기준으로 합니다.

```text
API / View Controller
        ↓
Application Service
        ↓
Domain + Repository Interface
        ↓
MyBatis Repository Implementation
        ↓
MySQL
```

- Controller: HTTP 요청·응답 처리, 입력 검증, DTO 변환
- Application Service: 유스케이스 실행과 트랜잭션 조정
- Domain: 상태 전이와 핵심 비즈니스 규칙
- Repository Interface: 저장소 추상화
- `infra/mybatis`: MyBatis 기반 데이터 접근 구현

## 로컬 실행

### 권장: Docker로 빠르게 실행

#### 사전 준비

- Git
- Docker Desktop

Docker 방식에서는 Java, Gradle, MySQL을 별도로 설치할 필요가 없습니다. Gradle Wrapper와 Java 21 런타임, MySQL 8.4가 Docker 이미지 안에서 준비됩니다.

#### macOS / Linux

저장소를 처음 클론한 뒤 다음 네 줄로 실행할 수 있습니다.

```bash
git clone https://github.com/takgeun-O/ShoppingMall.git
cd ShoppingMall
cp .env.example .env
docker compose up --build
```

#### Windows PowerShell

```powershell
git clone https://github.com/takgeun-O/ShoppingMall.git
cd ShoppingMall
Copy-Item .env.example .env
docker compose up --build
```

최초 실행은 다음 작업 때문에 시간이 다소 걸릴 수 있습니다.

- Java 및 MySQL Docker 이미지 다운로드
- Gradle 의존성 다운로드
- Spring Boot 애플리케이션 빌드
- MySQL 초기화
- 데모 데이터 생성

#### 환경변수

`.env.example`은 Git에 포함되는 환경변수 예시 파일입니다. 이 파일을 `.env`로 복사하면 Docker Compose가 실제 로컬 실행값으로 읽습니다. `.env`는 Git에서 제외되며, 실제 DB 비밀번호와 관리자 비밀번호를 Git에 커밋하면 안 됩니다. `.env.example`에는 실행 구조를 보여주기 위한 예시값만 들어 있습니다.

| 환경변수 | 역할 |
| --- | --- |
| `MYSQL_DATABASE` | MySQL 컨테이너에 생성할 데이터베이스 이름 |
| `MYSQL_USER` | 애플리케이션이 사용할 MySQL 사용자 |
| `MYSQL_PASSWORD` | 애플리케이션용 MySQL 사용자의 비밀번호 |
| `MYSQL_ROOT_PASSWORD` | MySQL root 계정의 비밀번호 |
| `MYSQL_HOST_PORT` | 호스트에 공개할 MySQL 포트, 기본값 `3307` |
| `ADMIN_EMAIL` | `demo` 프로필에서 생성할 관리자 이메일 |
| `ADMIN_PASSWORD` | `demo` 프로필에서 생성할 관리자 비밀번호 |
| `ADMIN_NAME` | 데모 관리자 이름 |
| `ADMIN_PHONE` | 데모 관리자 전화번호 |

관리자 계정 정보는 공개하지 않습니다. 관리자 기능 시연이 필요하면 별도로 문의해 주세요.

#### Docker 구성

- `app`: Java 21에서 실행되는 Spring Boot 애플리케이션
- `db`: MySQL 8.4 데이터베이스
- MySQL healthcheck가 성공한 뒤 `app`이 실행됩니다.
- 컨테이너 내부에서 애플리케이션은 `db:3306`으로 MySQL에 연결합니다.
- 호스트에서 MySQL에 접근할 때는 기본적으로 `localhost:3307`을 사용합니다.
- 애플리케이션은 `demo,mybatis` 프로필로 실행됩니다.
- `demo` 프로필은 시작 시 `schema.sql`과 `data.sql`을 적용해 스키마와 데모 데이터를 준비합니다.

#### 접속 주소

- 쇼핑몰: [http://localhost:8080](http://localhost:8080)
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- MySQL 호스트 포트: `localhost:3307`

일반 사용자 데모 계정은 다음과 같습니다.

| 이메일 | 비밀번호 |
| --- | --- |
| `user1@test.com` | `pw12341234!` |

#### 실행 상태와 로그 확인

```bash
docker compose ps
docker compose logs -f app
docker compose logs -f db
```

#### 종료

일반 종료:

```bash
docker compose down
```

컨테이너와 MySQL 데이터 볼륨까지 삭제:

```bash
docker compose down -v
```

> `docker compose down -v`는 볼륨에 저장된 DB 데이터를 삭제합니다. 데모 DB를 완전히 초기화해야 할 때만 사용하세요. 또한 `demo` 프로필은 시작 시 `schema.sql`과 `data.sql`을 적용하므로 개인 데이터가 있는 데이터베이스에는 사용하지 마세요.

### 선택: 로컬 JDK와 MySQL로 직접 실행

#### 사전 준비

- Git
- JDK 21
- MySQL

Gradle은 Wrapper가 포함되어 있어 별도로 설치할 필요가 없습니다.

#### 1. 데이터베이스와 전용 계정 생성

MySQL에 접속한 뒤 아래 예시를 실행합니다. 비밀번호는 원하는 값으로 변경하세요.

```sql
CREATE DATABASE shoppingmall
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER 'shoppingmall_app'@'localhost'
    IDENTIFIED BY 'change-me';

GRANT ALL PRIVILEGES ON shoppingmall.*
    TO 'shoppingmall_app'@'localhost';

FLUSH PRIVILEGES;
```

이미 사용할 MySQL 계정이 있다면 데이터베이스만 생성해도 됩니다.

#### 2. 환경변수 설정

`application.yml`을 수정하거나 비밀번호를 커밋하지 말고 환경변수를 사용하세요.

macOS / Linux:

```bash
export DB_URL='jdbc:mysql://localhost:3306/shoppingmall?serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
export DB_USERNAME='shoppingmall_app'
export DB_PASSWORD='change-me'
export ADMIN_PASSWORD='change-admin-password'
```

Windows PowerShell:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/shoppingmall?serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
$env:DB_USERNAME='shoppingmall_app'
$env:DB_PASSWORD='change-me'
$env:ADMIN_PASSWORD='change-admin-password'
```

`ADMIN_PASSWORD`는 `demo` 프로필에서 시연용 관리자 계정을 초기화하기 위해 필요합니다. 관리자 이메일·이름·전화번호는 필요할 때 다음 환경변수로 변경할 수 있습니다.

```text
ADMIN_EMAIL
ADMIN_NAME
ADMIN_PHONE
```

#### 3. 애플리케이션 실행

macOS / Linux:

```bash
./gradlew bootRun --args='--spring.profiles.active=demo,mybatis'
```

Windows:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=demo,mybatis"
```

실행 후 다음 주소를 확인합니다.

- 웹 화면: [http://localhost:8080](http://localhost:8080)
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

> **주의:** `demo` 프로필은 실행할 때 `schema.sql`과 `data.sql`을 적용하며 기존 데모 테이블 데이터를 초기화합니다. 개인 데이터가 있는 데이터베이스에는 사용하지 마세요.

## 테스트 실행

통합 테스트는 운영용 데이터와 분리된 MySQL 테스트 데이터베이스를 사용합니다.

### 1. 테스트 데이터베이스 생성

```sql
CREATE DATABASE shoppingmall_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON shoppingmall_test.*
    TO 'shoppingmall_app'@'localhost';
```

### 2. 테스트 환경변수 설정

macOS / Linux:

```bash
export TEST_DB_URL='jdbc:mysql://localhost:3306/shoppingmall_test?serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
export TEST_DB_USERNAME='shoppingmall_app'
export TEST_DB_PASSWORD='change-me'
```

Windows PowerShell:

```powershell
$env:TEST_DB_URL='jdbc:mysql://localhost:3306/shoppingmall_test?serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
$env:TEST_DB_USERNAME='shoppingmall_app'
$env:TEST_DB_PASSWORD='change-me'
```

### 3. 전체 테스트 실행

macOS / Linux:

```bash
./gradlew clean test
```

Windows:

```powershell
.\gradlew.bat clean test
```

테스트 결과 보고서는 다음 경로에서 확인할 수 있습니다.

```text
build/reports/tests/test/index.html
```

## 화면 확인 경로

| 화면 | URL |
| --- | --- |
| 메인 | `/` |
| 상품 목록 | `/products` |
| 로그인 | `/login` |
| 회원가입 | `/signup` |
| 장바구니 | `/cart` |
| 마이페이지 | `/members/me` |
| 주문서 | `/orders/checkout` |
| 관리자 대시보드 | `/admin` |

## 주요 설계 결정

### 주문과 재고의 트랜잭션 일관성

주문 생성 과정에서 회원 상태와 상품 판매 상태를 확인한 뒤 재고를 차감하고 주문을 저장합니다. 이 흐름을 하나의 트랜잭션으로 묶어 중간 실패 시 전체 작업이 롤백되도록 구성했습니다. 주문 취소 시에는 취소 가능 상태와 소유권을 검증한 후 주문 수량만큼 재고를 복구합니다.

### 주문 상품 스냅샷

상품 정보가 나중에 변경되더라도 주문 당시 내역을 유지할 수 있도록 상품명, 판매 가격, 정가, 이미지 URL을 `OrderItem`에 복사하여 저장합니다.

### 중복 주문 요청 방지

클라이언트가 전달한 `requestKey`의 기존 처리 여부를 확인해 같은 요청이 반복 처리되는 것을 막습니다. 현재 구현은 중복 요청에 기존 결과를 재반환하는 방식이 아니라 충돌 응답으로 차단하는 방식입니다.

### 화면과 REST API의 공존

기존 Thymeleaf 화면을 유지하면서 도메인별 `api` 패키지에 REST Controller와 전용 DTO를 추가했습니다. 화면 Form·View DTO와 REST 요청·응답 DTO를 분리해 각 표현 계층의 변경이 서로에게 미치는 영향을 줄였습니다.

### 표준화된 API 오류 응답

인증 실패, 권한 부족, 입력값 검증 실패, 잘못된 JSON, 지원하지 않는 미디어 타입, 리소스 미존재, 비즈니스 충돌 등을 공통 JSON 형식으로 반환합니다.

## 제한 사항

- 외부 결제 시스템과 배송 시스템은 연동하지 않았으며 주문 생성 시 결제 성공을 가정합니다.
- 장바구니는 HTTP 세션 기반으로 동작하며 REST API는 아직 제공하지 않습니다.
- 관리자 기능은 Thymeleaf 화면으로 제공하며 관리자 REST API는 아직 구현하지 않았습니다.
- Ngrok 데모는 개발 서버가 실행 중일 때만 이용할 수 있습니다.

## 향후 계획

- JPA 기반 Repository 구현 추가
- QueryDSL을 이용한 동적 검색·필터링
- 장바구니 및 관리자 기능 REST API 확장
- API 문서와 테스트 시나리오 보강
- 배포 환경과 CI 파이프라인 구성
